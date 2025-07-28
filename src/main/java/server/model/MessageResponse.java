package server.model;

import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Đối tượng phản hồi gửi về client.
 * Chứa trạng thái xác thực và kết quả scan subdomain.
 */
public class MessageResponse {
    @JsonProperty("status")
    public String status;
    
    @JsonProperty("result")
    public String result;
    
    @JsonProperty("foundDomains")
    public List<String> foundDomains; // Danh sách domain tìm được
    
    @JsonProperty("totalScanned")
    public int totalScanned; // Tổng số domain đã scan
    
    @JsonProperty("totalFound")
    public int totalFound; // Tổng số domain tìm được
    
    @JsonProperty("totalNotFound")
    public int totalNotFound; // Tổng số domain không tìm thấy

    @JsonProperty("targetDomain")
    public String targetDomain;

    // Constructor mặc định cho Jackson
    public MessageResponse() {
        this.status = "";
        this.result = "";
        this.foundDomains = new ArrayList<>();
        this.totalScanned = 0;
        this.totalFound = 0;
        this.totalNotFound = 0;
    }

    public MessageResponse(String status, String result) {
        this.status = status;
        this.result = result;
        this.foundDomains = new ArrayList<>();
        this.totalScanned = 0;
        this.totalFound = 0;
        this.totalNotFound = 0;
    }

    public MessageResponse(String status, String result, List<String> foundDomains, int totalScanned, int totalFound) {
        this.status = status;
        this.result = result;
        this.foundDomains = foundDomains != null ? foundDomains : new ArrayList<>();
        this.totalScanned = totalScanned;
        this.totalFound = totalFound;
        this.totalNotFound = totalScanned - totalFound;
    }

    // Getter methods
    public String getStatus() { return status; }
    public String getResult() { return result; }
    public List<String> getFoundDomains() { return foundDomains; }
    public int getTotalScanned() { return totalScanned; }
    public int getTotalFound() { return totalFound; }
    public int getTotalNotFound() { return totalNotFound; }
    public String getTargetDomain() { return targetDomain; }

    // Setter methods
    public void setStatus(String status) { this.status = status; }
    public void setResult(String result) { this.result = result; }
    public void setFoundDomains(List<String> foundDomains) { this.foundDomains = foundDomains; }
    public void setTotalScanned(int totalScanned) { this.totalScanned = totalScanned; }
    public void setTotalFound(int totalFound) { this.totalFound = totalFound; }
    public void setTotalNotFound(int totalNotFound) { this.totalNotFound = totalNotFound; }
    public void setTargetDomain(String targetDomain) { this.targetDomain = targetDomain; }
} 