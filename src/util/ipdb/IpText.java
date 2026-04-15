package util.ipdb;

import java.net.InetAddress;
import java.net.UnknownHostException;

final class IpText {
    private IpText() {
    }

    static byte[] toNumericV4(String text) {
        try {
            InetAddress ia = InetAddress.getByName(text.trim());
            byte[] a = ia.getAddress();
            return a.length == 4 ? a : null;
        } catch (UnknownHostException e) {
            return null;
        }
    }

    static byte[] toNumericV6(String text) {
        try {
            InetAddress ia = InetAddress.getByName(text.trim());
            byte[] a = ia.getAddress();
            return a.length == 16 ? a : null;
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
