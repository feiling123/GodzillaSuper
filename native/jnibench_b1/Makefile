# B1 JNI for shells.plugins.java.JNINativeBenchPlugin (static native -> jclass in JNI)
# Linux: produces libjnibench_b1.so — copy path into Godzilla plugin UI.
#
#   export JAVA_HOME=/path/to/jdk
#   make linux
#
# Windows (MinGW x64): mingw32-make win MINGW=x86_64-w64-mingw32

JAVA_HOME ?= $(shell java -XshowSettings:properties -version 2>&1 | sed -n 's/.*java.home = //p' | head -1)
INCLUDES = -I"$(JAVA_HOME)/include"

linux: INCLUDES += -I"$(JAVA_HOME)/include/linux"
linux: CFLAGS = -fPIC -shared -O2 -Wall
linux: libjnibench_b1.so

libjnibench_b1.so: JNINativeBenchPlugin.c
	gcc $(CFLAGS) $(INCLUDES) -o $@ $<

# MinGW: produce jnibench_b1.dll
win: INCLUDES += -I"$(JAVA_HOME)/include/win32"
win: CFLAGS = -shared -O2 -Wall
win: jnibench_b1.dll

jnibench_b1.dll: JNINativeBenchPlugin.c
	$(MINGW)-gcc $(CFLAGS) $(INCLUDES) -o $@ $<

clean:
	rm -f libjnibench_b1.so jnibench_b1.dll

.PHONY: linux win clean
