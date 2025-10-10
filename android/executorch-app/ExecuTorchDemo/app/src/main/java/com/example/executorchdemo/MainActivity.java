package com.example.executorchdemo;

import android.content.Intent;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import org.pytorch.executorch.EValue;
import org.pytorch.executorch.Module;
import org.pytorch.executorch.Tensor;

import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SELECT_IMAGE = 1;
    private Module module;
    private Bitmap degradedBitmap;
    private ImageView imageViewInput;
    private ImageView imageViewOutput;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageViewInput = findViewById(R.id.input_image);
        imageViewOutput = findViewById(R.id.output_image);
        Button selectImageButton = findViewById(R.id.select_image_button);
        Button enhanceImageButton = findViewById(R.id.process_image_button);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 100);
            }
        } else {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
            }
        }

        selectImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
        });

        enhanceImageButton.setOnClickListener(v -> {
            if (degradedBitmap == null || module == null) return;
            performInference(degradedBitmap);
        });

        try {
            AssetManager assetManager = getAssets();
            InputStream inputStream = assetManager.open("realesrgan_x4_exported_execu.pte");
            File outFile = new File(getFilesDir(), "realesrgan_x4_exported_execu.pte");
            try (OutputStream outputStream = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
            }
            module = Module.load(outFile.getAbsolutePath());
            Log.d("ExecuTorchDebug", "Modulo caricato correttamente.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            new Thread(() -> {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();
                    int size = Math.min(width, height);
                    int xOffset = (width - size) / 2;
                    int yOffset = (height - size) / 2;
                    Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, xOffset, yOffset, size, size);

                    Bitmap downscaled = Bitmap.createScaledBitmap(croppedBitmap, size / 2, size / 2, true);
                    Bitmap upscaled = Bitmap.createScaledBitmap(downscaled, size, size, true);

                    Bitmap blurred = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                    for (int y = 1; y < size - 1; y++) {
                        for (int x = 1; x < size - 1; x++) {
                            int sumR = 0, sumG = 0, sumB = 0;
                            for (int dy = -1; dy <= 1; dy++) {
                                for (int dx = -1; dx <= 1; dx++) {
                                    int pixel = upscaled.getPixel(x + dx, y + dy);
                                    sumR += (pixel >> 16) & 0xFF;
                                    sumG += (pixel >> 8) & 0xFF;
                                    sumB += pixel & 0xFF;
                                }
                            }
                            int avgR = sumR / 9;
                            int avgG = sumG / 9;
                            int avgB = sumB / 9;
                            int color = 0xFF000000 | (avgR << 16) | (avgG << 8) | avgB;
                            blurred.setPixel(x, y, color);
                        }
                    }

                    degradedBitmap = Bitmap.createScaledBitmap(blurred, 224, 224, true);

                    runOnUiThread(() -> imageViewInput.setImageBitmap(degradedBitmap));

                } catch (Exception e) {
                    Log.e("ExecuTorchDebug", "Errore nel preprocessing:", e);
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void performInference(Bitmap inputBitmap) {
        new Thread(() -> {
            try {
                float[] inputData = new float[3 * 224 * 224];
                int offsetR = 0, offsetG = 224 * 224, offsetB = 2 * 224 * 224;

                for (int y = 0; y < 224; y++) {
                    for (int x = 0; x < 224; x++) {
                        int pixel = inputBitmap.getPixel(x, y);
                        int r = (pixel >> 16) & 0xFF;
                        int g = (pixel >> 8) & 0xFF;
                        int b = pixel & 0xFF;
                        int idx = y * 224 + x;
                        inputData[offsetR + idx] = r / 255.0f;
                        inputData[offsetG + idx] = g / 255.0f;
                        inputData[offsetB + idx] = b / 255.0f;
                    }
                }

                Tensor inputTensor = Tensor.fromBlob(inputData, new long[]{1, 3, 224, 224});
                EValue[] input = new EValue[]{EValue.from(inputTensor)};

                long startTime = System.nanoTime();
                EValue[] result = module.forward(input);
                long endTime = System.nanoTime();
                double elapsedMillis = (endTime - startTime) / 1_000_000.0;

                Tensor outputTensor = result[0].toTensor();
                float[] outputData = outputTensor.getDataAsFloatArray();
                long[] shape = outputTensor.shape();
                int outH = (int) shape[2], outW = (int) shape[3];
                int pixels = outH * outW;

                Bitmap outputBitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888);
                for (int y = 0; y < outH; y++) {
                    for (int x = 0; x < outW; x++) {
                        int idx = y * outW + x;
                        float r = Math.max(0f, Math.min(1f, outputData[idx]));
                        float g = Math.max(0f, Math.min(1f, outputData[pixels + idx]));
                        float b = Math.max(0f, Math.min(1f, outputData[2 * pixels + idx]));
                        int color = 0xFF000000 | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
                        outputBitmap.setPixel(x, y, color);
                    }
                }

                String savedPath = "N/D";
                try {
                    File dir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "ExecutorchOutput");
                    if (!dir.exists()) dir.mkdirs();
                    File file = new File(dir, "executorch_output.jpg");
                    FileOutputStream out = new FileOutputStream(file);
                    outputBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.flush(); out.close();
                    savedPath = file.getAbsolutePath();
                } catch (Exception e) {
                    Log.e("ExecuTorchDebug", "Errore salvataggio file:", e);
                }

                String finalSavedPath = savedPath;

                runOnUiThread(() -> {
                    imageViewOutput.setImageBitmap(outputBitmap);
                    Log.d("ExecuTorchDebug", "Immagine upscalata");
                    Log.d("ExecuTorchDebug", "Tempo inferenza: " + String.format("%.2f ms", elapsedMillis));
                    Log.d("ExecuTorchDebug", "Salvata in: " + finalSavedPath);
                });


            } catch (Exception e) {
                Log.e("ExecuTorchDebug", "Errore durante inferenza:", e);
                e.printStackTrace();
            }
        }).start();
    }
}
