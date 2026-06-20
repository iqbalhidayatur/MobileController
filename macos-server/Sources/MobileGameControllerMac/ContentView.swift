import AppKit
import SwiftUI

struct ContentView: View {
    @EnvironmentObject private var server: ServerHost

    @State private var portText = "27015"
    @State private var localAddresses = NetworkHelper.localIPv4Addresses()
    @State private var selectedAddress = NetworkHelper.localIPv4Addresses().first ?? ""

    private var effectivePort: Int {
        Int(portText) ?? 27_015
    }

    private var fullAddress: String {
        selectedAddress.isEmpty ? "—" : "\(selectedAddress):\(effectivePort)"
    }

    private var statusTitle: String {
        switch server.status.state {
        case .stopped: return "Stopped"
        case .starting: return "Starting"
        case .listening: return "Waiting for phone"
        case .clientConnected: return "Connected"
        case .error: return "Error"
        }
    }

    private var statusDetail: String {
        switch server.status.state {
        case .stopped:
            return "Start the server, then connect from your phone."
        case .starting:
            return "Preparing keyboard and mouse input bridge..."
        case .listening:
            return "Open the Android app, enter the address below, and tap Connect."
        case .clientConnected:
            return "Controller input is forwarded as keyboard and mouse events."
        case .error:
            return server.status.errorMessage ?? "Unknown error."
        }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                header
                statusSection
                connectionSection
                mappingSection
                logSection
            }
            .padding(24)
        }
        .background(Color(nsColor: NSColor(red: 0.07, green: 0.07, blue: 0.07, alpha: 1)))
        .foregroundStyle(.white)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Mobile Game Controller")
                .font(.system(size: 28, weight: .bold))
            Text("macOS server for Android controller app")
                .foregroundStyle(.secondary)
        }
    }

    private var statusSection: some View {
        HStack(alignment: .top, spacing: 16) {
            VStack(alignment: .leading, spacing: 10) {
                Text("Server Status")
                    .font(.caption)
                    .foregroundStyle(.secondary)

                Text(statusTitle)
                    .font(.system(size: 22, weight: .semibold))

                Text(statusDetail)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)

                HStack(spacing: 8) {
                    Text("Connected client:")
                        .foregroundStyle(.secondary)
                    Text(server.status.connectedClient ?? "—")
                        .fontWeight(.semibold)
                }
                .padding(.top, 4)
            }
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(surface)
            .clipShape(RoundedRectangle(cornerRadius: 12))

            VStack(alignment: .leading, spacing: 8) {
                Text("Port")
                    .font(.caption)
                    .foregroundStyle(.secondary)

                TextField("27015", text: $portText)
                    .textFieldStyle(.roundedBorder)
                    .disabled(server.isRunning)
                    .frame(width: 180)

                Button(server.isRunning ? "Stop Server" : "Start Server") {
                    toggleServer()
                }
                .buttonStyle(PrimaryButtonStyle())
                .frame(width: 180)
            }
        }
    }

    private var connectionSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Connect from Android")
                .font(.title3.weight(.semibold))

            Text("1. Make sure phone and Mac are on the same WiFi.\n2. Start the server here.\n3. In the Android app, enter the address below and tap Connect.")
                .foregroundStyle(.secondary)

            HStack {
                Text("Local IP")
                Picker("Local IP", selection: $selectedAddress) {
                    ForEach(localAddresses, id: \.self) { address in
                        Text(address).tag(address)
                    }
                }
                .labelsHidden()
                .frame(minWidth: 220)

                Button("Refresh") {
                    refreshAddresses()
                }
                .buttonStyle(SecondaryButtonStyle())
            }

            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 6) {
                    Text("Address for Android app")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text(fullAddress)
                        .font(.system(size: 24, weight: .bold))
                        .foregroundStyle(Color(red: 0.13, green: 0.59, blue: 0.95))
                }
                .padding(14)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(nsColor: NSColor(red: 0.09, green: 0.09, blue: 0.09, alpha: 1)))
                .clipShape(RoundedRectangle(cornerRadius: 8))

                Button("Copy Address") {
                    copyAddress()
                }
                .buttonStyle(PrimaryButtonStyle())
                .disabled(!server.isRunning || selectedAddress.isEmpty)
            }
        }
        .padding(18)
        .background(surface)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private var mappingSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Default Key Mapping")
                .font(.title3.weight(.semibold))

            Text("macOS does not expose a public virtual Xbox controller API. This app maps input to keyboard and mouse instead.")
                .foregroundStyle(.secondary)

            Grid(alignment: .leading, horizontalSpacing: 24, verticalSpacing: 6) {
                GridRow {
                    Text("Face buttons").foregroundStyle(.secondary)
                    Text("A=Space  B=Shift  X=E  Y=Q")
                }
                GridRow {
                    Text("Shoulders / triggers").foregroundStyle(.secondary)
                    Text("LB=1  RB=2  LT=3  RT=4")
                }
                GridRow {
                    Text("Left stick / D-Pad").foregroundStyle(.secondary)
                    Text("WASD / Arrow keys")
                }
                GridRow {
                    Text("Right stick").foregroundStyle(.secondary)
                    Text("Mouse movement")
                }
                GridRow {
                    Text("Menu").foregroundStyle(.secondary)
                    Text("Start=Return  Select=Esc")
                }
            }
            .font(.callout)
        }
        .padding(18)
        .background(surface)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private var logSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Activity Log")
                .font(.headline)

            ScrollView {
                LazyVStack(alignment: .leading, spacing: 4) {
                    if server.logEntries.isEmpty {
                        Text("No activity yet.")
                            .foregroundStyle(.secondary)
                    } else {
                        ForEach(Array(server.logEntries.enumerated()), id: \.offset) { _, entry in
                            Text(entry)
                                .font(.system(.caption, design: .monospaced))
                                .foregroundStyle(.secondary)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                    }
                }
            }
            .frame(height: 180)
            .padding(12)
            .background(Color(nsColor: NSColor(red: 0.09, green: 0.09, blue: 0.09, alpha: 1)))
            .clipShape(RoundedRectangle(cornerRadius: 8))
        }
        .padding(18)
        .background(surface)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private var surface: some View {
        Color(nsColor: NSColor(red: 0.12, green: 0.12, blue: 0.12, alpha: 1))
    }

    private func toggleServer() {
        if server.isRunning {
            server.stop()
            return
        }

        guard let port = Int(portText), (1...65535).contains(port) else {
            return
        }

        server.start(port: port)
    }

    private func refreshAddresses() {
        localAddresses = NetworkHelper.localIPv4Addresses()
        if !localAddresses.contains(selectedAddress) {
            selectedAddress = localAddresses.first ?? ""
        }
    }

    private func copyAddress() {
        let pasteboard = NSPasteboard.general
        pasteboard.clearContents()
        pasteboard.setString(fullAddress, forType: .string)
    }
}

private struct PrimaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(Color(red: 0.30, green: 0.69, blue: 0.31).opacity(configuration.isPressed ? 0.8 : 1))
            .foregroundStyle(.white)
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .fontWeight(.semibold)
    }
}

private struct SecondaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(Color.white.opacity(configuration.isPressed ? 0.08 : 0.12))
            .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}
