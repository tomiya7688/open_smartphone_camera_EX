# Roadmap

The roadmap is intentionally vertical: each milestone should leave the project in a runnable/testable state.

## M0 — Capture boundary

**Goal:** prove that camera APIs can be replaced without changing the processing contract.

Deliverables:

- [ ] `capture-api` project-owned contracts
- [ ] `processing-api` stage/pipeline contracts
- [ ] Android app shell
- [ ] one Android capture backend
- [ ] camera enumeration
- [ ] capability reporting
- [ ] still capture
- [ ] frame + metadata translation
- [ ] no-op/reference processing stage
- [ ] output/logging path
- [ ] backend contract tests where practical

Definition of done:

> Tap shutter → capture frame → convert to `CapturedFrame` → submit to `ProcessingPipeline` → receive result, with no Android camera type entering the processing API.

## M1 — Neutral single-frame pipeline

**Goal:** create a correct baseline image before adding computational photography.

RAW path where supported:

- [ ] black-level normalization
- [ ] white-level normalization
- [ ] CFA interpretation
- [ ] basic bad-pixel handling
- [ ] demosaic
- [ ] white balance
- [ ] camera color transform
- [ ] working color space
- [ ] tone mapping
- [ ] basic sharpening
- [ ] JPEG/PNG reference export

Fallback path:

- [ ] YUV input support
- [ ] encoded image fallback

Quality target:

- predictable, neutral color and exposure behavior,
- no manufacturer-specific “look” required,
- reproducible reference output from test fixtures.

## M2 — Multi-frame foundation

- [ ] burst capture abstraction
- [ ] frame grouping
- [ ] exposure/timestamp consistency checks
- [ ] image registration
- [ ] motion/confidence map
- [ ] temporal denoise
- [ ] ghosting-safe merge
- [ ] memory/resource budgeting

## M3 — HDR

- [ ] exposure-bracket planning
- [ ] highlight-safe short exposures
- [ ] multi-exposure alignment
- [ ] radiometric merge
- [ ] local/global tone mapping
- [ ] motion-aware fallback

## M4 — Device calibration and robustness

- [ ] calibration fixture format
- [ ] color target workflow
- [ ] lens shading handling
- [ ] verified quirk database
- [ ] automated device capability report
- [ ] crash/timeout recovery tests
- [ ] wider phone coverage

## M5 — Performance

Correctness first, then:

- [ ] buffer pooling
- [ ] fewer copies at capture boundary
- [ ] ARM SIMD/NEON paths
- [ ] native C++ processing implementation
- [ ] Vulkan/compute experiments for suitable stages
- [ ] thermal/memory-aware quality scaling

The public processing contracts should not change merely because a stage moves from Kotlin/CPU to C++/GPU.

## M6 — Optional AI stages

AI is additive, not required for capture.

Possible stages:

- [ ] AI denoise
- [ ] deblur
- [ ] super-resolution
- [ ] semantic tone/local adjustments

Requirements:

- [ ] each AI feature can be disabled,
- [ ] non-AI pipeline remains fully functional,
- [ ] model/runtime implementation stays behind a processing-stage boundary,
- [ ] resource requirements are reported before execution,
- [ ] results can be compared against the non-AI path.

## Near-term issue order

1. Define core capture/processing contracts.
2. Bootstrap Android app/backend module.
3. Implement camera capability dump.
4. Implement single still capture.
5. Translate frame and metadata into project types.
6. Connect capture to no-op pipeline.
7. Add recorded-frame test backend.
8. Begin neutral RAW reference pipeline.
