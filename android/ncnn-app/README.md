# Real-ESRGAN Mobile (NCNN) — Android

Single-image super-resolution on Android using **NCNN** + **OpenCV** (arm64-v8a).

> This module is the Android NCNN app from my MSc thesis (Digital Healthcare & AI).  
> The repository keeps the project lightweight: **no heavy SDKs/models are committed**.

---

## Requirements
- Android Studio (AGP compatible)
- NDK **26.1.10909125**
- CMake **3.22.1**
- A physical Android device (**arm64-v8a**) is recommended

---

## Local setup (files not in VCS)
Place external SDKs and models **locally** as follows:

android/ncnn-app/third_party/opencv-4.11.0-android-sdk/OpenCV-android-sdk/sdk/native/
jni/include/opencv2/core.hpp
libs/arm64-v8a/libopencv_java4.so

app/src/main/cpp/include/ncnn/ # NCNN headers (kept light)

libs/arm64-v8a/libncnn.so # NCNN shared library

app/src/main/jniLibs/arm64-v8a/libc++_shared.so # NDK C++ runtime (shipped in APK)

app/src/main/assets/
realesr_general_x4v3.ncnn.param
realesr_general_x4v3.ncnn.bin


**Notes**
- The OpenCV Android SDK is stored under `third_party/` and is **ignored by Git**.
- `libc++_shared.so` is shipped via `jniLibs/` to avoid runtime issues on some ROMs.
- Model files live in `assets/` **locally only**; the repo contains documentation but not the heavy blobs.

---

## Build & Run
1. Open **`android/ncnn-app/`** in Android Studio.
2. Sync Gradle. Ensure:
   - `ndkVersion = "26.1.10909125"`
   - CMake `3.22.1`
   - ABI filter includes `arm64-v8a`.
3. Connect an arm64 device and **Run ▶️**.
4. In the app:
   - Pick an input image.
   - The app applies preprocessing and runs NCNN inference (Real-ESRGAN).
   - Outputs are saved under: `/storage/emulated/0/Pictures/NCNNOutput`.

---


## Troubleshooting

- **`java.lang.UnsatisfiedLinkError: libc++_shared.so not found`**  
  Ensure `app/src/main/jniLibs/arm64-v8a/libc++_shared.so` exists.  
  (We also link the STL in CMake; shipping via `jniLibs` is the safest fallback.)

- **OpenCV headers or `libopencv_java4.so` not found at build time**  
  Check the path includes the extra folder:  
  `third_party/opencv-4.11.0-android-sdk/OpenCV-android-sdk/sdk/native/...`

- **`libncnn.so` not found or ABI mismatch**  
  Make sure `app/src/main/cpp/libs/arm64-v8a/libncnn.so` exists and matches the `abiFilters`.

- **Storage permissions**  
  Output images are written to `/storage/emulated/0/Pictures/NCNNOutput`.  
  On modern Android versions, writing here does not require legacy permissions, but behavior may vary by OEM.

---

## License
- Project code: MIT (see root `LICENSE`)
- External SDKs/models: follow their own licenses.
