# ComposePDF

A Compose Multiplatform PDF viewer library for Android, iOS, and Desktop.

> **Status: early development.** Public APIs and platform support are under active development and are not yet stable.

## Vision

ComposePDF aims to provide a Compose-first PDF viewing experience with a consistent Kotlin API, pluggable rendering engines, predictable document lifecycle management, and customizable UI.

## Planned platforms

- Android
- iOS (device and simulator)
- Desktop (JVM)
- Web is a future target, subject to rendering-engine feasibility.

## Design principles

- Compose-first UI that can be customized or replaced.
- Rendering engines behind explicit interfaces and capability declarations.
- Shared Kotlin contracts with platform-specific file access and rendering.
- Lazy page rendering, bounded caching, and cancellable work.
- No promise of feature parity where a backend does not support a capability.

## Project modules

The initial implementation is organized around `composepdf-core` and `composepdf-viewer`. Additional engine and feature modules will be introduced as their implementations become concrete.

## License

Apache License 2.0. See [LICENSE](LICENSE).
