#include <jni.h>
#include <string>
#include <android/log.h>
#include <android/bitmap.h>
#include <fstream>
#include "net.h"
#include "layer.h"
#include "android/asset_manager.h"
#include "android/asset_manager_jni.h"
#include <chrono>
#include <opencv2/opencv.hpp>
#include <sys/stat.h>   // Per mkdir()
#include <sys/types.h>
#include <ctime>
#include <sstream>

using namespace cv;


// Macro per logging
#define LOG_TAG "RealESRGAN-NCNN"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Dichiarazione del modello NCNN
ncnn::Net realesrgan;

// Memorizza se Vulkan è disponibile (evita chiamate multiple a get_gpu_count)
bool isVulkanAvailable = false;

// Funzione per copiare un file dagli assets alla memoria locale
bool copyAssetToFile(AAssetManager* mgr, const char* assetName, const char* outPath) {
    AAsset* asset = AAssetManager_open(mgr, assetName, AASSET_MODE_BUFFER);
    if (!asset) {
        LOGE("Errore: impossibile aprire asset %s", assetName);
        return false;
    }

    std::ofstream outFile(outPath, std::ios::binary);
    if (!outFile) {
        LOGE("Errore: impossibile creare file %s", outPath);
        AAsset_close(asset);
        return false;
    }

    const void* buffer = AAsset_getBuffer(asset);
    size_t size = AAsset_getLength(asset);
    outFile.write(reinterpret_cast<const char*>(buffer), size);

    AAsset_close(asset);
    outFile.close();
    return true;
}

std::string getCurrentTimestampString() {
    std::time_t now = std::time(nullptr);
    std::tm* localTime = std::localtime(&now);
    std::ostringstream oss;
    oss << std::put_time(localTime, "%Y%m%d_%H%M%S");  // es. 20250703_1214
    return oss.str();
}



// Funzione per caricare il modello
extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_realesrganappncnn_MainActivity_loadModel(JNIEnv *env, jobject instance, jobject assetManager) {
    AAssetManager* mgr = AAssetManager_fromJava(env, assetManager);
    if (!mgr) {
        LOGE("Errore: impossibile ottenere AAssetManager");
        return JNI_FALSE;
    }

    const char* paramAsset = "realesr_general_x4v3.ncnn.param";
    const char* binAsset = "realesr_general_x4v3.ncnn.bin";

    const char* paramFile = "/data/data/com.example.realesrganappncnn/files/realesr_general_x4v3.ncnn.param";
    const char* binFile = "/data/data/com.example.realesrganappncnn/files/realesr_general_x4v3.ncnn.bin";

    // Copia i file dagli assets alla memoria locale
    if (!copyAssetToFile(mgr, paramAsset, paramFile) || !copyAssetToFile(mgr, binAsset, binFile)) {
        return JNI_FALSE;
    }

    LOGD("Caricamento modello NCNN: %s e %s", paramFile, binFile);

    // Carica i file nel modello NCNN
    if (realesrgan.load_param(paramFile) != 0) {
        LOGE("Errore nel caricamento del file .param");
        return JNI_FALSE;
    }
    if (realesrgan.load_model(binFile) != 0) {
        LOGE("Errore nel caricamento del file .bin");
        return JNI_FALSE;
    }

    LOGD("Modello caricato con successo!");
    return JNI_TRUE;
}

