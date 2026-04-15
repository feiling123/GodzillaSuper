package shells.payloads.java.modules;

import java.io.File;
import java.util.Map;

public class MoveFileModule {
    private Map session;
    private Object servletRequest;

    private String getString(String key) {
        Object value = this.session != null ? this.session.get(key) : null;
        if (value instanceof byte[]) {
            return new String((byte[])value);
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
        String src = this.getString("srcFileName");
        String dest = this.getString("destFileName");
        if (src != null && dest != null) {
            File srcFile = new File(src);

            try {
                if (srcFile.exists()) {
                    return srcFile.renameTo(new File(dest)) ? "ok".getBytes() : "fail".getBytes();
                } else {
                    return "The target does not exist".getBytes();
                }
            } catch (Exception e) {
                StringBuffer sb = new StringBuffer();
                sb.append("Exception errMsg:");
                sb.append(e.getMessage());
                return sb.toString().getBytes();
            }
        } else {
            return "No parameter srcFileName,destFileName".getBytes();
        }
    }

    public String getModuleName() {
        return "moveFile";
    }
}
