package core;

import core.persist.OperationLogStore;
import core.ui.component.OperationLogPanel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import util.Log;

/**
 * \u672c\u6b21\u8fd0\u884c\u7684\u4f1a\u8bdd ID\u3001\u5206\u7ec4\uff0c\u4ee5\u53ca\u5b9e\u65f6\u65e5\u5fd7\u901a\u77e5\u3002
 */
public final class OperationLogRuntime {

    public static final String KV_ACTIVE_GROUP_ID = "operation_log_active_group_id";

    public interface ChunkListener {
        void onChunk(String sessionId, String textChunk);
    }

    private static volatile long activeGroupId = -1;
    private static volatile String currentSessionId;
    private static volatile long currentSessionStartedMs;
    private static final CopyOnWriteArrayList<ChunkListener> LISTENERS = new CopyOnWriteArrayList<>();

    private OperationLogRuntime() {
    }

    public static void addChunkListener(ChunkListener l) {
        if (l != null) {
            LISTENERS.add(l);
        }
    }

    public static void removeChunkListener(ChunkListener l) {
        LISTENERS.remove(l);
    }

    static void notifyChunk(String sessionId, String chunk) {
        if (sessionId == null || chunk == null || chunk.isEmpty()) {
            return;
        }
        for (ChunkListener l : LISTENERS) {
            try {
                l.onChunk(sessionId, chunk);
            } catch (Throwable t) {
                Log.error(t);
            }
        }
    }

    public static String getCurrentSessionId() {
        return currentSessionId;
    }

    public static long getActiveGroupId() {
        return activeGroupId;
    }

    public static long getCurrentSessionStartedMs() {
        return currentSessionStartedMs;
    }

    public static String formatSessionTreeLabel(String sessionId, long startedAtMs, boolean markCurrent) {
        String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(startedAtMs));
        String shortId = sessionId != null && sessionId.length() > 10
                ? sessionId.substring(0, 10) + "\u2026"
                : String.valueOf(sessionId);
        String base = ts + "  [" + shortId + "]";
        return markCurrent ? base + "  (\u5f53\u524d)" : base;
    }

    public static void bootstrap(OperationLogPanel panel) {
        if (!OperationLogStore.init()) {
            OperationAuditLog.bindSessionForPersistence(null);
            if (panel != null) {
                panel.onPersistenceUnavailable();
            }
            return;
        }
        try {
            String gidStr = OperationLogStore.getKv(KV_ACTIVE_GROUP_ID);
            long gid;
            if (gidStr == null || gidStr.isEmpty()) {
                gid = OperationLogStore.ensureGroup("\u9ed8\u8ba4");
                OperationLogStore.setKv(KV_ACTIVE_GROUP_ID, String.valueOf(gid));
            } else {
                gid = Long.parseLong(gidStr.trim());
                if (!OperationLogStore.groupExists(gid)) {
                    gid = OperationLogStore.ensureGroup("\u9ed8\u8ba4");
                    OperationLogStore.setKv(KV_ACTIVE_GROUP_ID, String.valueOf(gid));
                }
            }
            activeGroupId = gid;
            currentSessionStartedMs = System.currentTimeMillis();
            currentSessionId = OperationLogStore.createSession(gid, currentSessionStartedMs);
            OperationAuditLog.bindSessionForPersistence(currentSessionId);
            if (panel != null) {
                panel.onPersistenceReady(currentSessionId, gid);
            }
        } catch (Exception e) {
            Log.error(e);
            OperationAuditLog.bindSessionForPersistence(null);
            if (panel != null) {
                panel.onPersistenceUnavailable();
            }
        }
    }

    /**
     * \u4e0b\u6b21\u542f\u52a8\u65b0\u4f1a\u8bdd\u65f6\u5f52\u5165\u7684\u5206\u7ec4\uff08\u5f53\u524d\u4f1a\u8bdd\u4e0d\u53d8\uff09\u3002
     */
    public static void setPreferredGroupForNextLaunch(long groupId) {
        if (groupId > 0 && OperationLogStore.isAvailable()) {
            OperationLogStore.setKv(KV_ACTIVE_GROUP_ID, String.valueOf(groupId));
            activeGroupId = groupId;
        }
    }

    public static List<OperationLogStore.GroupRow> listGroups() {
        return OperationLogStore.listGroups();
    }

    public static long createGroup(String name) {
        return OperationLogStore.ensureGroup(name);
    }

    public static void appendPanelLine(String line) {
        if (line == null || line.isEmpty()) {
            return;
        }
        String oneLine = line.endsWith("\n") ? line.substring(0, line.length() - 1) : line;
        if (OperationLogStore.isAvailable() && currentSessionId != null) {
            OperationLogStore.appendEntry(currentSessionId, oneLine);
            notifyChunk(currentSessionId, line.endsWith("\n") ? line : line + "\n");
        } else {
            notifyChunk(currentSessionId, line.endsWith("\n") ? line : line + "\n");
        }
    }
}
