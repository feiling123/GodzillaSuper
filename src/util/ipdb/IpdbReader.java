package util.ipdb;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * IPIP.net IPDB format reader (compatible with qqwry.ipdb).
 * Logic adapted from <a href="https://github.com/ipipdotnet/ipdb-java">ipipdotnet/ipdb-java</a>.
 */
public final class IpdbReader {

    private int fileSize;
    private int nodeCount;
    private Meta meta;
    private byte[] data;
    private int v4offset;

    public IpdbReader(InputStream in) throws IOException, IpdbException {
        init(readAll(in));
    }

    public IpdbReader(byte[] fileBytes) throws IpdbException {
        init(fileBytes);
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        in.close();
        return out.toByteArray();
    }

    private void init(byte[] raw) throws IpdbException {
        this.fileSize = raw.length;
        if (this.fileSize < 5) {
            throw new IpdbException("database file size error");
        }
        long metaLength = bytesToLong(raw[0], raw[1], raw[2], raw[3]);
        int metaEnd = (int) metaLength + 4;
        byte[] metaBytes = Arrays.copyOfRange(raw, 4, metaEnd);
        try {
            JSONObject jo = JSONUtil.parseObj(new String(metaBytes, StandardCharsets.UTF_8));
            this.meta = parseMeta(jo);
            this.nodeCount = this.meta.nodeCount;
        } catch (Exception e) {
            throw new IpdbException("meta: " + e.getMessage());
        }
        int expected = this.meta.totalSize + metaEnd;
        if (expected != this.fileSize) {
            throw new IpdbException("database file size mismatch: expected " + expected + " got " + this.fileSize);
        }
        this.data = Arrays.copyOfRange(raw, metaEnd, this.fileSize);
        if ((this.meta.ipVersion & 0x01) == 0x01) {
            int node = 0;
            for (int i = 0; i < 96 && node < this.nodeCount; i++) {
                if (i >= 80) {
                    node = readNode(node, 1);
                } else {
                    node = readNode(node, 0);
                }
            }
            this.v4offset = node;
        }
    }

    private static Meta parseMeta(JSONObject jo) {
        Meta m = new Meta();
        m.build = jo.getInt("build", 0);
        m.ipVersion = jo.getInt("ip_version", jo.getInt("IPVersion", 0));
        m.nodeCount = jo.getInt("node_count", jo.getInt("nodeCount", 0));
        m.totalSize = jo.getInt("total_size", jo.getInt("totalSize", 0));
        JSONArray fields = jo.getJSONArray("fields");
        if (fields != null) {
            m.fields = new String[fields.size()];
            for (int i = 0; i < fields.size(); i++) {
                m.fields[i] = fields.getStr(i);
            }
        } else {
            m.fields = new String[0];
        }
        m.languages = new HashMap<>();
        JSONObject langs = jo.getJSONObject("languages");
        if (langs != null) {
            for (String key : langs.keySet()) {
                m.languages.put(key, langs.getInt(key));
            }
        }
        return m;
    }

    public String[] find(String addr, String language) throws IpdbException {
        Integer off = meta.languages.get(language);
        if (off == null) {
            if (!meta.languages.isEmpty()) {
                Map.Entry<String, Integer> first = meta.languages.entrySet().iterator().next();
                off = first.getValue();
                language = first.getKey();
            } else {
                return null;
            }
        }
        byte[] ipv;
        if (addr.indexOf(':') >= 0) {
            ipv = IpText.toNumericV6(addr);
            if (ipv == null) {
                throw new IpdbException("ipv6 format error");
            }
            if ((this.meta.ipVersion & 0x02) != 0x02) {
                throw new IpdbException("no support ipv6");
            }
        } else if (addr.indexOf('.') > 0) {
            ipv = IpText.toNumericV4(addr);
            if (ipv == null) {
                throw new IpdbException("ipv4 format error");
            }
            if ((this.meta.ipVersion & 0x01) != 0x01) {
                throw new IpdbException("no support ipv4");
            }
        } else {
            throw new IpdbException("ip format error");
        }
        int node;
        try {
            node = findNode(ipv);
        } catch (NotFoundException nfe) {
            return null;
        }
        String row = resolve(node);
        int fieldCount = meta.fields.length;
        int langSpan = fieldCount * meta.languages.size();
        String[] parts = row.split("\t", langSpan);
        if (off + fieldCount > parts.length) {
            return Arrays.copyOfRange(parts, Math.min(off, parts.length), parts.length);
        }
        return Arrays.copyOfRange(parts, off, off + fieldCount);
    }

    private int findNode(byte[] binary) throws NotFoundException {
        int node = 0;
        int bit = binary.length * 8;
        if (bit == 32) {
            node = this.v4offset;
        }
        for (int i = 0; i < bit; i++) {
            if (node > this.nodeCount) {
                break;
            }
            int bitVal = 1 & ((0xFF & binary[i / 8]) >> (7 - (i % 8)));
            node = readNode(node, bitVal);
        }
        if (node > this.nodeCount) {
            return node;
        }
        throw new NotFoundException();
    }

    private String resolve(int node) throws IpdbException {
        int resolved = node - this.nodeCount + this.nodeCount * 8;
        if (resolved >= this.fileSize) {
            throw new IpdbException("database resolve error");
        }
        byte b = 0;
        int size = (int) bytesToLong(b, b, this.data[resolved], this.data[resolved + 1]);
        if (this.data.length < resolved + 2 + size) {
            throw new IpdbException("database resolve error");
        }
        return new String(this.data, resolved + 2, size, StandardCharsets.UTF_8);
    }

    private int readNode(int node, int index) {
        int off = node * 8 + index * 4;
        return (int) bytesToLong(this.data[off], this.data[off + 1], this.data[off + 2], this.data[off + 3]);
    }

    private static long bytesToLong(byte a, byte b, byte c, byte d) {
        return int2long(((a & 0xff) << 24) | ((b & 0xff) << 16) | ((c & 0xff) << 8) | (d & 0xff));
    }

    private static long int2long(int i) {
        long l = i & 0x7fffffffL;
        if (i < 0) {
            l |= 0x080000000L;
        }
        return l;
    }

    private static final class Meta {
        int build;
        int ipVersion;
        int nodeCount;
        Map<String, Integer> languages = new HashMap<>();
        String[] fields = new String[0];
        int totalSize;
    }

    private static final class NotFoundException extends Exception {
    }

    public static final class IpdbException extends Exception {
        public IpdbException(String m) {
            super(m);
        }
    }
}
