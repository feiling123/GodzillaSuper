#!/bin/bash
# RASP Bypass Module Compile Script for Linux/macOS
# Compiles the server-side module and packages it as .classs file

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="${SCRIPT_DIR}/src/shells/plugins/java/assets"
OUTPUT_DIR="${SCRIPT_DIR}/src/shells/plugins/java/assets"

echo "========================================"
echo "RASP Bypass Module Compile Script"
echo "========================================"
echo

# Find JAVA_HOME
if [ -z "$JAVA_HOME" ]; then
    JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java) 2>/dev/null || echo "/usr/bin/java") 2>/dev/null))
fi

if [ ! -d "$JAVA_HOME" ]; then
    echo "[-] JAVA_HOME not found. Please set JAVA_HOME environment variable."
    exit 1
fi

echo "[+] JAVA_HOME: $JAVA_HOME"

# Check if javac exists
JAVAC="${JAVA_HOME}/bin/javac"
if [ ! -x "$JAVAC" ]; then
    echo "[-] javac not found at $JAVAC"
    exit 1
fi

echo "[+] Compiling RaspBypassModule.java..."

# Compile the module
"$JAVAC" -source 1.8 -target 1.8 -encoding UTF-8 -d "$OUTPUT_DIR" "${SRC_DIR}/RaspBypassModule.java" 2>&1

echo "[+] Compilation successful!"

# Check if class file exists
CLASS_FILE="${OUTPUT_DIR}/shells/plugins/java/assets/RaspBypassModule.class"
if [ -f "$CLASS_FILE" ]; then
    echo "[+] Class file created: $CLASS_FILE"
    
    # Copy to .classs file (Godzilla format)
    cp -f "$CLASS_FILE" "${OUTPUT_DIR}/RaspBypassModule.classs"
    
    echo "[+] Packaged as: ${OUTPUT_DIR}/RaspBypassModule.classs"
    
    # Clean up directory structure
    rm -rf "${OUTPUT_DIR}/shells"
    
    echo "[+] Cleanup complete!"
else
    echo "[-] Class file not found!"
    exit 1
fi

echo
echo "========================================"
echo "Build completed successfully!"
echo "========================================"
