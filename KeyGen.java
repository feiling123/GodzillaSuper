import java.awt.GraphicsEnvironment;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JOptionPane;

import java.io.File;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Locale;
import java.util.Base64;
import javax.crypto.Mac;

public class KeyGen {
    private static final String LICENSE_FILE_NAME = "license.lic";
    private static final String LICENSE_HEADER = "GSL1:";

    private static final String LICENSE_VERSION = "GSL1";
    private static final String AES_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final byte[] AES_KEY = "GSL_LICENSE_KEY_".getBytes();
    private static final byte[] HMAC_KEY = "GSL_LICENSE_HMAC_KEY".getBytes();

    public static void main(String[] args) {
        boolean commandLineMode = args.length > 0 || GraphicsEnvironment.isHeadless() || isCi();
        try {
            if (commandLineMode) {
                runCommandLine(args);
            } else {
                runGui();
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (!commandLineMode) {
                JOptionPane.showMessageDialog(null, "\u751f\u6210\u5931\u8d25: " + e.getMessage());
            }
            System.exit(1);
        }
    }

    private static void runGui() throws Exception {
        long days = 365L;

        String input = JOptionPane.showInputDialog(
                null,
                "\u8bf7\u8f93\u5165\u6388\u6743\u6709\u6548\u671f\u5929\u6570 (\u9ed8\u8ba4\u4e3a365\u5929):",
                "\u751f\u6210\u6388\u6743 (Generate License)",
                JOptionPane.QUESTION_MESSAGE
        );

        if (input != null && !input.trim().isEmpty()) {
            try {
                days = Long.parseLong(input.trim());
                if (days <= 0) {
                    JOptionPane.showMessageDialog(null, "\u6709\u6548\u671f\u5929\u6570\u5fc5\u987b\u5927\u4e8e0\u5929\u3002");
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "\u65e0\u6548\u7684\u6570\u5b57\uff0c\u4f7f\u7528\u9ed8\u8ba4365\u5929\u3002");
            }
        } else if (input == null) {
            return;
        }

        String bindDirInput = JOptionPane.showInputDialog(
                null,
                "\u8bf7\u8f93\u5165\u6388\u6743\u7ed1\u5b9a\u76ee\u5f55\u8def\u5f84 (\u7559\u7a7a=\u4efb\u610f\u76ee\u5f55):",
                "\u751f\u6210\u6388\u6743 (Generate License)",
                JOptionPane.QUESTION_MESSAGE
        );

        if (bindDirInput == null) {
            return;
        }

        File licenseFile = new File(LICENSE_FILE_NAME);
        if (licenseFile.exists()) {
            int result = JOptionPane.showConfirmDialog(
                    null,
                    "license.lic \u5df2\u5b58\u5728\uff0c\u662f\u5426\u8986\u76d6\uff1f",
                    "\u786e\u8ba4 (Confirm)",
                    JOptionPane.YES_NO_OPTION
            );
            if (result != JOptionPane.YES_OPTION) {
                return;
            }
        }

        LicenseResult result = generateLicense(licenseFile, days, bindDirInput, true);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String expireDateStr = sdf.format(new Date(result.notAfter));

        JOptionPane.showMessageDialog(
                null,
                "\u6210\u529f\u751f\u6210\u6388\u6743\u6587\u4ef6\uff01\n\u4f4d\u7f6e: " + result.licenseFile.getAbsolutePath() + "\n\u8fc7\u671f\u65f6\u95f4: " + expireDateStr + "\n\u7ed1\u5b9a\u76ee\u5f55: " + result.canonicalDir
        );
    }

    private static void runCommandLine(String[] args) throws Exception {
        CommandLineOptions options = parseCommandLineOptions(args);
        LicenseResult result = generateLicense(options.outputFile, options.days, options.bindDir, options.force);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        System.out.println("License generated: " + result.licenseFile.getAbsolutePath());
        System.out.println("Expires at: " + sdf.format(new Date(result.notAfter)));
        System.out.println("Bind directory: " + result.canonicalDir);
    }

    private static LicenseResult generateLicense(File licenseFile, long days, String bindDirInput, boolean force) throws Exception {
        if (days <= 0) {
            throw new IllegalArgumentException("days must be greater than 0");
        }

        if (licenseFile.exists() && !force) {
            throw new IllegalStateException(licenseFile.getPath() + " already exists. Use --force or KEYGEN_FORCE=true to overwrite.");
        }

        File parent = licenseFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Failed to create output directory: " + parent.getPath());
        }

        long notBefore = System.currentTimeMillis();
        long notAfter = notBefore + days * 24L * 60L * 60L * 1000L;

        String bindInputTrim = bindDirInput == null ? "" : bindDirInput.trim();
        String canonicalDir;
        String canonicalDirHash;
        if (bindInputTrim.isEmpty()) {
            canonicalDir = "*";
            canonicalDirHash = "ANY";
        } else {
            File bindDir = new File(bindInputTrim);
            canonicalDir = bindDir.getCanonicalPath();
            canonicalDirHash = sha256Hex(canonicalDir.getBytes("UTF-8"));
        }

        String signingData = LICENSE_VERSION + "|" + notBefore + "|" + notAfter + "|" + canonicalDirHash;
        String sigHex = hmacSha256Hex(signingData.getBytes("UTF-8"), HMAC_KEY);
        String payload = signingData + "|" + sigHex;

        byte[] encryptedData = encrypt(payload.getBytes("UTF-8"));
        String outText = LICENSE_HEADER + base64Encode(encryptedData);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(licenseFile);
            fos.write(outText.getBytes("UTF-8"));
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                }
            }
        }

        return new LicenseResult(licenseFile.getAbsoluteFile(), notAfter, canonicalDir);
    }

    private static CommandLineOptions parseCommandLineOptions(String[] args) {
        CommandLineOptions options = new CommandLineOptions();
        options.days = parseLongOrDefault(System.getenv("KEYGEN_DAYS"), 365L);
        options.bindDir = stringOrDefault(System.getenv("KEYGEN_BIND_DIR"), "");
        options.outputFile = new File(nonBlankOrDefault(System.getenv("KEYGEN_OUTPUT"), LICENSE_FILE_NAME));
        options.force = parseBooleanOrDefault(System.getenv("KEYGEN_FORCE"), false);

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--days".equals(arg)) {
                options.days = Long.parseLong(requireValue(args, ++i, arg));
            } else if ("--bind-dir".equals(arg)) {
                options.bindDir = requireValue(args, ++i, arg);
            } else if ("--output".equals(arg)) {
                options.outputFile = new File(requireValue(args, ++i, arg));
            } else if ("--force".equals(arg)) {
                options.force = true;
            } else if ("--no-force".equals(arg)) {
                options.force = false;
            } else if ("--help".equals(arg) || "-h".equals(arg)) {
                printUsageAndExit();
            } else {
                throw new IllegalArgumentException("Unknown argument: " + arg);
            }
        }

        return options;
    }

    private static String requireValue(String[] args, int index, String optionName) {
        if (index >= args.length) {
            throw new IllegalArgumentException("Missing value for " + optionName);
        }
        return args[index];
    }

    private static long parseLongOrDefault(String value, long defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return Long.parseLong(value.trim());
    }

    private static boolean parseBooleanOrDefault(String value, boolean defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value.trim()) || "1".equals(value.trim()) || "yes".equalsIgnoreCase(value.trim());
    }

    private static String stringOrDefault(String value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    private static String nonBlankOrDefault(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value;
    }

    private static boolean isCi() {
        return "true".equalsIgnoreCase(System.getenv("CI"));
    }

    private static void printUsageAndExit() {
        System.out.println("Usage: java KeyGen [--days 365] [--bind-dir /path] [--output license.lic] [--force]");
        System.out.println("Environment: KEYGEN_DAYS, KEYGEN_BIND_DIR, KEYGEN_OUTPUT, KEYGEN_FORCE");
        System.exit(0);
    }

    private static byte[] encrypt(byte[] input) throws Exception {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);

        SecretKeySpec keySpec = new SecretKeySpec(AES_KEY, "AES");
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(iv));
        byte[] cipherBytes = cipher.doFinal(input);

        byte[] out = new byte[16 + cipherBytes.length];
        System.arraycopy(iv, 0, out, 0, 16);
        System.arraycopy(cipherBytes, 0, out, 16, cipherBytes.length);
        return out;
    }

    private static String base64Encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static String sha256Hex(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return toHexLower(md.digest(data));
    }

    private static String hmacSha256Hex(byte[] data, byte[] key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return toHexLower(mac.doFinal(data));
    }

    private static String toHexLower(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format(Locale.ROOT, "%02x", bytes[i] & 0xFF));
        }
        return sb.toString();
    }

    private static class CommandLineOptions {
        long days;
        String bindDir;
        File outputFile;
        boolean force;
    }

    private static class LicenseResult {
        final File licenseFile;
        final long notAfter;
        final String canonicalDir;

        LicenseResult(File licenseFile, long notAfter, String canonicalDir) {
            this.licenseFile = licenseFile;
            this.notAfter = notAfter;
            this.canonicalDir = canonicalDir;
        }
    }
}
