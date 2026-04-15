package core;

import core.persist.OperationLogStore;
import core.shell.ShellEntity;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;

public final class OperationAuditLog {
    private static final int MAX_ENTRIES = 50000;
    private static final int MAX_DETAIL_CHARS = 20000;
    private static final ArrayDeque<String> ENTRIES = new ArrayDeque<>(1024);
    private static final Object LOCK = new Object();
    private static volatile String boundSessionId;

    private OperationAuditLog() {
    }

    public static void bindSessionForPersistence(String sessionId) {
        boundSessionId = sessionId;
    }

    public static void clear() {
        synchronized (LOCK) {
            if (OperationLogStore.isAvailable() && boundSessionId != null) {
                OperationLogStore.clearSessionEntries(boundSessionId);
            }
            ENTRIES.clear();
        }
    }

    public static String getFullText() {
        synchronized (LOCK) {
            if (OperationLogStore.isAvailable() && boundSessionId != null) {
                return OperationLogStore.loadSessionText(boundSessionId);
            }
            if (ENTRIES.isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder(ENTRIES.size() * 128);
            for (String e : ENTRIES) {
                sb.append(e).append("\n\n");
            }
            return sb.toString();
        }
    }

    public static void ui(String category, String summary, String detail) {
        appendBlock(formatBlock(null, category, summary, detail));
    }

    public static void fileOp(ShellEntity shell, String op, String path, String note) {
        appendBlock(formatBlock(shell, op, path == null ? "" : path, note == null ? "" : note));
    }

    public static void exec(ShellEntity shell, String commandLine, String output) {
        String out = trimDetail(output, MAX_DETAIL_CHARS);
        appendBlock(formatBlock(shell, "\u547d\u4ee4\u6267\u884c", commandLine == null ? "" : commandLine, out));
    }

    private static String formatBlock(ShellEntity shell, String category, String summary, String detail) {
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String sid = shell != null ? shell.getId() : "\u672c\u5730";
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(time).append("] [").append(sid).append("] [").append(category).append("] ");
        sb.append(summary == null ? "" : summary);
        if (detail != null && !detail.isEmpty()) {
            sb.append('\n').append(detail);
        }
        return sb.toString();
    }

    private static void appendBlock(String block) {
        synchronized (LOCK) {
            if (OperationLogStore.isAvailable() && boundSessionId != null) {
                OperationLogStore.appendEntry(boundSessionId, block);
                OperationLogRuntime.notifyChunk(boundSessionId, block + "\n\n");
            } else {
                while (ENTRIES.size() >= MAX_ENTRIES) {
                    ENTRIES.removeFirst();
                }
                ENTRIES.addLast(block);
                OperationLogRuntime.notifyChunk(boundSessionId, block + "\n\n");
            }
        }
    }

    private static String trimDetail(String s, int max) {
        if (s == null) {
            return "";
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "\n... (\u5df2\u622a\u65ad\uff0c\u539f\u957f " + s.length() + " \u5b57\u7b26)";
    }
}
