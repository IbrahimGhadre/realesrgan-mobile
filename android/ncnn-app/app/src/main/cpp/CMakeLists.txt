# Minimum CMake version for Android Studio toolchain
cmake_minimum_required(VERSION 3.22.1)

# Project name (matches your module)
project("realesrganappncnn")

# --- Debug prints (optional)
message(STATUS "CMAKE_SOURCE_DIR: ${CMAKE_SOURCE_DIR}")
message(STATUS "ANDROID_ABI: ${ANDROID_ABI}")

# --- C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# =============================================================================
# Third-party dependencies (OpenCV + NCNN)
# We keep large SDKs/libraries out of app/src and under a non-versioned folder.
# Expected layout (relative to this CMakeLists.txt at app/src/main/cpp):
#   ../../../../third_party/opencv-4.11.0-android-sdk/
#   ../../../../libs/<abi>/libncnn.so
#   ../../../../include/ncnn/ (headers)
# =============================================================================

# Paths to third_party and libs/include (relative to this file)
set(THIRD_PARTY_DIR         ${CMAKE_SOURCE_DIR}/../../../../third_party)
set(NCNN_INCLUDE_DIR        ${CMAKE_SOURCE_DIR}/include/ncnn)
set(NCNN_LIB_DIR            ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI})
set(NCNN_LIB                ${NCNN_LIB_DIR}/libncnn.so)

# --- OpenCV Android SDK (use native sdk folder)
set(OPENCV_SDK_DIR          ${THIRD_PARTY_DIR}/opencv-4.11.0-android-sdk/OpenCV-android-sdk/sdk/native)
set(OPENCV_INCLUDE_DIR      ${OPENCV_SDK_DIR}/jni/include)
set(OPENCV_LIB_DIR          ${OPENCV_SDK_DIR}/libs/${ANDROID_ABI})
set(OPENCV_JAVA_LIB         ${OPENCV_LIB_DIR}/libopencv_java4.so)

# --- Sanity checks with friendly errors
if(NOT EXISTS "${NCNN_LIB}")
    message(FATAL_ERROR
            "NCNN shared library not found: ${NCNN_LIB}\n"
            "Please place libncnn.so under: ${NCNN_LIB_DIR}\n"
            "and NCNN headers under: ${NCNN_INCLUDE_DIR}")
endif()

if(NOT EXISTS "${OPENCV_INCLUDE_DIR}/opencv2/core.hpp")
    message(FATAL_ERROR
            "OpenCV headers not found at: ${OPENCV_INCLUDE_DIR}\n"
            "Expected OpenCV Android SDK under: ${THIRD_PARTY_DIR}/opencv-4.11.0-android-sdk/")
endif()

if(NOT EXISTS "${OPENCV_JAVA_LIB}")
    message(FATAL_ERROR
            "OpenCV native library not found for ABI ${ANDROID_ABI}: ${OPENCV_JAVA_LIB}\n"
            "Check your OpenCV Android SDK libs folder.")
endif()

# --- Header include directories
include_directories(${NCNN_INCLUDE_DIR})
include_directories(${OPENCV_INCLUDE_DIR})

# (Optional) link directories; we also import with absolute paths below
link_directories(${NCNN_LIB_DIR})
link_directories(${OPENCV_LIB_DIR})

# --- OpenMP (before building the target)
find_package(OpenMP REQUIRED)

# --- Vulkan (optional, only if you enable NCNN_VULKAN in Gradle/CMake)
if (NCNN_VULKAN)
    find_package(Vulkan REQUIRED)
endif()

# =============================================================================
# Prebuilt/imported libraries
# =============================================================================

# NCNN prebuilt .so
add_library(ncnn SHARED IMPORTED)
set_target_properties(ncnn PROPERTIES
        IMPORTED_LOCATION "${NCNN_LIB}"
)

# OpenCV prebuilt .so (Java binding loads other OpenCV modules internally)
add_library(opencv_java4 SHARED IMPORTED)
set_target_properties(opencv_java4 PROPERTIES
        IMPORTED_LOCATION "${OPENCV_JAVA_LIB}"
)

# =============================================================================
# Your native library
# Add all your native sources here (add more .cpp if needed)
# =============================================================================
add_library(${CMAKE_PROJECT_NAME} SHARED
        native-lib.cpp
)

# --- ✅ NEW: resolve the shared C++ runtime from the NDK
find_library(CPP_SHARED NAMES c++_shared)

# Optional: fail early if not found (rare)
if(NOT CPP_SHARED)
    message(FATAL_ERROR "Could not find c++_shared from the NDK toolchain.")
endif()


# =============================================================================
# Link everything
# Order: your lib, third-party libs, system libs
# =============================================================================
target_link_libraries(${CMAKE_PROJECT_NAME}
        # Third-party
        ncnn
        opencv_java4
        OpenMP::OpenMP_CXX

        # Optional Vulkan
        $<$<BOOL:${NCNN_VULKAN}>:Vulkan::Vulkan>

        # Android system libs
        log
        android
        jnigraphics

        # ✅ Use the resolved STL so Gradle packs libc++_shared.so into the APK
        ${CPP_SHARED}

)
