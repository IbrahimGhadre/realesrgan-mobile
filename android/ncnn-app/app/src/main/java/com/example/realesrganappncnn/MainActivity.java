package com.example.realesrganappncnn;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;


import java.io.FileNotFoundException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;

    static {
        System.loadLibrary("realesrganappncnn");
        System.loadLibrary("opencv_java4");
    }

    private ImageView inputImageView, outputImageView;
    public native Bitmap preprocessImage(Bitmap bitmap);
    private Bitmap selectedBitmap;

    public native boolean loadModel(AssetManager assetManager);
    public native Bitmap processImage(Bitmap bitmap);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }


        inputImageView = findViewById(R.id.input_image);
        outputImageView = findViewById(R.id.output_image);
        Button selectImageButton = findViewById(R.id.select_image_button);
        Button processImageButton = findViewById(R.id.process_image_button);

        boolean modelLoaded = loadModel(getAssets());
        if (modelLoaded) {
            Log.d("NCNN", "Modello NCNN caricato correttamente!");
        } else {
            Log.e("NCNN", "Errore nel caricamento del modello NCNN.");
        }

        selectImageButton.setOnClickListener(v -> openImagePicker());

        processImageButton.setOnClickListener(v -> {
            if (selectedBitmap != null) {
                Log.d("NCNN", "Elaborazione immagine in corso...");
                Bitmap outputBitmap = processImage(selectedBitmap);
                if (outputBitmap != null) {
                    outputImageView.setImageBitmap(outputBitmap);
                    Log.d("NCNN", "Immagine elaborata con successo!");
                } else {
                    Log.e("NCNN", "Errore: l'immagine elaborata è NULL!");
                }
            } else {
                Log.e("NCNN", "Nessuna immagine selezionata.");
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(data.getData());
                selectedBitmap = BitmapFactory.decodeStream(inputStream);

                // Visualizza immagine degradata
                Bitmap degradedBitmap = preprocessImage(selectedBitmap);
                inputImageView.setImageBitmap(degradedBitmap);  // Visualizza l’immagine degradata
                Log.d("NCNN", "Immagine selezionata con successo!");
            } catch (FileNotFoundException e) {
                Log.e("NCNN", "Errore nella selezione dell'immagine", e);
            }
        }
    }
}
