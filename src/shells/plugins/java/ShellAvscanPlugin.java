package shells.plugins.java;

import core.EasyI18N;
import core.annotation.PluginAnnotation;
import core.imp.Plugin;
import core.shell.ShellEntity;
import core.ui.ShellAvscan;
import javax.swing.JPanel;

@PluginAnnotation(
        payloadName = "JavaDynamicPayload",
        Name = "ShellAvscan",
        DisplayName = "\u6740\u8f6f\u8bc6\u522b"
)
public class ShellAvscanPlugin implements Plugin {
    private JPanel view;

    public ShellAvscanPlugin() {
    }

    @Override
    public void init(ShellEntity shellEntity) {
        ShellAvscan panel = new ShellAvscan(shellEntity);
        EasyI18N.installObject(panel);
        this.view = panel;
    }

    @Override
    public JPanel getView() {
        return this.view;
    }
}
