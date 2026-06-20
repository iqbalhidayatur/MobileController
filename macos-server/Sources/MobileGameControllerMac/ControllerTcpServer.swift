import Foundation
import Network

final class ControllerTcpServer: @unchecked Sendable {
    var onLog: ((String) -> Void)?
    var onClientConnected: ((String) -> Void)?
    var onClientDisconnected: (() -> Void)?

    private(set) var isClientConnected = false
    private(set) var connectedClientEndpoint: String?

    private let inputManager: KeyboardControllerManager
    private var listener: NWListener?
    private let queue = DispatchQueue(label: "ControllerTcpServer")
    private var isStopped = false

    init(inputManager: KeyboardControllerManager) {
        self.inputManager = inputManager
    }

    func start(port: UInt16) throws {
        isStopped = false

        let parameters = NWParameters.tcp
        parameters.allowLocalEndpointReuse = true

        guard let nwPort = NWEndpoint.Port(rawValue: port) else {
            throw ServerError.invalidPort
        }

        let listener = try NWListener(using: parameters, on: nwPort)
        self.listener = listener

        listener.stateUpdateHandler = { [weak self] state in
            if case .failed(let error) = state {
                self?.log("Listener failed: \(error.localizedDescription)")
            }
        }

        listener.newConnectionHandler = { [weak self] connection in
            self?.queue.async {
                self?.serve(connection)
            }
        }

        listener.start(queue: queue)
        log("Listening on port \(port)...")
        log("Waiting for Android client connection...")
    }

    func stop() {
        isStopped = true
        listener?.cancel()
        listener = nil
    }

    private func serve(_ connection: NWConnection) {
        connection.start(queue: queue)

        let endpoint = connection.endpoint.debugDescription
        isClientConnected = true
        connectedClientEndpoint = endpoint
        log("Client connected: \(endpoint)")
        onClientConnected?(endpoint)

        receive(on: connection, buffer: Data())
    }

    private func receive(on connection: NWConnection, buffer: Data) {
        var buffer = buffer

        connection.receive(minimumIncompleteLength: 1, maximumLength: 8192) { [weak self] data, _, isComplete, error in
            guard let self else { return }

            if self.isStopped {
                self.finishClient(connection)
                return
            }

            if let error {
                self.log("Client error: \(error.localizedDescription)")
                self.finishClient(connection)
                return
            }

            if let data, !data.isEmpty {
                buffer.append(data)

                while let newlineIndex = buffer.firstIndex(of: 0x0A) {
                    let lineData = buffer[..<newlineIndex]
                    buffer = Data(buffer[(buffer.index(after: newlineIndex))...])

                    if let line = String(data: lineData, encoding: .utf8), !line.isEmpty {
                        self.processMessage(line, connection: connection)
                    }
                }
            }

            if isComplete {
                self.finishClient(connection)
                return
            }

            self.receive(on: connection, buffer: buffer)
        }
    }

    private func processMessage(_ json: String, connection: NWConnection) {
        guard let data = json.data(using: .utf8) else { return }

        do {
            let message = try JSONDecoder().decode(ControllerMessage.self, from: data)

            switch message.type.lowercased() {
            case "button":
                guard let button = message.button, let pressed = message.pressed else { return }
                if button.uppercased() == "LT" || button.uppercased() == "RT" {
                    inputManager.handleTrigger(button, pressed: pressed)
                } else {
                    inputManager.handleButton(button, pressed: pressed)
                }

            case "analog":
                guard let stick = message.stick,
                      let x = message.x,
                      let y = message.y else { return }
                inputManager.handleAnalog(stick: stick, x: x, y: y)

            case "ping":
                let timestamp = message.timestamp ?? Int64(Date().timeIntervalSince1970 * 1000)
                let pong = "{\"type\":\"pong\",\"timestamp\":\(timestamp)}\n"
                connection.send(content: Data(pong.utf8), completion: .contentProcessed { _ in })

            default:
                break
            }
        } catch {
            log("Invalid JSON: \(error.localizedDescription)")
        }
    }

    private func finishClient(_ connection: NWConnection) {
        connection.cancel()
        inputManager.resetController()

        if isClientConnected {
            isClientConnected = false
            connectedClientEndpoint = nil
            log("Client disconnected. Input reset.")
            onClientDisconnected?()
        }
    }

    private func log(_ message: String) {
        onLog?(message)
    }
}

enum ServerError: LocalizedError {
    case invalidPort

    var errorDescription: String? {
        switch self {
        case .invalidPort:
            return "Invalid port number."
        }
    }
}
