#include <jni.h>
#include <string>
#include <android/log.h>

#define TAG "SCADroid-Native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_de_fisch981_scadroid_nativebridge_OpenSCADEngine_getEngineVersion(
        JNIEnv* env,
        jobject /* this */) {
    std::string version = "SCADroid Native Engine v0.1.0 (CGAL & OpenCSG)";
    LOGI("Native getEngineVersion called: %s", version.c_str());
    return env->NewStringUTF(version.c_str());
}