// Funzione per elaborare un'immagine con Real-ESRGAN
extern "C" JNIEXPORT jobject JNICALL
Java_com_example_realesrganappncnn_MainActivity_processImage(JNIEnv *env, jobject instance, jobject bitmap) {
    AndroidBitmapInfo info;
    void* pixels;

    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0 || info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Formato Bitmap non supportato!");
        return nullptr;
    }

    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
        LOGE("Errore nel lock dei pixel del Bitmap!");
        return nullptr;
    }

    // Converti in Mat OpenCV (RGBA)
    cv::Mat rgba(info.height, info.width, CV_8UC4, pixels);
    cv::Mat rgb;
    cv::cvtColor(rgba, rgb, cv::COLOR_RGBA2RGB);
    AndroidBitmap_unlockPixels(env, bitmap);

    // CROP centrale per rendere l'immagine quadrata
    int cropSize = std::min(rgb.cols, rgb.rows);
    int x = (rgb.cols - cropSize) / 2;
    int y = (rgb.rows - cropSize) / 2;
    cv::Rect roi(x, y, cropSize, cropSize);
    cv::Mat cropped = rgb(roi);

    // BLUR gaussiano
    cv::Mat blurred;
    cv::GaussianBlur(cropped, blurred, cv::Size(3, 3), 0);

    // Simula COMPRESSIONE JPEG: downscale + upscale
    cv::Mat downscaled, restored;
    cv::resize(blurred, downscaled, cv::Size(cropSize / 2, cropSize / 2), 0, 0, cv::INTER_LINEAR);
    cv::resize(downscaled, restored, cv::Size(cropSize, cropSize), 0, 0, cv::INTER_LINEAR);

    // RIDIMENSIONAMENTO a 224x224
    cv::Mat resized;
    cv::resize(restored, resized, cv::Size(224, 224), 0, 0, cv::INTER_LINEAR);

    // SALVATAGGIO immagine degradata con timestamp
    std::string degradedDir = "/storage/emulated/0/Pictures/NCNNOutput";
    mkdir(degradedDir.c_str(), 0777);  // Crea la cartella se non esiste

    std::string timestamp = getCurrentTimestampString();
    std::string degradedPath = degradedDir + "/ncnn_input_" + timestamp + ".jpg";
    // resized è RGB (224x224)
    cv::Mat degradedBGR;
    cv::cvtColor(resized, degradedBGR, cv::COLOR_RGB2BGR);
    cv::imwrite(degradedPath, resized);
    LOGD("Input degradato salvato in: %s", degradedPath.c_str());


    // CONVERSIONE in ncnn::Mat
    ncnn::Mat in = ncnn::Mat::from_pixels(resized.data, ncnn::Mat::PIXEL_RGB, 224, 224);
    const float norm_vals[3] = {1.f / 255.f, 1.f / 255.f, 1.f / 255.f};
    in.substract_mean_normalize(0, norm_vals);

    // INFERENZA
    ncnn::Option opt;
    opt.num_threads = 4;
    opt.use_vulkan_compute = isVulkanAvailable;
    realesrgan.opt = opt;

    ncnn::Extractor ex = realesrgan.create_extractor();
    auto start = std::chrono::high_resolution_clock::now();
    ex.input("in0", in);
    ncnn::Mat out;
    if (ex.extract("out0", out) != 0) {
        LOGE("Errore nell'estrazione del risultato!");
        return nullptr;
    }
    auto end = std::chrono::high_resolution_clock::now();
    LOGD("Tempo inferenza NCNN: %.2f ms", std::chrono::duration<double, std::milli>(end - start).count());

    // CONVERSIONE output in Bitmap
    jclass bitmapClass = env->FindClass("android/graphics/Bitmap");
    jmethodID createBitmapMethod = env->GetStaticMethodID(bitmapClass, "createBitmap", "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    jclass configClass = env->FindClass("android/graphics/Bitmap$Config");
    jfieldID argb8888Field = env->GetStaticFieldID(configClass, "ARGB_8888", "Landroid/graphics/Bitmap$Config;");
    jobject argb8888 = env->GetStaticObjectField(configClass, argb8888Field);
    jobject bitmapOutput = env->CallStaticObjectMethod(bitmapClass, createBitmapMethod, out.w, out.h, argb8888);

    void* outputPixels;
    if (AndroidBitmap_lockPixels(env, bitmapOutput, &outputPixels) < 0) {
        LOGE("Errore nel lock del Bitmap di output!");
        return nullptr;
    }

    const float denorm_vals[3] = {255.f, 255.f, 255.f};
    out.substract_mean_normalize(0, denorm_vals);

    // SALVATAGGIO immagine output con timestamp
    std::string outputDir = "/storage/emulated/0/Pictures/NCNNOutput";
    mkdir(outputDir.c_str(), 0777);
    std::string outputPath = outputDir + "/ncnn_output_" + timestamp + ".jpg";

    // Converti ncnn::Mat in cv::Mat RGBA, poi in BGR
    cv::Mat outputMatRGBA(out.h, out.w, CV_8UC4);
    out.to_pixels(outputMatRGBA.data, ncnn::Mat::PIXEL_RGB2RGBA);

    cv::Mat outputMatBGR;
    cv::cvtColor(outputMatRGBA, outputMatBGR, cv::COLOR_RGBA2BGR);

    cv::imwrite(outputPath, outputMatBGR);
    LOGD("Output salvato in: %s", outputPath.c_str());



    out.to_pixels((unsigned char*)outputPixels, ncnn::Mat::PIXEL_RGB2RGBA);
    AndroidBitmap_unlockPixels(env, bitmapOutput);

    return bitmapOutput;
}


