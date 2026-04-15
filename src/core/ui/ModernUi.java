package core.ui;

import core.Db;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import javax.swing.UIManager;

/**
 * FlatLaf tweaks for a cleaner, more contemporary look (rounded controls, table density, accents).
 */
public final class ModernUi {

    /** 主窗口不透明度（1.0 完全不透明）；仅在系统支持 {@link java.awt.GraphicsDevice.WindowTranslucency#TRANSLUCENT} 且开启设置时生效 */
    public static final float WINDOW_OPACITY = 0.94f;

    public static final String SETTING_WINDOW_TRANSLUCENT = "ui-windowTranslucent";
    public static final String SETTING_AURORA_GRADIENT = "ui-auroraGradient";
    /** Absolute path to image file under {@code user.dir/wallpapers/} (imported copy) */
    public static final String SETTING_WALLPAPER_PATH = "ui-wallpaperPath";

    public static final Color ACCENT = new Color(0x3b82f6);
    public static final Color ACCENT_MUTED = new Color(0x60a5fa);
    public static final Color STATUS_BAR_BG = new Color(0x0f172a);
    public static final Color STATUS_BAR_FG = new Color(0xf8fafc);

    private ModernUi() {
    }

    public static boolean isWindowTranslucentEnabled() {
        return Boolean.parseBoolean(Db.tryGetSetingValue(SETTING_WINDOW_TRANSLUCENT, "true"));
    }

    public static boolean isAuroraGradientEnabled() {
        return Boolean.parseBoolean(Db.tryGetSetingValue(SETTING_AURORA_GRADIENT, "true"));
    }

    public static void applyWindowOpacityTo(Window w) {
        if (w == null) {
            return;
        }
        try {
            if (!isWindowTranslucentEnabled()) {
                w.setOpacity(1f);
                return;
            }
            if (WINDOW_OPACITY >= 0.999f) {
                w.setOpacity(1f);
                return;
            }
            GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = env.getDefaultScreenDevice();
            if (gd.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT)) {
                w.setOpacity(WINDOW_OPACITY);
            } else {
                w.setOpacity(1f);
            }
        } catch (Exception ignored) {
        }
    }

    public static void installBeforeTheme() {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.trackArc", 999);
        UIManager.put("Table.showHorizontalLines", false);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 2));
        UIManager.put("Table.rowHeight", 28);
        UIManager.put("Table.selectionBackground", new Color(0x3b82f6));
        UIManager.put("Table.selectionForeground", Color.WHITE);
        UIManager.put("Table.selectionInactiveBackground", new Color(0x94a3b8));
        UIManager.put("Table.selectionInactiveForeground", Color.WHITE);
        UIManager.put("TableHeader.height", 30);
        UIManager.put("TableHeader.separatorColor", new Color(0xe2e8f0));
        UIManager.put("MenuBar.borderColor", new Color(0xe2e8f0));
        UIManager.put("Separator.stripeWidth", 1);
        UIManager.put("TabbedPane.showTabSeparators", false);
        UIManager.put("Component.focusWidth", 1);
    }
}
