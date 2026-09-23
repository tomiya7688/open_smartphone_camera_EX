# Architecture

## 1. Objective

`open_smartphone_camera_EX` separates **capturing pixels** from **processing pixels**.

The capture side is expected to change over time: Android APIs evolve, vendor behavior differs, and different phones expose different stream combinations. The processing side should not care which Android API produced a frame.

The architectural invariant is:

> Platform camera objects end at the backend boundary.

No processing module may depend on `android.hardware.camera2.*`, `android.media.Image`, CameraX `ImageProxy`, `CameraCharacteristics`, `CaptureResult`, or other Android camera types.

## 2. Dependency direction

```text
app-android
    │
    ├──────────────► capture-api ◄────────────── platform/android/*
    │                    │
    │                    ▼
    └──────────────► processing-api ◄────────── engine/*
```

Allowed:

- Android backend -> capture-api
- app -> capture-api / processing-api
- engine -> processing-api
- adapter code -> Android APIs

Forbidden:

- processing-api -> Android
- engine -> Android camera APIs
- capture-api -> Camera2/CameraX types
- one processing stage reaching into a capture session

This makes an Android API migration an adapter problem rather than an engine rewrite.

## 3. Capture boundary

The capture boundary should expose a small project-owned vocabulary:

- `CameraBackend`
- `CameraDescriptor`
- `CameraCapabilities`
- `CaptureRequest`
- `CapturedFrame`
- `CaptureMetadata`
- `FrameBuffer`
- `PixelFormat`

The backend is responsible for translating platform-specific objects into these types.

### Backend responsibilities

A backend may:

- enumerate cameras,
- report capabilities,
- open/close a camera,
- configure preview/capture streams,
- perform a still capture,
- translate platform metadata,
- copy or transfer plane data into a project-owned frame buffer.

A backend must not:

- perform application color grading,
- decide the final HDR algorithm,
- invoke AI enhancement,
- expose Android camera objects to the processing engine.

Backend-specific workarounds are allowed when required to make capture reliable on a device. They stay inside the backend.

## 4. Frame ownership

Frame lifetime must be explicit.

Android image objects often have strict lifetime rules. For example, CameraX requires `ImageProxy` objects to be closed, and Camera2 image readers can stall when too many images remain acquired.

Therefore the default boundary rule is:

1. receive the platform image,
2. validate its format/planes,
3. move/copy the required bytes into a project-owned buffer,
4. translate metadata,
5. close the platform image as early as possible,
6. emit `CapturedFrame`.

The initial implementation may copy more data than the final optimized implementation. Correct ownership is more important than zero-copy at M0.

Later, a zero-copy implementation can be introduced behind `FrameBuffer` as long as it preserves the same ownership contract.

## 5. Metadata

Capture metadata is not a free-form dump of Android keys.

The core model should contain fields that the processing engine actually understands, for example:

- timestamp,
- width / height,
- pixel format,
- exposure time,
- sensitivity (ISO),
- aperture when known,
- focal length when known,
- orientation,
- black level / white level when available,
- CFA pattern when available,
- color calibration / transform data when available,
- lens shading or related calibration data when available.

Unknown or device-specific data may be stored in a namespaced extension map, but processing stages should prefer stable typed fields.

All metadata fields that are not guaranteed must be optional.

## 6. Capabilities, not device-name checks

Do not branch on manufacturer/model names for normal behavior.

Prefer:

```text
if RAW + manual exposure + burst are available:
    use full RAW path
else if YUV capture is available:
    use YUV processing path
else:
    use encoded-image fallback
```

A device quirk table may exist only for verified hardware/firmware bugs that cannot be represented as capabilities.

Possible capability groups:

- formats: RAW, YUV, JPEG/HEIF
- manual sensor controls
- burst support / practical burst limits
- exposure range
- sensitivity range
- logical / physical camera information
- per-frame metadata availability
- hardware level / backend-specific constraints

## 7. Processing pipeline

Image processing is a chain of `ProcessingStage` objects.

```text
CapturedFrame(s)
   ↓
input normalization
   ↓
alignment                 (multi-frame path)
   ↓
merge / temporal denoise (multi-frame path)
   ↓
demosaic                  (RAW path)
   ↓
white balance
   ↓
color transform
   ↓
HDR / tone mapping
   ↓
local contrast / sharpen
   ↓
optional AI stage(s)
   ↓
encode/export
```

Not every stage runs for every input. A RAW Bayer frame and an already encoded JPEG require different graphs.

### AI is a stage, not a special architecture

AI processing must be optional and replaceable.

Examples:

- AI denoise,
- semantic local tone adjustment,
- super-resolution,
- deblur.

A profile may enable or disable such stages without changing how capture works.

## 8. Pipeline selection

A future `PipelinePlanner` should choose a processing graph from:

- input format,
- number of frames,
- metadata availability,
- device/backend capabilities,
- user quality mode,
- enabled features,
- resource budget.

This prevents UI code from hard-coding algorithm order.

Example profiles:

```text
FAST
  YUV/JPEG → light normalize → tone → encode

BALANCED
  short burst → align → merge → color → tone → sharpen → encode

QUALITY
  RAW burst → normalize → align → denoise/HDR → demosaic
            → color → tone → sharpen → encode

QUALITY + AI
  QUALITY pipeline + selected AI stage(s)
```

## 9. API-change containment

Android camera dependencies belong under:

```text
platform/android/
```

Prefer separate backend implementations instead of mixing APIs throughout the app:

```text
platform/android/
├── camera2/
├── camerax/
└── common/
```

If a future CameraX release changes interop APIs, only the CameraX adapter should require migration.

If Camera2 remains the better API for a specific RAW/burst path, both backends can coexist behind the same `CameraBackend` contract.

## 10. Stable core API rules

To keep our own abstraction from becoming another source of breakage:

1. Keep contracts small.
2. Add optional capability/metadata fields rather than repeatedly redesigning requests.
3. Prefer sealed project-owned enums/value objects to platform constants.
4. Do not expose backend implementation details through generic `Any` fields.
5. Version serialized test fixtures.
6. Treat frame ownership and byte layout as part of the API contract.
7. Add contract tests that every backend must pass.

Breaking the internal core API is allowed during early development, but changes should be deliberate and documented.

## 11. Testing strategy

### Backend contract tests

Every backend should be tested for:

- open/close behavior,
- capability consistency,
- frame dimensions and plane layout,
- metadata translation,
- frame lifetime / resource release,
- cancellation and error propagation.

### Processing tests

Processing tests should not require Android hardware.

Use recorded fixtures:

```text
sample-data/
├── raw/
├── yuv/
└── metadata/
```

A file/test backend can replay fixtures into the exact same `CapturedFrame` interface used by a phone.

### Golden-image tests

For deterministic stages, compare against reference outputs with tolerances rather than exact JPEG bytes.

## 12. Current Android implementation note

As of September 2026, CameraX remains built on Camera2 and provides Camera2 interoperability APIs, while RAW-capable Camera2 devices have guaranteed RAW-related stream combinations defined by Android. We should use those capabilities where useful, but neither CameraX nor Camera2 types belong in the processing API.

The project should prefer stable AndroidX releases for the app/backend layer and keep version-specific interop code isolated in the adapter implementation.
