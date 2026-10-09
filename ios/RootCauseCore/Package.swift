// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "RootCauseCore",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [.library(name: "RootCauseCore", targets: ["RootCauseCore"])],
    targets: [
        .target(name: "RootCauseCore"),
        .testTarget(name: "RootCauseCoreTests", dependencies: ["RootCauseCore"]),
    ]
)
