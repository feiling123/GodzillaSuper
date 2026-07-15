package shells.plugins.netcore;

import core.annotation.PluginAnnotation;
import shells.plugins.generic.ZipCompress;
import util.functions;

@PluginAnnotation(
        payloadName = "NetCoreDynamicPayload",
        Name = "Zip",
        DisplayName = "ZIP\u538b\u7f29"
)
public class NZip extends ZipCompress {
    private static final String CLASS_NAME = "CZip.Run";

    public NZip() {
    }

    protected String getPluginName() {
        return CLASS_NAME;
    }

    protected byte[] getPlugin() {
        return functions.readInputStreamAutoClose(NZip.class.getResourceAsStream("assets/CZip.dll"));
    }
}
