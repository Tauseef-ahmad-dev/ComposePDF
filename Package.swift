// swift-tools-version: 5.7
import PackageDescription

let package = Package(
    name: "ComposePDF",
    products: [
        .library(
            name: "ComposePDFViewer",
            targets: ["ComposePDFViewer"]
        )
    ],
    targets: [
        .binaryTarget(
            name: "ComposePDFViewer",
            url: "https://github.com/Tauseef-ahmad-dev/ComposePDF/releases/download/ios-0.1.0-alpha.1/ComposePDFViewer.xcframework.zip",
            checksum: "654c37295218d43fd328748777bfec9ba4bf36f181cc4501e7481843d779c725"
        )
    ]
)
