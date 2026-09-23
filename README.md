# open_smartphone_camera_EX

An open-source smartphone camera project focused on a replaceable capture layer and a deep, vendor-independent image-processing pipeline.

> **Current status:** architecture/bootstrap phase.

## Goal

The first goal is intentionally simple:

1. capture a photo reliably,
2. obtain the image data and useful capture metadata,
3. hand both to an image-processing pipeline through a stable project-owned API.

The processing pipeline can become as sophisticated as needed without making the rest of the application depend on Android camera API details.

AI processing will be supported later as an optional processing stage. It is not a separate architecture: it should be possible to enable or disable it like any other stage.

## Design principles

- **Android camera APIs stay at the edge.** `android.media.Image`, Camera2 classes, CameraX `ImageProxy`, and other platform-specific types must not cross into the processing core.
- **Own the boundary.** The project defines its own capture request, frame, metadata, capability, and processing contracts.
- **Backends are replaceable.** Camera2, CameraX, a future Android API, recorded test data, or another platform can implement the same capture contract.
- **Processing is composable.** Denoise, HDR merge, demosaic, color, tone mapping, sharpening, AI, and export are stages that can evolve independently.
- **Capability-driven behavior.** Features are selected from what a device/backend actually exposes instead of assuming every phone supports the same path.
- **Explicit ownership.** A frame has a clear lifetime. Platform image objects are converted into project-owned buffers before leaving the backend boundary.
- **Test without a phone.** The same pipeline must accept frames from files/test fixtures so algorithms can be tested independently from camera hardware.

## Proposed architecture

```text
┌──────────────────────────── Android UI / App ────────────────────────────┐
│                                                                          │
│  preview / controls / lifecycle / permissions                            │
│                         │                                                │
└─────────────────────────┼────────────────────────────────────────────────┘
                          │ project-owned API only
                          ▼
┌──────────────────────── Capture API ─────────────────────────────────────┐
│ CameraBackend · CaptureRequest · CapturedFrame · CaptureMetadata         │
│ CameraCapabilities · PixelFormat · FrameBuffer                           │
└─────────────────────────┬────────────────────────────────────────────────┘
                          │
             ┌────────────┴─────────────┐
             ▼                          ▼
┌───────────────────────┐   ┌────────────────────────┐
│ Android Camera2       │   │ future CameraX/backend │
│ adapter/backend       │   │ or test/file backend   │
└───────────────────────┘   └────────────────────────┘

                          CapturedFrame
                               │
                               ▼
┌──────────────────────── Processing API ──────────────────────────────────┐
│ ProcessingPipeline · ProcessingStage · ProcessingContext                 │
└─────────────────────────┬────────────────────────────────────────────────┘
                          ▼
  normalize → align → denoise → HDR → demosaic → color → tone → sharpen
                                                     ↘ optional AI stage
                          │
                          ▼
                    JPEG / HEIF / DNG / etc.
```

The most important rule is that replacing an Android camera API should require replacing an adapter/backend, **not rewriting the processing engine**.

## Repository layout

```text
.
├── capture-api/        # platform-independent capture contracts
├── processing-api/     # platform-independent processing contracts
├── platform/
│   └── android/        # Camera2/CameraX adapters live here
├── engine/             # processing implementations (CPU/native/GPU later)
├── app-android/        # Android UI/orchestration
└── docs/
    ├── architecture.md
    └── roadmap.md
```

Not every directory is implemented yet. The layout describes dependency direction, not a requirement to build everything at once.

## First milestone

**M0 — Capture → Frame → Pipeline**

- enumerate camera capabilities,
- open one camera,
- capture a still frame,
- obtain RAW/YUV/JPEG where supported,
- normalize metadata into project-owned types,
- pass a `CapturedFrame` to a no-op/reference processing pipeline,
- save/log the result,
- verify that the processing API contains no Android-specific types.

After that, the first real processing target is a neutral single-frame path. Multi-frame denoise/HDR and AI come later.

See [docs/architecture.md](docs/architecture.md) and [docs/roadmap.md](docs/roadmap.md).
