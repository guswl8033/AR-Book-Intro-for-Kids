LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := native-lib
LOCAL_SRC_FILES := HelloWorld.cpp
LOCAL_CFLAGS = -DSTDC_HEADERS


include $(BUILD_SHARED_LIBRARY)