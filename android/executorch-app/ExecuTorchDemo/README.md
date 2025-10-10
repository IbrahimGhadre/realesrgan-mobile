# Real-ESRGAN Mobile (ExecuTorch)

Android implementation of Real-ESRGAN using **Meta's ExecuTorch** (Java-only).  
This module complements the NCNN version to compare frameworks on-device.

## Highlights
- Java-only (no JNI/C++)
- Model format: `.pte` (**included**)
- Same preprocessing as NCNN (center crop, Gaussian blur, JPEG-like down/up, resize 224×224)
- Output saved to `/storage/emulated/0/Pictures/ExecuTorchOutput`

## Model files (included)
app/src/main/assets/realesrgan.pte

## Build & Run
1. Open `android/executorch-app/` in Android Studio  
2. Sync Gradle  
3. Connect a device and press **Run ▶**

## Troubleshooting
- **Model not found**: ensure the `.pte` filename matches the one used in code.
- **Runtime errors**: verify the used ops are supported by your ExecuTorch version.

## License
MIT for this module. External model licenses follow their originals.
