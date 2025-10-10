# Real-ESRGAN Mobile

On-device super-resolution for dermatology use-cases.
Android apps with **NCNN (C++/JNI)** and **ExecuTorch (Java)**, plus reproducible benchmarking (PSNR, SSIM, MSE, LPIPS).

## 🎬 Demo (NCNN vs ExecuTorch)

<p align="center">
  <figure style="display:inline-block; margin:0 12px; text-align:center;">
    <img src="docs/demo ncnn app.gif" alt="NCNN demo" width="360"/>
    <figcaption><b>NCNN</b></figcaption>
  </figure>
  <figure style="display:inline-block; margin:0 12px; text-align:center;">
    <img src="docs/docs/demo execu app.gif" alt="ExecuTorch demo" width="360"/>
    <figcaption><b>ExecuTorch</b></figcaption>
  </figure>
</p>



## 🚀 Quick Start
- Android: open `android/ncnn-app` or `android/executorch-app` in Android Studio, place model files in `app/src/main/assets/`, build Debug, run on device.
- Reproducibility: see `notebooks/` and `scripts/` for export and metrics.


## 📁 Repository Layout
- `android/ncnn-app/` – Android app using NCNN + OpenCV  
- `android/executorch-app/` – Android app using **ExecuTorch** (Java-only)  
- `docs/` – Demo GIFs and documentation


## 🧪 Pipeline (shared)
Center crop → Gaussian blur → simulated JPEG (downscale–upscale) → resize 224×224 → normalization → Real-ESRGAN inference.

## 🧠 Models included
- **NCNN**: `.param` + `.bin` in `android/ncnn-app/app/src/main/assets/`
- **ExecuTorch**: `.pte` in `android/executorch-app/app/src/main/assets/`


## 📄 License & Citation
- Code under **MIT** (unless noted otherwise).
- `CITATION.cff` available in a later commit.
