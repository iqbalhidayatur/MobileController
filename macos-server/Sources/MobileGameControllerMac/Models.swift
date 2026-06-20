import Foundation

struct ControllerMessage: Codable, Sendable {
    let type: String
    let button: String?
    let pressed: Bool?
    let stick: String?
    let x: Float?
    let y: Float?
    let timestamp: Int64?
}

enum ServerRunState: Equatable {
    case stopped
    case starting
    case listening
    case clientConnected
    case error
}

struct ServerStatus: Equatable {
    var state: ServerRunState = .stopped
    var port: Int = 27_015
    var connectedClient: String?
    var errorMessage: String?
    var isInputReady: Bool = false
}

enum NetworkHelper {
    static func localIPv4Addresses() -> [String] {
        var addresses: [String] = []
        var ifaddr: UnsafeMutablePointer<ifaddrs>?

        guard getifaddrs(&ifaddr) == 0, let first = ifaddr else {
            return addresses
        }

        defer { freeifaddrs(ifaddr) }

        for pointer in sequence(first: first, next: { $0.pointee.ifa_next }) {
            let interface = pointer.pointee
            guard let addr = interface.ifa_addr else { continue }
            guard addr.pointee.sa_family == UInt8(AF_INET) else { continue }

            let name = String(cString: interface.ifa_name)
            if name == "lo0" { continue }

            var hostname = [CChar](repeating: 0, count: Int(NI_MAXHOST))
            let result = getnameinfo(
                addr,
                socklen_t(addr.pointee.sa_len),
                &hostname,
                socklen_t(hostname.count),
                nil,
                0,
                NI_NUMERICHOST
            )

            guard result == 0 else { continue }
            addresses.append(String(cString: hostname))
        }

        return addresses
    }
}
