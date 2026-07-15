package shells.plugins.netcore;

import core.annotation.PluginAnnotation;
import java.io.IOException;
import shells.plugins.generic.PortScan;
import util.functions;

@PluginAnnotation(
        payloadName = "NetCoreDynamicPayload",
        Name = "PortScan",
        DisplayName = "\u7aef\u53e3\u626b\u63cf"
)
public class NPortScan extends PortScan {
    private static final String CLASS_NAME = "CProtScan.Run";

    public NPortScan() {
    }

    public byte[] readPlugin() throws IOException {
        return functions.readInputStreamAutoClose(NPortScan.class.getResourceAsStream("assets/CProtScan.dll"));
    }

    public String getClassName() {
        return CLASS_NAME;
    }
}
