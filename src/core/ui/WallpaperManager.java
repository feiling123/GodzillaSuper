package core.ui;

import core.Db;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javax.imageio.ImageIO;
import javax.swing.UIManager;
import util.Log;

/**
 * Loads and stores custom wallpaper image for the main window.
 */
public final class WallpaperManager {

    public static final int MAX_LOAD_EDGE = 3840;
    private static final String WALLPAPER_DIR = "wallpapers";
    private static final String WALLPAPER_FILE = "background";

    private WallpaperManager() {
    }

    public static String getStoredPathOrEmpty() {
        String p = Db.tryGetSetingValue(ModernUi.SETTING_WALLPAPER_PATH, "");
        return p != null ? p.trim() : "";
    }

    public static BufferedImage loadForDisplay(String absolutePath) {
        if (absolutePath == null || absolutePath.isEmpty()) {
            return null;
        }
        File f = new File(absolutePath);
        if (!f.isFile()) {
            return null;
        }
        try {
            BufferedImage raw = ImageIO.read(f);
            if (raw == null) {
                return null;
            }
            int w = raw.getWidth();
            int h = raw.getHeight();
            int max = Math.max(w, h);
            if (max <= MAX_LOAD_EDGE) {
                return raw;
            }
            double scale = MAX_LOAD_EDGE / (double) max;
            int nw = Math.max(1, (int) Math.round(w * scale));
            int nh = Math.max(1, (int) Math.round(h * scale));
            BufferedImage scaled = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = scaled.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(raw, 0, 0, nw, nh, null);
            g2.dispose();
            return scaled;
        } catch (IOException e) {
            Log.error(e);
            return null;
        }
    }

    /**
     * Copies selected file into {@code user.dir/wallpapers/} and returns absolute path of the copy.
     */
    public static File importWallpaperFile(File userSelected) throws IOException {
        if (userSelected == null || !userSelected.isFile()) {
            throw new IOException("invalid file");
        }
        File dir = new File(System.getProperty("user.dir"), WALLPAPER_DIR);
        Files.createDirectories(dir.toPath());
        String name = userSelected.getName();
        int dot = name.lastIndexOf('.');
        String ext = dot >= 0 ? name.substring(dot) : ".img";
        if (ext.length() > 8) {
            ext = ".img";
        }
        File dest = new File(dir, WALLPAPER_FILE + ext.toLowerCase());
        Files.copy(userSelected.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return dest.getAbsoluteFile();
    }

    public static void clearWallpaperSetting() {
        String old = getStoredPathOrEmpty();
        if (!old.isEmpty()) {
            try {
                File f = new File(old);
                if (f.getName().startsWith(WALLPAPER_FILE) && f.getParentFile() != null
                        && WALLPAPER_DIR.equals(f.getParentFile().getName())) {
                    if (!f.delete()) {
                        f.deleteOnExit();
                    }
                }
            } catch (Exception ignored) {
            }
        }
        Db.removeSetingK(ModernUi.SETTING_WALLPAPER_PATH);
    }

    public static java.awt.Color panelBackgroundFallback() {
        java.awt.Color c = UIManager.getColor("Panel.background");
        return c != null ? c : new java.awt.Color(0xf0f0f0);
    }
}
