//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package shells.channel;

import core.ProxyT;
import core.shell.ShellEntity;
import java.io.ByteArrayOutputStream;
import java.net.Proxy;
import util.functions;
import util.http.Http;
import util.http.HttpResponse;

public class HttpRequestChannel extends RequestChannel {
    protected Http http;
    protected Proxy proxy;

    public HttpRequestChannel(ShellEntity ctx) {
        super(ctx);
        this.proxy = ProxyT.getProxy(ctx);
        this.http = new Http(ctx);
        ctx.setMergeResponseCookie(true);
    }

    public byte[] sendRequest(byte[] data) {
        String left = this.ctx.getReqLeft();
        String right = this.ctx.getReqRight();
        if (this.ctx.isSendLRReqData()) {
            byte[] leftData = left.getBytes();
            byte[] rightData = right.getBytes();

            try {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                Throwable var7 = null;

                try {
                    buffer.write(leftData);
                    buffer.write(this.cryption.encode(data));
                    buffer.write(rightData);
                    data = buffer.toByteArray();
                } catch (Throwable var17) {
                    var7 = var17;
                    throw var17;
                } finally {
                    if (buffer != null) {
                        if (var7 != null) {
                            try {
                                buffer.close();
                            } catch (Throwable var16) {
                                var7.addSuppressed(var16);
                            }
                        } else {
                            buffer.close();
                        }
                    }

                }
            } catch (Exception var19) {
                var19.printStackTrace();
            }
        } else {
            data = this.cryption.encode(data);

        }

        String url = this.ctx.getUrl();
        if (url != null) {
            int queryIndex = url.indexOf(63);
            String baseUrl = queryIndex >= 0 ? url.substring(0, queryIndex) : url;
            String query = queryIndex >= 0 ? url.substring(queryIndex) : "";
            if (baseUrl.endsWith("/*")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1) + functions.getRandomApiPath();
                url = baseUrl + query;
            }
        }

        HttpResponse httpResponse = this.sendRequest(url, "POST", data);

        return this.cryption.decode(httpResponse.getResult());
    }

    public HttpResponse sendRequest(String url, String method, byte[] data) {
        return this.http.SendOkHttpConn(url, method, this.ctx.getHeaders(), data, this.ctx.getConnTimeout(), this.ctx.getReadTimeout(), this.proxy);

    }

    public Proxy getProxy() {
        return this.proxy;
    }

    public Http getHttp() {
        return this.http;
    }
}
