import SwiftUI

@main
struct MobileGameControllerMacApp: App {
    @StateObject private var serverHost = ServerHost()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(serverHost)
                .frame(minWidth: 760, minHeight: 600)
        }
        .windowResizability(.contentSize)
    }
}