extern "C" JNIEXPORT jobject JNICALL
Java_com_example_realesrganappncnn_MainActivity_preprocessImage(JNIEnv *env, jobject instance, jobject bitmap) {
    AndroidBitmapInfo info;
    void* pixels;

    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0 || info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Formato Bitmap non supportato!");
        return nullptr;
    }

    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
        LOGE("Errore nel lock dei pixel del Bitmap!");
        return nullptr;
    }

    cv::Mat rgba(info.height, info.width, CV_8UC4, pixels);
    cv::Mat rgb;
    cv::cvtColor(rgba, rgb, cv::COLOR_RGBA2RGB);
    AndroidBitmap_unlockPixels(env, bitmap);

    // CROP centrale
    int cropSize = std::min(rgb.cols, rgb.rows);
    int x = (rgb.cols - cropSize) / 2;
    int y = (rgb.rows - cropSize) / 2;
    cv::Rect roi(x, y, cropSize, cropSize);
    cv::Mat cropped = rgb(roi);

    // BLUR
    cv::Mat blurred;
    cv::GaussianBlur(cropped, blurred, cv::Size(3, 3), 0);

    // COMPRESSIONE simulata
    cv::Mat downscaled, restored;
    cv::resize(blurred, downscaled, cv::Size(cropSize / 2, cropSize / 2));
    cv::resize(downscaled, restored, cv::Size(cropSize, cropSize));

    // RESIZE a 224x224
    cv::Mat resized;
    cv::resize(restored, resized, cv::Size(224, 224));

    // Converti in Bitmap ARGB_8888 da mostrare
    cv::Mat rgba_out;
    cv::cvtColor(resized, rgba_out, cv::COLOR_RGB2RGBA);

    // Crea Bitmap Java
    jclass bitmapClass = env->FindClass("android/graphics/Bitmap");
    jmethodID createBitmapMethod = env->GetStaticMethodID(bitmapClass, "createBitmap",
                                                          "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    jclass configClass = env->FindClass("android/graphics/Bitmap$Config");
    jfieldID argb8888Field = env->GetStaticFieldID(configClass, "ARGB_8888", "Landroid/graphics/Bitmap$Config;");
    jobject argb8888 = env->GetStaticObjectField(configClass, argb8888Field);
    jobject outputBitmap = env->CallStaticObjectMethod(bitmapClass, createBitmapMethod, 224, 224, argb8888);

    void* outputPixels;
    if (AndroidBitmap_lockPixels(env, outputBitmap, &outputPixels) < 0) {
        LOGE("Errore nel lock dell'immagine di output!");
        return nullptr;
    }

    memcpy(outputPixels, rgba_out.data, 224 * 224 * 4);
    AndroidBitmap_unlockPixels(env, outputBitmap);

    return outputBitmap;
}




extern "C" JNIEXPORT jstring JNICALL
Java_com_example_realesrganappncnn_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++!";
    return env->NewStringUTF(hello.c_str());
}