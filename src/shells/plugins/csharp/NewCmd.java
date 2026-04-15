package shells.plugins.csharp;

import core.annotation.PluginAnnotation;
import shells.plugins.generic.ShellcodeLoader;

@PluginAnnotation(payloadName = "CSharpDynamicPayload", Name = "综合插件", DisplayName = "综合插件")


public class NewCmd extends shells.plugins.generic.NewCmd {
    protected ShellcodeLoader getShellcodeLoader() {
        return (ShellcodeLoader) this.shellEntity.getFrame().getPlugin("ShellcodeLoader");
    }
}
