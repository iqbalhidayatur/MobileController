// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "MobileGameControllerMac",
    platforms: [
        .macOS(.v13)
    ],
    products: [
        .executable(
            name: "MobileGameControllerMac",
            targets: ["MobileGameControllerMac"]
        )
    ],
    targets: [
        .executableTarget(
            name: "MobileGameControllerMac",
            path: "Sources/MobileGameControllerMac"
        )
    ]
)
