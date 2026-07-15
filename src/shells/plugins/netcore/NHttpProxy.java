package shells.plugins.netcore;

import core.annotation.PluginAnnotation;
import core.imp.Plugin;
import java.io.IOException;
import shells.plugins.generic.HttpProxy;
import util.functions;

@PluginAnnotation(
        payloadName = "NetCoreDynamicPayload",
        Name = "HttpProxy",
        DisplayName = "Http\u4ee3\u7406"
)
public class NHttpProxy extends HttpProxy implements Plugin {
    private static final String CLASS_NAME = "HttpRequest.Run";

    public NHttpProxy() {
    }

    public byte[] readPlugin() throws IOException {
        return functions.readInputStreamAutoClose(NHttpProxy.class.getResourceAsStream("assets/HttpRequest.dll"));
    }

    public String getClassName() {
        return CLASS_NAME;
    }
}
