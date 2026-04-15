package core;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JOptionPane;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Base64;
import javax.crypto.Mac;

public class ApplicationConfig {
    private static final int k0 = 90;

    public static void license() {
        File licenseFile = a0();
        if (licenseFile == null) {
            i0("\u5f53\u524d\u76ee\u5f55\u4e0b\u672a\u627e\u5230\u6388\u6743\u6587\u4ef6 license.lic\uff01\n\u8bf7\u83b7\u53d6\u6388\u6743\u7801\u5e76\u751f\u6210\u6388\u6743\u6587\u4ef6\u3002\nLicense file not found!");
        }
        
        try {
            byte[] fileBytes = h0(licenseFile);
            String fileText = new String(fileBytes, "UTF-8").trim();
            String header = b0();
            if (fileText.startsWith(header)) {
                fileText = fileText.substring(header.length()).trim();
            }

            byte[] encryptedData = c0(fileText);
            byte[] decryptedData = g0(encryptedData);
            String content = new String(decryptedData, "UTF-8");

            String[] parts = content.split("\\|", -1);
            if (parts.length != 5) {
                i0("\u6388\u6743\u6587\u4ef6\u5185\u5bb9\u683c\u5f0f\u9519\u8bef\uff01\nInvalid license content format.");
            }

            if (!d0().equals(parts[0])) {
                i0("\u6388\u6743\u6587\u4ef6\u7248\u672c\u4e0d\u5339\u914d\uff01\nInvalid license version.");
            }

            long notBefore = Long.parseLong(parts[1]);
            long notAfter = Long.parseLong(parts[2]);
            String dirHash = parts[3];
            String sigHex = parts[4];

            String canonicalDir = new File(".").getCanonicalPath();
            String canonicalDirHash = e0(canonicalDir.getBytes("UTF-8"));
            if (!"ANY".equalsIgnoreCase(dirHash) && !canonicalDirHash.equalsIgnoreCase(dirHash)) {
                i0("\u6388\u6743\u6587\u4ef6\u4e0d\u5c5e\u4e8e\u5f53\u524d\u76ee\u5f55\uff01\nLicense is not for this directory.");
            }

            String signingData = parts[0] + "|" + parts[1] + "|" + parts[2] + "|" + parts[3];
            String expectedSigHex = f0(signingData.getBytes("UTF-8"), j0());
            if (!expectedSigHex.equalsIgnoreCase(sigHex)) {
                i0("\u6388\u6743\u6587\u4ef6\u7b7e\u540d\u6821\u9a8c\u5931\u8d25\uff01\nInvalid license signature.");
            }

            long now = System.currentTimeMillis();
            if (now < notBefore) {
                i0("\u6388\u6743\u5c1a\u672a\u751f\u6548\uff01\n\u751f\u6548\u65f6\u95f4: " + j1(notBefore) + "\nLicense not active yet.");
            }
            if (now > notAfter) {
                i0("\u6388\u6743\u5df2\u8fc7\u671f\uff01\n\u8fc7\u671f\u65f6\u95f4: " + j1(notAfter) + "\nLicense expired!");
            }
        } catch (Exception e) {
            i0("\u6388\u6743\u6821\u9a8c\u5931\u8d25\uff1a" + e.getMessage() + "\n\u53ef\u80fd\u662f\u6388\u6743\u6587\u4ef6\u635f\u574f\u6216\u975e\u6cd5\u3002\nVerification failed.");
        }
    }

    private static File a0() {
        File f1 = new File(k1());
        if (f1.exists() && f1.isFile()) {
            return f1;
        }
        File f2 = new File(k2());
        if (f2.exists() && f2.isFile()) {
            return f2;
        }
        File f3 = new File(k3());
        if (f3.exists() && f3.isFile()) {
            return f3;
        }
        return null;
    }

    private static String j1(long epochMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(epochMillis));
    }

    private static byte[] c0(String s) {
        return Base64.getDecoder().decode(s);
    }

    private static String e0(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return d1(md.digest(data));
    }

    private static String f0(byte[] data, byte[] key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return d1(mac.doFinal(data));
    }

    private static String d1(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format(Locale.ROOT, "%02x", bytes[i] & 0xFF));
        }
        return sb.toString();
    }

    private static byte[] g0(byte[] input) throws Exception {
        if (input == null || input.length < 17) {
            throw new IllegalArgumentException("license content too short");
        }
        byte[] iv = new byte[16];
        System.arraycopy(input, 0, iv, 0, 16);
        byte[] cipherBytes = new byte[input.length - 16];
        System.arraycopy(input, 16, cipherBytes, 0, cipherBytes.length);

        SecretKeySpec keySpec = new SecretKeySpec(i1(), "AES");
        Cipher cipher = Cipher.getInstance(l0());
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
        return cipher.doFinal(cipherBytes);
    }

    private static byte[] h0(File file) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int read;
            while ((read = fis.read(buf)) > -1) {
                if (read > 0) {
                    bos.write(buf, 0, read);
                }
            }
            return bos.toByteArray();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private static void i0(String message) {
        JOptionPane.showMessageDialog(null, message, "\u6388\u6743\u9519\u8bef (License Error)", JOptionPane.ERROR_MESSAGE);
        System.exit(0);
    }

    private static String d0() {
        return m0(new int[]{76, 88, 71, 58}, 11);
    }

    private static String b0() {
        return m0(new int[]{76, 88, 71, 58, 49}, 11);
    }

    private static String k1() {
        return m0(new int[]{127, 122, 112, 118, 125, 96, 118, 61, 127, 122, 112}, 19);
    }

    private static String k2() {
        return m0(new int[]{127, 122, 112, 118, 125, 96, 118}, 19);
    }

    private static String k3() {
        return m0(new int[]{116, 96, 127, 61, 127, 122, 112}, 19);
    }

    private static String l0() {
        return "AES/CBC/PKCS5Padding";
    }

    private static byte[] i1() {
        return n0(new int[]{29, 9, 22, 5, 22, 19, 25, 31, 20, 9, 31, 5, 17, 31, 3, 5});
    }

    private static byte[] j0() {
        return n0(new int[]{29, 9, 22, 5, 22, 19, 25, 31, 20, 9, 31, 5, 18, 23, 27, 25, 5, 17, 31, 3});
    }

    private static byte[] n0(int[] a) {
        byte[] b = new byte[a.length];
        int v = k0;
        for (int i = 0; i < a.length; i++) {
            b[i] = (byte)(a[i] ^ v);
        }
        return b;
    }

    private static String m0(int[] a, int k) {
        char[] c = new char[a.length];
        for (int i = 0; i < a.length; i++) {
            c[i] = (char)(a[i] ^ k);
        }
        return new String(c);
    }
}
