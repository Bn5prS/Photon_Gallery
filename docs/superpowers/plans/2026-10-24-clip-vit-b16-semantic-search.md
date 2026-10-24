# CLIP ViT-B/16 Semantic Search Upgrade Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Architecture:** Replace the defective MobileCLIP-S2 model with the empirically verified winner, OpenAI CLIP ViT-B/16 (Patch-16, 196 tokens, 224×224). Update image preprocessing to standard 224×224 with official CLIP channel normalization, configure BPE tokenizer, set calibrated noise-floor gating (`minConfidence = 0.235f`, margin `0.040f`), update model download URLs/hashes, bump version to 3 to trigger re-indexing, verify with JVM unit tests, build release APK, and deploy to device.

---

## Tasks

### Task 1: Package Winning ViT-B/16 Models in Project & Assets
- [ ] Copy `scratch/clip_vit_b16/vision_model_quantized.onnx` and `text_model_quantized.onnx` into `models/`
- [ ] Ensure `models/tokenizer.json` is in place
- [ ] Verify file sizes and SHA-256 hashes

### Task 2: Update ModelDownloadWorker URLs, Hashes, and Version
- [ ] In `app/src/main/java/com/inferno/gallery/workers/ModelDownloadWorker.kt`:
  - Set `TEXT_MODEL_URL = "https://huggingface.co/Xenova/clip-vit-base-patch16/resolve/main/onnx/text_model_quantized.onnx"`
  - Set `VISION_MODEL_URL = "https://huggingface.co/Xenova/clip-vit-base-patch16/resolve/main/onnx/vision_model_quantized.onnx"`
  - Set `TOKENIZER_URL = "https://huggingface.co/Xenova/clip-vit-base-patch16/raw/main/tokenizer.json"`
  - Set sizes: Vision `87_461_602L`, Text `64_504_507L`, Tokenizer `2_224_081L`
  - Update SHA-256 checksums
  - Bump `SEMANTIC_MODEL_VERSION = 3` and `MODEL_DIR = "smart_search_model_v3"`

### Task 3: Update SmartSearchEngine Preprocessing & Tokenization
- [ ] In `app/src/main/java/com/inferno/gallery/data/ai/SmartSearchEngine.kt`:
  - Change target image size to `224`
  - Re-enable official CLIP normalization:
    - `(r - 0.48145466f) / 0.26862954f`
    - `(g - 0.4578275f) / 0.26130258f`
    - `(b - 0.40821073f) / 0.2757771f`
  - Use `MODEL_DIR = "smart_search_model_v3"`
  - Keep prompt ensembling: `baseVec + templatedVec`

### Task 4: Calibrate GalleryViewModel Search Scoring
- [ ] In `app/src/main/java/com/inferno/gallery/ui/GalleryViewModel.kt`:
  - Set `minConfidence = 0.235f`
  - Set `adaptiveThreshold = maxOf(minConfidence, topScore - 0.040f)`
  - Update display percentage formula:
    `(((score - 0.235f) / (0.330f - 0.235f)) * 40f + 60f).coerceIn(60f, 99f).toInt()`

### Task 5: Unit Tests Verification
- [ ] Update `app/src/test/java/com/inferno/gallery/ui/SemanticSearchScoringTest.kt` with ViT-B/16 calibrated constants
- [ ] Run `./gradlew testDebugUnitTest` and confirm 100% pass

### Task 6: Release Build, Device Deployment & Logcat Verification
- [ ] Run `./gradlew assembleRelease`
- [ ] Stream-install `PhotonGallery-arm64-v8a.apk` onto device (`10BEAK2JR50012G`)
- [ ] Launch app and verify in logcat that re-indexing starts smoothly with ViT-B/16
