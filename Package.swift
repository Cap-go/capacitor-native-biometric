// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapgoCapacitorNativeBiometric",
    platforms: [.iOS(.v14)],
    products: [
        .library(
            name: "CapgoCapacitorNativeBiometric",
            targets: ["CapgoNativeBiometricPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "7.0.0")
    ],
    targets: [
        .target(
            name: "CapgoNativeBiometricPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/CapgoNativeBiometricPlugin"),
        .testTarget(
            name: "CapgoNativeBiometricPluginTests",
            dependencies: ["CapgoNativeBiometricPlugin"],
            path: "ios/Tests/CapgoNativeBiometricPluginTests")
    ]
)
