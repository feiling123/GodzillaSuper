package util.ipdb;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import util.Log;
import util.ipdb.IpdbReader.IpdbException;

/**
 * Resolves URL host to location string using qqwry.ipdb (IPIP format).
 * Data file: <a href="https://github.com/nmgliangwei/qqwry.ipdb">nmgliangwei/qqwry.ipdb</a> standard {@code qqwry.ipdb}.
 */
public final class IpLocationService {
    private static final String COL_HINT_NO_DB = "\u672a\u914d\u7f6e qqwry.ipdb";
    private static final String COL_HINT_RESOLVE = "\u57df\u540d\u89e3\u6790\u8d85\u65f6/\u5931\u8d25";
    private static final ExecutorService DNS_POOL = Executors.newFixedThreadPool(6, r -> {
        Thread t = new Thread(r, "ipdb-dns");
        t.setDaemon(true);
        return t;
    });
    private static volatile IpdbReader reader;
    private static final Map<String, String> HOST_CACHE = new ConcurrentHashMap<>();

    private IpLocationService() {
    }

    private static final String CLASSPATH_IPDB = "data/qqwry.ipdb";

    public static synchronized void init() {
        if (reader != null) {
            return;
        }
        Path path = locateDatabaseFile();
        if (path != null && Files.isRegularFile(path)) {
            try (InputStream in = new FileInputStream(path.toFile())) {
                reader = new IpdbReader(in);
                Log.log("qqwry.ipdb loaded: %s", path.toAbsolutePath().normalize());
                return;
            } catch (Exception e) {
                reader = null;
                Log.error(e);
            }
        }
        InputStream bundled = openClasspathIpdb();
        if (bundled != null) {
            try {
                reader = new IpdbReader(bundled);
                Log.log("qqwry.ipdb loaded: classpath %s", CLASSPATH_IPDB);
            } catch (Exception e) {
                reader = null;
                Log.error(e);
            }
        }
    }

    public static boolean isLoaded() {
        return reader != null;
    }

    public static String getDatabasePathHint() {
        Path p = locateDatabaseFile();
        if (p != null && Files.isRegularFile(p)) {
            return p.toAbsolutePath().toString();
        }
        if (classpathIpdbAvailable()) {
            return "classpath:" + CLASSPATH_IPDB;
        }
        return "";
    }

    private static boolean classpathIpdbAvailable() {
        java.net.URL u = IpLocationService.class.getResource("/" + CLASSPATH_IPDB);
        return u != null;
    }

    private static InputStream openClasspathIpdb() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            InputStream s = cl.getResourceAsStream(CLASSPATH_IPDB);
            if (s != null) {
                return s;
            }
        }
        return IpLocationService.class.getResourceAsStream("/" + CLASSPATH_IPDB);
    }

    private static Path locateDatabaseFile() {
        String prop = System.getProperty("qqwry.ipdb.path");
        if (prop != null && !prop.isEmpty()) {
            Path p = Paths.get(prop);
            if (Files.isRegularFile(p)) {
                return p;
            }
        }
        String userDir = System.getProperty("user.dir", ".");
        Path projectData = Paths.get(userDir, "data", "qqwry.ipdb");
        if (Files.isRegularFile(projectData)) {
            return projectData;
        }
        Path cwdRoot = Paths.get(userDir, "qqwry.ipdb");
        if (Files.isRegularFile(cwdRoot)) {
            return cwdRoot;
        }
        Path dataRelative = Paths.get("data", "qqwry.ipdb");
        if (Files.isRegularFile(dataRelative)) {
            return dataRelative;
        }
        Path cwdBare = Paths.get("qqwry.ipdb");
        if (Files.isRegularFile(cwdBare)) {
            return cwdBare;
        }
        Path home = Paths.get(System.getProperty("user.home"), ".webshell-manager", "qqwry.ipdb");
        if (Files.isRegularFile(home)) {
            return home;
        }
        return null;
    }

    public static String lookupUrl(String url) {
        if (reader == null) {
            init();
        }
        if (reader == null) {
            return COL_HINT_NO_DB;
        }
        if (HOST_CACHE.size() > 5000) {
            HOST_CACHE.clear();
        }
        String host = hostFromUrl(url);
        if (host.isEmpty()) {
            return "";
        }
        String cached = HOST_CACHE.get(host);
        if (cached != null) {
            return cached;
        }
        String ip = resolveHostToIp(host);
        if (ip == null) {
            String v = COL_HINT_RESOLVE;
            HOST_CACHE.put(host, v);
            return v;
        }
        try {
            String[] fields = reader.find(ip, "CN");
            if (fields == null) {
                fields = reader.find(ip, "EN");
            }
            String v = formatFields(fields);
            HOST_CACHE.put(host, v);
            return v;
        } catch (IpdbException e) {
            String v = e.getMessage() != null ? e.getMessage() : "ipdb";
            HOST_CACHE.put(host, v);
            return v;
        }
    }

    public static void clearCache() {
        HOST_CACHE.clear();
    }

    private static String hostFromUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "";
        }
        String u = url.trim();
        if (u.startsWith("jdbc:")) {
            int idx = u.indexOf("://");
            if (idx > 0) {
                u = "http" + u.substring(idx);
            }
        }
        if (!u.contains("://")) {
            u = "http://" + u;
        }
        try {
            URI uri = new URI(u);
            String h = uri.getHost();
            return h != null ? h : "";
        } catch (URISyntaxException e) {
            return "";
        }
    }

    private static String resolveHostToIp(String host) {
        if (isLiteralIp(host)) {
            return host;
        }
        Future<String> f = DNS_POOL.submit(() -> {
            try {
                return InetAddress.getByName(host).getHostAddress();
            } catch (UnknownHostException e) {
                return null;
            }
        });
        try {
            return f.get(2, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            f.cancel(true);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException e) {
            return null;
        }
    }

    private static boolean isLiteralIp(String host) {
        if (host.indexOf(':') >= 0) {
            return true;
        }
        return host.matches("^(\\d{1,3}\\.){3}\\d{1,3}$");
    }

    private static String formatFields(String[] fields) {
        if (fields == null || fields.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String f : fields) {
            if (f == null || f.isEmpty() || "0".equals(f)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(f.trim());
        }
        return sb.length() == 0 ? "" : sb.toString();
    }
}
