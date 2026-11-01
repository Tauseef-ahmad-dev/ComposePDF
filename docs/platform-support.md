# Platform support

## Build targets

The project is configured for:

- Android
- iOS Arm64
- iOS Simulator Arm64
- Desktop JVM

A configured compilation target does not imply that a built-in PDF rendering engine is already available on that platform.

## Current functional support

| Capability | Android | iOS | Desktop |
|---|---|---|---|
| Shared viewer UI | Yes | Yes | Yes |
| Built-in rendering backend | Initial PdfRenderer adapter | Not yet | Not yet |
| Local file loading | Yes | Not yet | Not yet |
| Byte-array loading | Yes | Not yet | Not yet |
| Content URI loading | Yes | N/A | N/A |
| Remote URL loading | Not yet | Not yet | Not yet |
| Text extraction | Not yet | Not yet | Not yet |
| Search | Not yet | Not yet | Not yet |
| Annotations | Not yet | Not yet | Not yet |

## Android PdfRenderer adapter

Use androidPdfEngineFactory(context) to create an engine factory. The Android adapter supports local filesystem paths, in-memory bytes, and content URIs. Content URI streams are copied to a temporary cache file because PdfRenderer requires a seekable file descriptor.

The adapter currently reports unsupported capabilities rather than implying feature parity with other PDF engines.
