//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package shells.plugins.java;

import core.annotation.PluginAnnotation;
import shells.plugins.generic.ShellcodeLoader;

@PluginAnnotation(
        payloadName = "JavaDynamicPayload",
        Name = "Useradd",
        DisplayName = "Useradd"
)
public class Useradd extends shells.plugins.generic.Useradd {
    public Useradd() {

    }

    protected ShellcodeLoader getShellcodeLoader() {
        return (ShellcodeLoader) this.shellEntity.getFrame().getPlugin("ShellcodeLoader");
    }
}
