#include "com_google_ar_sceneform_samples_augmentedimage_first.h"

extern "C"
JNIEXPORT jint JNICALL
Java_com_google_ar_sceneform_samples_augmentedimage_first_helloNDK(JNIEnv *env, jobject instance,
                                                                   jint v) {

    jint a = v + 10;

    return a;

}