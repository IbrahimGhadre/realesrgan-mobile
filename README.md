# Real-ESRGAN Mobile

On-device super-resolution for dermatology use-cases â€” Android apps with **NCNN (C++/JNI)** and **ExecuTorch (Java)**, plus reproducible benchmarking (PSNR, SSIM, MSE, LPIPS).

## íº€ Quick Start
- Android: open `android/ncnn-app` or `android/executorch-app` in Android Studio, place model files in `app/src/main/assets/`, build Debug, run on device.
- Reproducibility: see `notebooks/` and `scripts/` for export and metrics.

## í³¦ Repository Layout
- `android/â€¦` â€“ two Android apps (NCNN & ExecuTorch)
- `notebooks/` â€“ Jupyter notebooks (export & evaluation)
- `scripts/` â€“ Python utilities (export, metrics)
- `models/` â€“ model download/convert instructions (no large binaries)
- `assets/` â€“ sample images & screenshots
- `docs/` â€“ figures & results
- `.github/workflows/` â€“ CI pipelines (build APKs)

## í·ª Pipeline (shared)
Center crop â†’ Gaussian blur â†’ simulated JPEG (downscaleâ€“upscale) â†’ resize 224Ã—224 â†’ normalization â†’ Real-ESRGAN inference.

## í´— Models
See `models/README_MODELS.md` for how to download/convert official weights to NCNN (`.param/.bin`) and ExecuTorch (`.pte`).

## í³„ License & Citation
- Code under **MIT** (unless noted otherwise).
- `CITATION.cff` available in a later commit.
