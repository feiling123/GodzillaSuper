package shells.plugins.java;

import core.annotation.PluginAnnotation;
import shells.plugins.generic.ShellcodeLoader;

@PluginAnnotation(Name = "综合插件", payloadName = "JavaDynamicPayload", DisplayName = "综合插件")

public class NewCmd extends shells.plugins.generic.NewCmd {
    public NewCmd() {

    }

    @Override
    protected ShellcodeLoader getShellcodeLoader() {
        return (ShellcodeLoader) this.shellEntity.getFrame().getPlugin("ShellcodeLoader");
    }
}
