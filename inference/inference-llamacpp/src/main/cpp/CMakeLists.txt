cmake_minimum_required(VERSION 3.22.1)
project("slate_inference" CXX C)

# Build llama.cpp as a subdirectory
# Disable features we don't need on Android to keep build lean
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_CURL OFF CACHE BOOL "" FORCE)

# CPU-only for MVP (no Vulkan/OpenCL)
set(GGML_VULKAN OFF CACHE BOOL "" FORCE)
set(GGML_OPENCL OFF CACHE BOOL "" FORCE)
set(GGML_METAL OFF CACHE BOOL "" FORCE)
set(GGML_CUDA OFF CACHE BOOL "" FORCE)
set(GGML_CPU ON CACHE BOOL "" FORCE)

# Enable NEON on ARM (automatic for arm64-v8a)
if(CMAKE_ANDROID_ARCH_ABI STREQUAL "arm64-v8a")
    add_compile_options(-march=armv8-a+fp+simd)
endif()

# Performance flags — do NOT use -ffast-math (llama.cpp requires non-finite math)
add_compile_options(-O3 -DNDEBUG)

add_subdirectory(llama.cpp)

# Build our JNI bridge as a shared library
add_library(slate_inference SHARED
    jni_bridge.cpp
)

target_include_directories(slate_inference PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp/include
    ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp/ggml/include
)

target_link_libraries(slate_inference
    llama
    log
)
