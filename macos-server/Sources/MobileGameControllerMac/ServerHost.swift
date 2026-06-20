import Foundation

@MainActor
final class ServerHost: ObservableObject {
    @Published private(set) var status = ServerStatus()
    @Published private(set) var logEntries: [String] = []

    private let inputManager = KeyboardControllerManager()
    private var tcpServer: ControllerTcpServer?
    private var listeningPort = 27_015

    var isRunning: Bool {
        switch status.state {
        case .listening, .clientConnected:
            return true
        default:
            return false
        }
    }

    init() {
        appendLog("Ready. Grant Accessibility permission when starting the server.")
    }

    func start(port: Int) {
        guard !isRunning else { return }

        listeningPort = port
        updateStatus(.starting, port: port)

        guard KeyboardControllerManager.ensureAccessibilityPermission() else {
            appendLog("Accessibility permission required for keyboard/mouse input.")
            updateStatus(
                .error,
                port: port,
                errorMessage: "Enable Accessibility for this app in System Settings → Privacy & Security → Accessibility."
            )
            return
        }

        let server = ControllerTcpServer(inputManager: inputManager)
        server.onLog = { [weak self] message in
            Task { @MainActor in self?.appendLog(message) }
        }
        server.onClientConnected = { [weak self] endpoint in
            Task { @MainActor in
                self?.updateStatus(.clientConnected, port: port, connectedClient: endpoint, isInputReady: true)
            }
        }
        server.onClientDisconnected = { [weak self] in
            Task { @MainActor in
                self?.updateStatus(.listening, port: port, isInputReady: true)
            }
        }

        do {
            try server.start(port: UInt16(port))
            tcpServer = server
            updateStatus(.listening, port: port, isInputReady: true)
            appendLog("Input bridge ready (keyboard + mouse mapping).")
        } catch {
            appendLog(error.localizedDescription)
            updateStatus(.error, port: port, errorMessage: error.localizedDescription)
            tcpServer = nil
        }
    }

    func stop() {
        tcpServer?.stop()
        tcpServer = nil
        inputManager.resetController()
        updateStatus(.stopped, port: listeningPort)
        appendLog("Server stopped.")
    }

    private func updateStatus(
        _ state: ServerRunState,
        port: Int,
        connectedClient: String? = nil,
        errorMessage: String? = nil,
        isInputReady: Bool = false
    ) {
        status = ServerStatus(
            state: state,
            port: port,
            connectedClient: connectedClient,
            errorMessage: errorMessage,
            isInputReady: isInputReady
        )
    }

    private func appendLog(_ message: String) {
        let entry = "[\(Self.timeFormatter.string(from: Date()))] \(message)"
        logEntries.insert(entry, at: 0)
        if logEntries.count > 200 {
            logEntries.removeLast()
        }
    }

    private static let timeFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm:ss"
        return formatter
    }()
}
