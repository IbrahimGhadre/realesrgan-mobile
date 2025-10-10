# Real-ESRGAN Mobile

On-device super-resolution for dermatology use-cases.
Android apps with **NCNN (C++/JNI)** and **ExecuTorch (Java)**, plus reproducible benchmarking (PSNR, SSIM, MSE, LPIPS).

## 🎬 Demo

<p align="center">
  <img src="docs/demo ncnn app.gif" alt="Real-ESRGAN Mobile Demo" width="320"/>
</p>

This short demo shows the **Real-ESRGAN Mobile (NCNN)** app running on an Android device.  
The app performs single-image super-resolution in real time, with preprocessing, NCNN inference, and result saving to `/Pictures/NCNNOutput`.


## 🚀 Quick Start
- Android: open `android/ncnn-app` or `android/executorch-app` in Android Studio, place model files in `app/src/main/assets/`, build Debug, run on device.
- Reproducibility: see `notebooks/` and `scripts/` for export and metrics.

## 📁 Repository Layout
- `android/ncnn-app/` – Android app using NCNN + OpenCV  
- `models/` – Model setup and conversion instructions  
- `docs/` – Demo GIF and future documentation  

> ExecuTorch app, notebooks and scripts will be added later.


## 🧪 Pipeline (shared)
Center crop → Gaussian blur → simulated JPEG (downscale–upscale) → resize 224×224 → normalization → Real-ESRGAN inference.

## 🔗 Models
See `models/README_MODELS.md` for how to download/convert official weights to NCNN (`.param/.bin`) and ExecuTorch (`.pte`).

## 📄 License & Citation
- Code under **MIT** (unless noted otherwise).
- `CITATION.cff` available in a later commit.
