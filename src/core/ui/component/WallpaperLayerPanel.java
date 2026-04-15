package core.ui.component;

import core.ui.WallpaperManager;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import javax.swing.UIManager;

/**
 * Full-frame wallpaper (cover) with optional frosted work area on top.
 */
public class WallpaperLayerPanel extends JPanel {

    private BufferedImage wallpaper;
    private JPanel contentRoot;

    public WallpaperLayerPanel() {
        super(new BorderLayout(0, 0));
        setOpaque(true);
    }

    public void setContentRoot(JPanel root) {
        if (this.contentRoot != null) {
            remove(this.contentRoot);
        }
        this.contentRoot = root;
        if (root != null) {
            add(root, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }

    public void reloadFromSettings() {
        wallpaper = WallpaperManager.loadForDisplay(WallpaperManager.getStoredPathOrEmpty());
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        if (wallpaper == null) {
            g2.setColor(WallpaperManager.panelBackgroundFallback());
            g2.fillRect(0, 0, w, h);
            g2.dispose();
            return;
        }
        int iw = wallpaper.getWidth();
        int ih = wallpaper.getHeight();
        if (iw <= 0 || ih <= 0) {
            g2.setColor(WallpaperManager.panelBackgroundFallback());
            g2.fillRect(0, 0, w, h);
            g2.dispose();
            return;
        }
        double sx = w / (double) iw;
        double sy = h / (double) ih;
        double scale = Math.max(sx, sy);
        int nw = Math.max(1, (int) Math.round(iw * scale));
        int nh = Math.max(1, (int) Math.round(ih * scale));
        int x = (w - nw) / 2;
        int y = (h - nh) / 2;
        g2.drawImage(wallpaper, x, y, nw, nh, null);
        g2.dispose();
    }

    /**
     * Semi-transparent panel background so theme wallpaper shows through the main work area.
     */
    public static class FrostedWorkArea extends JPanel {

        /** Lower = more visible wallpaper through the work area */
        private float alpha = 0.28f;

        public FrostedWorkArea(LayoutManager lm) {
            super(lm);
            setOpaque(false);
        }

        public void setFrostAlpha(float a) {
            this.alpha = Math.max(0.12f, Math.min(1f, a));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Color base = UIManager.getColor("Panel.background");
            if (base == null) {
                base = new Color(0xf5f5f5);
            }
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setColor(base);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
