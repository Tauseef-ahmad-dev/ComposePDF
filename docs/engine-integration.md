# Rendering engine integration

ComposePDF separates the viewer from the PDF backend through the PdfRenderEngine interface in composepdf-core.

## Implement an engine

A backend implements:

- open(source, password) to open a document and return its metadata.
- getPageMetadata(pageIndex) to report page dimensions in PDF points.
- renderPage(pageIndex, options) to return a Compose ImageBitmap.
- extractText(pageIndex) when text extraction is supported.
- close() to release document handles, page handles, temporary files, and native resources.

Every engine must expose a PdfEngineCapabilities value. A capability must be false unless the backend genuinely supports it.

## Provide a factory

Create a PdfEngineFactory that returns a fresh engine for each document session. Do not share one mutable native document handle across unrelated viewer states.

## Threading and lifecycle

Engine implementations own their threading rules. They must serialize access when their native library is not thread-safe, avoid blocking the UI thread, and make cleanup safe after failed or cancelled opens.

## Current backend status

| Backend | Status | Source support |
|---|---|---|
| Android PdfRenderer | Implemented initial adapter | File, bytes, content URI |
| iOS PDFKit | Planned | Planned |
| Desktop PDFBox/PDFium | Planned | Planned |
| PDFium shared adapter | Under evaluation | Planned |

The Android adapter does not currently support password-protected documents, URL downloads, text extraction, text selection, annotations, or forms.
