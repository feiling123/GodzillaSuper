#!/bin/bash
# RASP Bypass JNI Library Build Script
# Usage: ./build_jni.sh [target]
# Targets: linux-x64, linux-x86, win-x64, mac

set -e

# Configuration
JAVA_HOME=${JAVA_HOME:-"/usr/lib/jvm/java-8-openjdk-amd64"}
SRC_DIR="$(cd "$(dirname "$0")" && pwd)"
OUTPUT_DIR="${SRC_DIR}/../src/shells/plugins/java/assets"
mkdir -p "${OUTPUT_DIR}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_info() {
    echo -e "${GREEN}[+] $1${NC}"
}

print_warn() {
    echo -e "${YELLOW}[!] $1${NC}"
}

print_error() {
    echo -e "${RED}[-] $1${NC}"
}

build_linux_x64() {
    print_info "Building for Linux x64..."
    if gcc -shared -fPIC -O2 \
        -o "${OUTPUT_DIR}/rasp_bypass_linux_x64.so" \
        "${SRC_DIR}/rasp_bypass_jni.c" \
        -I"${JAVA_HOME}/include" \
        -I"${JAVA_HOME}/include/linux" \
        -static-libgcc -static-libstdc++; then
        :
    else
        print_warn "Static linking failed, using dynamic linking..."
        gcc -shared -fPIC -O2 \
            -o "${OUTPUT_DIR}/rasp_bypass_linux_x64.so" \
            "${SRC_DIR}/rasp_bypass_jni.c" \
            -I"${JAVA_HOME}/include" \
            -I"${JAVA_HOME}/include/linux"
    fi
    print_info "Linux x64 build complete: ${OUTPUT_DIR}/rasp_bypass_linux_x64.so"
}

build_linux_x86() {
    print_info "Building for Linux x86..."
    if ! gcc -shared -fPIC -m32 -O2 \
        -o "${OUTPUT_DIR}/rasp_bypass_linux_x86.so" \
        "${SRC_DIR}/rasp_bypass_jni.c" \
        -I"${JAVA_HOME}/include" \
        -I"${JAVA_HOME}/include/linux"; then
        print_error "Linux x86 build failed. Install gcc-multilib: apt-get install gcc-multilib g++-multilib"
        return 1
    fi
    print_info "Linux x86 build complete: ${OUTPUT_DIR}/rasp_bypass_linux_x86.so"
}

build_win_x64() {
    print_info "Building for Windows x64..."
    if command -v x86_64-w64-mingw32-gcc &> /dev/null; then
        if ! x86_64-w64-mingw32-gcc -shared -O2 \
            -o "${OUTPUT_DIR}/rasp_bypass_win_x64.dll" \
            "${SRC_DIR}/rasp_bypass_jni.c" \
            -I"${JAVA_HOME}/include" \
            -I"${JAVA_HOME}/include/win32"; then
            print_error "Windows x64 build failed"
            return 1
        fi
        print_info "Windows x64 build complete: ${OUTPUT_DIR}/rasp_bypass_win_x64.dll"
    else
        print_error "MinGW-w64 not found. Install: apt-get install mingw-w64"
        return 1
    fi
}

build_win_x86() {
    print_info "Building for Windows x86..."
    if command -v i686-w64-mingw32-gcc &> /dev/null; then
        if ! i686-w64-mingw32-gcc -shared -O2 \
            -o "${OUTPUT_DIR}/rasp_bypass_win_x86.dll" \
            "${SRC_DIR}/rasp_bypass_jni.c" \
            -I"${JAVA_HOME}/include" \
            -I"${JAVA_HOME}/include/win32"; then
            print_error "Windows x86 build failed"
            return 1
        fi
        print_info "Windows x86 build complete: ${OUTPUT_DIR}/rasp_bypass_win_x86.dll"
    else
        print_error "MinGW-w64 not found. Install: apt-get install mingw-w64"
        return 1
    fi
}

build_mac() {
    print_info "Building for macOS..."
    if [[ "$OSTYPE" == "darwin"* ]]; then
        if ! gcc -shared -fPIC -O2 \
            -o "${OUTPUT_DIR}/rasp_bypass_mac.so" \
            "${SRC_DIR}/rasp_bypass_jni.c" \
            -I"${JAVA_HOME}/include" \
            -I"${JAVA_HOME}/include/darwin"; then
            print_error "macOS build failed"
            return 1
        fi
        print_info "macOS build complete: ${OUTPUT_DIR}/rasp_bypass_mac.so"
    else
        print_error "macOS build can only be done on macOS"
        return 1
    fi
}

build_all() {
    print_info "Building all targets..."
    build_linux_x64 || true
    build_linux_x86 || true
    build_win_x64 || true
    build_win_x86 || true
    build_mac || true
    print_info "Build complete!"
}

# Main
case "$1" in
    linux-x64)
        build_linux_x64
        ;;
    linux-x86)
        build_linux_x86
        ;;
    win-x64)
        build_win_x64
        ;;
    win-x86)
        build_win_x86
        ;;
    mac)
        build_mac
        ;;
    all|"")
        build_all
        ;;
    *)
        print_error "Unknown target: $1"
        echo "Usage: $0 [linux-x64|linux-x86|win-x64|win-x86|mac|all]"
        exit 1
        ;;
esac
