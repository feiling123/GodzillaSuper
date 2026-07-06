//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package shells.cryptions.JavaAes;

import core.annotation.CryptionAnnotation;
import core.c2profile.cryption.C2Channel;

@CryptionAnnotation(
    Name = "JAVA_C2",
    payloadName = "JavaDynamicPayload"
)
public class JavaC2 extends C2Channel {
    public JavaC2() {
    }

    protected String getPayloadName() {
        return "JavaDynamicPayload";
    }
}
