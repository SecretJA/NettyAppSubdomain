package server.model;

/**
 * Đối tượng nhận từ client gửi lên server.
 * Chứa raw message, chữ ký, public key mã hóa AES, key và IV AES.
 */
public class MessageRequest {
    public String rawMessage;
    public String signature;
    public String publicKeyAes;
    public String aesKey;
    public String aesIv;
    public String targetDomain;

    // Constructor mặc định cho Jackson
    public MessageRequest() {
    }

    public MessageRequest(String rawMessage, String signature, String publicKeyAes, String aesKey, String aesIv, String targetDomain) {
        this.rawMessage = rawMessage;
        this.signature = signature;
        this.publicKeyAes = publicKeyAes;
        this.aesKey = aesKey;
        this.aesIv = aesIv;
        this.targetDomain = targetDomain;
    }

    // Getters và Setters cho Jackson
    public String getRawMessage() {
        return rawMessage;
    }

    public void setRawMessage(String rawMessage) {
        this.rawMessage = rawMessage;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getPublicKeyAes() {
        return publicKeyAes;
    }

    public void setPublicKeyAes(String publicKeyAes) {
        this.publicKeyAes = publicKeyAes;
    }

    public String getAesKey() {
        return aesKey;
    }

    public void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }

    public String getAesIv() {
        return aesIv;
    }

    public void setAesIv(String aesIv) {
        this.aesIv = aesIv;
    }

    public String getTargetDomain() { return targetDomain; }
    public void setTargetDomain(String targetDomain) { this.targetDomain = targetDomain; }
} 