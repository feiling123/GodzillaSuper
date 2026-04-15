//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package util.http;

import com.httpProxy.server.response.HttpResponseHeader;
import com.httpProxy.server.response.HttpResponseStatus;
import core.shell.ShellEntity;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import util.functions;

public class HttpResponse {
    private byte[] result;
    private final ShellEntity shellEntity;
    private Map<String, List<String>> headerMap;
    private String message;
    private int responseCode;
    private List<HttpCookie> responseCookies;

    public byte[] getResult() {
        return this.result;
    }

    public Map<String, List<String>> getHeaderMap() {
        return this.headerMap;
    }

    public String getHeader(String key) {
        List<String> values = (List)this.getHeaderMap().get(key);
        return values != null && values.size() > 0 ? (String)values.get(0) : "";
    }

    public String getCookie(String cookieName) {
        List<HttpCookie> cookies = this.getResponseCookies();
        if (cookies != null && cookies.size() > 0) {
            Iterator var3 = cookies.iterator();

            while(var3.hasNext()) {
                HttpCookie cookie = (HttpCookie)var3.next();
                if (cookie.getName().equalsIgnoreCase(cookieName)) {
                    return cookie.getValue();
                }
            }
        }

        return "";
    }

    public List<HttpCookie> getResponseCookies() {
        return this.responseCookies;
    }

    public void setResult(byte[] result) {
        this.result = result;
    }

    public void setHeaderMap(Map<String, List<String>> headerMap) {
        this.headerMap = headerMap;
    }

    public HttpResponse(HttpURLConnection http, ShellEntity shellEntity, URI uri, CookieManager cookieManager) throws IOException {
        this.shellEntity = shellEntity;
        this.handleHeader(uri, cookieManager, http.getHeaderFields());
        this.responseCode = http.getResponseCode();
        this.ReadAllData(this.getInputStream(http), http);
        if (this.result != null && "gzip".equalsIgnoreCase(http.getContentEncoding())) {
            try {
                this.result = functions.gzipD(this.result);
            } catch (Exception var7) {
            }
        }

        try {
            http.disconnect();
        } catch (Exception var6) {
        }

    }

    public HttpResponse(byte[] result, Map<String, List<String>> headerMap, int responseCode, ShellEntity shellEntity, URI uri, CookieManager cookieManager, String contentEncoding) {
        this.shellEntity = shellEntity;
        this.result = result;
        this.responseCode = responseCode;
        this.handleHeader(uri, cookieManager, headerMap);
        if (this.result != null && "gzip".equalsIgnoreCase(contentEncoding)) {
            try {
                this.result = functions.gzipD(this.result);
            } catch (Exception var9) {
            }
        }
    }

    protected synchronized void handleHeader(URI uri, CookieManager cookieManager, Map<String, List<String>> map) {
        this.headerMap = map;

        try {
            if (map == null || map.get((Object)null) == null) {
                throw new RuntimeException("Request timeout");
            }

            List<String> httpMessages = (List)map.get((Object)null);
            if (httpMessages != null && httpMessages.size() > 0) {
                this.message = (String)httpMessages.get(0);
            }

            if (cookieManager != null && uri != null && this.shellEntity.isMergeResponseCookie()) {
                cookieManager.put(uri, map);
                cookieManager.getCookieStore().get(uri);
                List<HttpCookie> cookies = cookieManager.getCookieStore().get(uri);
                this.responseCookies = cookies;
                StringBuilder sb = new StringBuilder();
                cookies.forEach((cookiex) -> {
                    sb.append(String.format(" %s=%s;", cookiex.getName(), cookiex.getValue()));
                });
                if (sb.length() > 0) {
                    LinkedList<String> cookie = (LinkedList)this.shellEntity.getHeaders().get("Cookie");
                    if (cookie == null) {
                        cookie = new LinkedList();
                        this.shellEntity.getHeaders().put("Cookie", cookie);
                    } else {
                        cookie.clear();
                    }

                    cookie.addFirst(sb.deleteCharAt(sb.length() - 1).toString().trim());
                }
            } else {
                this.responseCookies = functions.parseResponseCookies(this.headerMap);
            }
        } catch (IOException var8) {
            var8.printStackTrace();
        }

    }

    protected InputStream getInputStream(HttpURLConnection httpURLConnection) throws IOException {
        try {
            InputStream inputStream = httpURLConnection.getErrorStream();
            return inputStream != null ? inputStream : httpURLConnection.getInputStream();
        } catch (FileNotFoundException var3) {
            return new ByteArrayInputStream(new byte[0]);
        }
    }

    protected void ReadAllData(InputStream inputStream, HttpURLConnection httpURLConnection) throws IOException {
        int maxLen = httpURLConnection.getContentLength();

        try {
            if (maxLen != -1) {
                this.result = this.ReadKnownNumData(inputStream, maxLen);
            } else {
                this.result = this.ReadUnknownNumData(inputStream);
            }
        } catch (NumberFormatException var5) {
            this.result = this.ReadUnknownNumData(inputStream);
        }

    }

    protected byte[] ReadKnownNumData(InputStream inputStream, int num) throws IOException {
        if (num <= 0) {
            return num == 0 ? this.ReadUnknownNumData(inputStream) : null;
        } else {
            byte[] response = new byte[num];
            int read = 0;

            try {
                while(read < response.length) {
                    int readOneNum = inputStream.read(response, read, response.length - read);
                    if (readOneNum < 0) {
                        break;
                    }

                    read += readOneNum;
                }
            } catch (Throwable var6) {
            }

            return read == response.length ? response : java.util.Arrays.copyOf(response, read);
        }
    }

    protected byte[] ReadUnknownNumData(InputStream inputStream) throws IOException {
        byte[] temp = new byte[5120];

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        int readOneNum;
        try {
            while((readOneNum = inputStream.read(temp)) > -1) {
                bos.write(temp, 0, readOneNum);
            }
        } catch (Throwable var6) {
        }

        return bos.toByteArray();
    }

    public com.httpProxy.server.response.HttpResponse parseHttpResponse() {
        com.httpProxy.server.response.HttpResponse httpResponse = new com.httpProxy.server.response.HttpResponse(new HttpResponseStatus(this.responseCode));
        httpResponse.setResponseData(this.result);
        HttpResponseHeader responseHeader = httpResponse.getHttpResponseHeader();
        Iterator<String> headerKeys = this.headerMap.keySet().iterator();

        while(headerKeys.hasNext()) {
            String keyString = (String)headerKeys.next();
            List<String> headList = (List)this.headerMap.get(keyString);
            if (headList != null) {
                headList.parallelStream().forEach((v) -> {
                    responseHeader.addHeader(keyString, v);
                });
            }
        }

        return httpResponse;
    }
}
