package core.ui.component;

import core.OperationLogRuntime;
import core.persist.OperationLogStore;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * \u5206\u7ec4 / \u4f1a\u8bdd ID / \u65f6\u95f4 \u6811\u72b6\u6d4f\u89c8 + \u8be6\u60c5\u6587\u672c\u3002
 */
public class OperationLogHistoryView extends JPanel {

    private final DefaultMutableTreeNode root = new DefaultMutableTreeNode("\u64cd\u4f5c\u65e5\u5fd7");
    private final DefaultTreeModel treeModel = new DefaultTreeModel(root);
    private final JTree tree = new JTree(treeModel);
    private final JTextArea detailArea = new JTextArea();
    private final JScrollPane treeScroll;
    private final JScrollPane detailScroll;
    private final JSplitPane split;

    private String currentSessionIdMarker;
    private volatile String selectedSessionId;

    public OperationLogHistoryView() {
        super(new BorderLayout(0, 0));
        setOpaque(false);
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                    boolean leaf, int row, boolean hasFocus) {
                Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                Object uo = value instanceof javax.swing.tree.DefaultMutableTreeNode
                        ? ((DefaultMutableTreeNode) value).getUserObject()
                        : null;
                String text;
                if (uo instanceof OperationLogStore.GroupRow) {
                    text = ((OperationLogStore.GroupRow) uo).name;
                } else if (uo instanceof OperationLogStore.SessionRow) {
                    OperationLogStore.SessionRow sr = (OperationLogStore.SessionRow) uo;
                    text = OperationLogRuntime.formatSessionTreeLabel(sr.id, sr.startedAtMs,
                            currentSessionIdMarker != null && currentSessionIdMarker.equals(sr.id));
                } else {
                    text = String.valueOf(uo);
                }
                setText(text);
                return c;
            }
        });
        tree.addTreeSelectionListener(this::onTreeSelection);
        treeScroll = new JScrollPane(tree);
        treeScroll.setBorder(BorderFactory.createEmptyBorder());

        detailArea.setEditable(false);
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);
        detailArea.setBackground(new Color(0x0c0c0e));
        detailArea.setForeground(new Color(0xe4e4e7));
        detailArea.setCaretColor(new Color(0x64748b));
        detailArea.setSelectedTextColor(new Color(0xe4e4e7));
        detailArea.setSelectionColor(new Color(0x8b5cf6));
        detailArea.setBorder(BorderFactory.createEmptyBorder(6, 10, 8, 10));
        detailScroll = new JScrollPane(detailArea);
        detailScroll.setBorder(BorderFactory.createEmptyBorder());
        detailScroll.getViewport().setBackground(new Color(0x0c0c0e));

        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, detailScroll);
        split.setResizeWeight(0.28);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setOpaque(false);
        add(split, BorderLayout.CENTER);
    }

    private void onTreeSelection(TreeSelectionEvent e) {
        TreePath path = tree.getSelectionPath();
        if (path == null) {
            selectedSessionId = null;
            return;
        }
        Object last = path.getLastPathComponent();
        if (!(last instanceof DefaultMutableTreeNode)) {
            return;
        }
        Object uo = ((DefaultMutableTreeNode) last).getUserObject();
        if (uo instanceof OperationLogStore.SessionRow) {
            String sid = ((OperationLogStore.SessionRow) uo).id;
            selectedSessionId = sid;
            detailArea.setText(OperationLogStore.loadSessionText(sid));
            detailArea.setCaretPosition(0);
        } else {
            selectedSessionId = null;
            detailArea.setText("");
        }
    }

    public void setCurrentSessionMarker(String sessionId) {
        this.currentSessionIdMarker = sessionId;
    }

    public String getSelectedSessionId() {
        return selectedSessionId;
    }

    public void reloadTree() {
        root.removeAllChildren();
        for (OperationLogStore.GroupRow g : OperationLogStore.listGroups()) {
            DefaultMutableTreeNode gn = new DefaultMutableTreeNode(g);
            for (OperationLogStore.SessionRow s : OperationLogStore.listSessions(g.id)) {
                gn.add(new DefaultMutableTreeNode(s));
            }
            root.add(gn);
        }
        treeModel.reload();
        int row = 0;
        while (row < tree.getRowCount()) {
            tree.expandRow(row);
            row++;
        }
        tree.repaint();
    }

    public void selectSessionInTree(String sessionId) {
        if (sessionId == null) {
            return;
        }
        for (int gi = 0; gi < root.getChildCount(); gi++) {
            DefaultMutableTreeNode gn = (DefaultMutableTreeNode) root.getChildAt(gi);
            for (int si = 0; si < gn.getChildCount(); si++) {
                DefaultMutableTreeNode sn = (DefaultMutableTreeNode) gn.getChildAt(si);
                Object uo = sn.getUserObject();
                if (uo instanceof OperationLogStore.SessionRow
                        && sessionId.equals(((OperationLogStore.SessionRow) uo).id)) {
                    TreePath tp = new TreePath(sn.getPath());
                    tree.setSelectionPath(tp);
                    tree.scrollPathToVisible(tp);
                    return;
                }
            }
        }
    }

    public void appendToDetailIfViewing(String sessionId, String chunk) {
        if (sessionId == null || chunk == null || chunk.isEmpty()) {
            return;
        }
        if (sessionId.equals(selectedSessionId)) {
            detailArea.append(chunk);
            detailArea.setCaretPosition(detailArea.getDocument().getLength());
        }
    }

    public void clearDetailDisplay() {
        detailArea.setText("");
    }

    /**
     * \u65e0\u672c\u5730\u5e93\u65f6\u5c55\u793a\u5185\u5b58\u4e2d\u7684\u5ba1\u8ba1\u6587\u672c\u3002
     */
    public void setFallbackDetailText(String text) {
        tree.clearSelection();
        selectedSessionId = null;
        detailArea.setText(text == null ? "" : text);
        detailArea.setCaretPosition(0);
    }

    public void applyDetailFont(Font font) {
        detailArea.setFont(font);
    }

    public void applyTreeFont(Font font) {
        tree.setFont(font);
    }

    public void setSplitDividerFromScale(float scale) {
        int w = Math.round(220 * scale);
        split.setDividerLocation(w);
    }
}
