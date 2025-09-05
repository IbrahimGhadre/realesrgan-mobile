# Models

This repository does **not** redistribute large model weights.
Follow these steps to use official Real-ESRGAN weights:

1) Download official `.pth` weights (see the Real-ESRGAN repo).
2) Convert to:
   - **NCNN** → `.param` + `.bin`
   - **ExecuTorch** → `.pte`
3) Place files under each Android app's `app/src/main/assets/`.

See the main repository README and `scripts/` for export commands.
