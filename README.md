# Real-ESRGAN Mobile

On-device super-resolution for dermatology use-cases.
Android apps with **NCNN (C++/JNI)** and **ExecuTorch (Java)**, plus reproducible benchmarking (PSNR, SSIM, MSE, LPIPS).

## 🎬 Demo (NCNN vs ExecuTorch)

<div align="center">
  <table>
    <tr>
      <td align="center">
        <img src="docs/demo_ncnn_app.gif" alt="NCNN demo" width="360"/><br/>
        <b>NCNN</b>
      </td>
      <td align="center">
        <img src="docs/demo_execu_app.gif" alt="ExecuTorch demo" width="360"/><br/>
        <b>ExecuTorch</b>
      </td>
    </tr>
  </table>
</div>




## 🚀 Quick Start
- Android: open `android/ncnn-app` or `android/executorch-app` in Android Studio, build Debug, run on device.


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
