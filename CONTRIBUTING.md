# Contributing to ComposePDF

Thanks for your interest in ComposePDF.

## Project scope

ComposePDF is a Kotlin and Compose Multiplatform PDF viewer library targeting Android, iOS, and Desktop (JVM). The project prioritizes a stable common API, pluggable rendering engines, explicit backend capabilities, and a customizable Compose UI.

## Development principles

- Keep platform-specific APIs out of common source sets.
- Put rendering and document operations behind the core engine contract.
- Report unsupported backend capabilities honestly.
- Keep document and native-resource lifecycles explicit.
- Prefer small, focused modules with clear responsibilities.
- Document public APIs and platform limitations.

## Current implementation

The initial implementation contains the core contracts, the Compose viewer surface, and an Android PdfRenderer adapter. iOS and Desktop rendering adapters, remote URL loading, search, zoom, thumbnails, and annotations are not yet implemented.

## Scope constraints

The repository intentionally does not include CI workflows or automated test infrastructure.

## License

By contributing, you agree that your contributions are licensed under the Apache License 2.0.
