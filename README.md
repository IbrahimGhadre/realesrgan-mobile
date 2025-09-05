# Real-ESRGAN Mobile

On-device super-resolution for dermatology use-cases.
Android apps with **NCNN (C++/JNI)** and **ExecuTorch (Java)**, plus reproducible benchmarking (PSNR, SSIM, MSE, LPIPS).

## 🚀 Quick Start
- Android: open `android/ncnn-app` or `android/executorch-app` in Android Studio, place model files in `app/src/main/assets/`, build Debug, run on device.
- Reproducibility: see `notebooks/` and `scripts/` for export and metrics.

## 📦 Repository Layout
- `android/…` – two Android apps (NCNN & ExecuTorch)
- `notebooks/` – Jupyter notebooks (export & evaluation)
- `scripts/` – Python utilities (export, metrics)
- `models/` – model download/convert instructions (no large binaries)
- `assets/` – sample images & screenshots
- `docs/` – figures & results
- `.github/workflows/` – CI pipelines (build APKs)

## 🧪 Pipeline (shared)
Center crop → Gaussian blur → simulated JPEG (downscale–upscale) → resize 224×224 → normalization → Real-ESRGAN inference.

## 🔗 Models
See `models/README_MODELS.md` for how to download/convert official weights to NCNN (`.param/.bin`) and ExecuTorch (`.pte`).

## 📄 License & Citation
- Code under **MIT** (unless noted otherwise).
- `CITATION.cff` available in a later commit.
