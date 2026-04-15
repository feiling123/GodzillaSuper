/*
 * Godzilla \u63a7\u5236\u7aef B1 \u57fa\u51c6\u7528 JNI\u3002\u51fd\u6570\u540d\u987b\u4e0e Java \u5168\u9650\u5b9a\u540d\u7c7b\u4e00\u81f4\u3002
 * \u7f16\u8bd1: \u89c1\u540c\u76ee\u5f55 Makefile\u3002\u4ec5\u7528\u4e8e\u6388\u6743\u5b89\u5168\u6d4b\u8bd5\u3002
 */
#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define OUT_CAP (1024 * 64)

JNIEXPORT jstring JNICALL
Java_shells_plugins_java_JNINativeBenchPlugin_raspBenchNativeExec(JNIEnv *env, jclass clazz, jstring jcmd)
{
    (void) clazz;
    const char *cmd = (*env)->GetStringUTFChars(env, jcmd, NULL);
    if (cmd == NULL) {
        return NULL;
    }

#ifdef _WIN32
    FILE *fp = _popen(cmd, "r");
#else
    FILE *fp = popen(cmd, "r");
#endif
    if (fp == NULL) {
        (*env)->ReleaseStringUTFChars(env, jcmd, cmd);
        return (*env)->NewStringUTF(env, "");
    }

    char *buf = (char *) malloc(OUT_CAP);
    if (!buf) {
#ifdef _WIN32
        _pclose(fp);
#else
        pclose(fp);
#endif
        (*env)->ReleaseStringUTFChars(env, jcmd, cmd);
        return (*env)->NewStringUTF(env, "");
    }

    size_t total = 0;
    size_t n;
    while (total + 1 < OUT_CAP && (n = fread(buf + total, 1, OUT_CAP - total - 1, fp)) > 0) {
        total += n;
    }
    buf[total] = '\0';

#ifdef _WIN32
    _pclose(fp);
#else
    pclose(fp);
#endif
    (*env)->ReleaseStringUTFChars(env, jcmd, cmd);

    jstring ret = (*env)->NewStringUTF(env, buf);
    free(buf);
    return ret;
}
