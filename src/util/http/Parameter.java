//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package util.http;

import core.Encoding;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import util.functions;

public class Parameter implements Serializable {
    public static final byte MAP_VALUE = 1;
    public static final byte BYTES_VALUE = 2;
    protected HashMap<String, Object> hashMap = new HashMap();
    protected Encoding encoding;

    public Parameter() {
    }

    public void setEncoding(Encoding encoding) {
        this.encoding = encoding;
    }

    public String getParameterString(String key) {
        byte[] retByteArray = this.getParameterByteArray(key);
        if (retByteArray != null) {
            return this.encoding != null ? this.encoding.Decoding(retByteArray) : new String(retByteArray);
        } else {
            return null;
        }
    }

    public byte[] getParameterByteArray(String key) {
        byte[] retByteArray = (byte[])((byte[])this.hashMap.get(key));
        return retByteArray;
    }

    public Parameter getParameter(String key) {
        Parameter parameter = (Parameter)this.hashMap.get(key);
        return parameter;
    }

    public Parameter addParameterString(String key, String value) {
        this.addParameterByteArray(key, value.getBytes());
        return this;
    }

    public synchronized Parameter addParameterByteArray(String key, byte[] value) {
        this.hashMap.put(key, value);
        return this;
    }

    public synchronized Parameter addParameter(String key, Parameter value) {
        this.hashMap.put(key, value);
        return this;
    }

    public Parameter remove(String key) {
        this.hashMap.remove(key);
        return this;
    }

    public byte[] get(String key) {
        return this.getParameterByteArray(key);
    }

    public Parameter add(String key, String value) {
        this.addParameterString(key, value);
        return this;
    }

    public Parameter add(String key, Parameter value) {
        this.addParameter(key, value);
        return this;
    }

    public Parameter add(String key, byte[] value) {
        this.addParameterByteArray(key, value);
        return this;
    }

    public long getSize() {
        return (long)this.hashMap.size();
    }

    public int len() {
        return this.serialize().length;
    }

    public Set<String> keys() {
        return this.hashMap.keySet();
    }

    public Set<String> keySet() {
        return this.keys();
    }

    public Collection values() {
        return this.hashMap.values();
    }

    public static Parameter deserialize(byte[] parameterByte) {
        if (parameterByte == null) {
            return new Parameter();
        }
        return deserialize((InputStream)(new ByteArrayInputStream(parameterByte)));
    }

    public static Parameter deserialize(InputStream inputStream) {
        Parameter resParameter = new Parameter();
        ByteArrayOutputStream stringBuffer = new ByteArrayOutputStream();
        String key = null;
        byte[] lenBytes = new byte[4];


        try {
            while(true) {
                byte tmpByte = (byte)inputStream.read();
                if (tmpByte == -1) {
                    break;
                }

                int len;
                if (tmpByte == 1) {
                    key = stringBuffer.toString();
                    inputStream.read(lenBytes);
                    len = functions.bytesToInt(lenBytes);
                    resParameter.addParameter(key, deserialize(functions.readInputStream(inputStream, len)));
                    stringBuffer.reset();
                } else if (tmpByte == 2) {
                    key = stringBuffer.toString();
                    inputStream.read(lenBytes);
                    len = functions.bytesToInt(lenBytes);
                    byte[] data = functions.readInputStream(inputStream, len);
                    resParameter.addParameterByteArray(key, data);
                    stringBuffer.reset();
                } else {
                    if (tmpByte <= 32 || tmpByte > 126) {
                        break;
                    }

                    stringBuffer.write(tmpByte);
                }
            }

            stringBuffer.close();
            inputStream.close();
        } catch (Exception var8) {
            var8.printStackTrace();
        }

        return resParameter.hashMap.size() > 0 ? resParameter : null;
    }

    public byte[] serialize() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        this.serialize(outputStream);
        return outputStream.toByteArray();
    }

    public void serialize(ByteArrayOutputStream outputStream) {
        Iterator<String> keys = this.hashMap.keySet().iterator();


        while(keys.hasNext()) {
            try {
                String key = (String)keys.next();
                Object _value = this.hashMap.get(key);
                outputStream.write(key.getBytes());
                byte[] value;
                if (_value instanceof byte[]) {
                    outputStream.write(2);
                    value = (byte[])((byte[])_value);
                } else if (_value instanceof Parameter) {
                    outputStream.write(1);
                    value = ((Parameter)_value).serialize();
                } else {
                    outputStream.write(2);
                    value = _value.toString().getBytes();
                }

                outputStream.write(functions.intToBytes(value.length));
                outputStream.write(value);
            } catch (Exception var6) {
                var6.printStackTrace();
            }
        }

    }
}
