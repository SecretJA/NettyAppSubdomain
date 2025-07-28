package server.model;

/**
 * Đối tượng lưu trữ AES key và IV trên server.
 */
public class KeyIvRecord {
    public String aesKey;
    public String aesIv;
    public String timestamp;
    public String targetDomain;
    public String clientIp;

    public KeyIvRecord(String aesKey, String aesIv, String targetDomain, String clientIp) {
        this.aesKey = aesKey;
        this.aesIv = aesIv;
        this.timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
        this.targetDomain = targetDomain;
        this.clientIp = clientIp;
    }

    public KeyIvRecord(String aesKey, String aesIv, String timestamp, String targetDomain, String clientIp) {
        this.aesKey = aesKey;
        this.aesIv = aesIv;
        this.timestamp = timestamp;
        this.targetDomain = targetDomain;
        this.clientIp = clientIp;
    }
} 