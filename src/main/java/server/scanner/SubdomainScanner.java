package server.scanner;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Quét subdomain từ wordlist, kiểm tra DNS, thu thập domain hợp lệ.
 */
public class SubdomainScanner {
    private static final String TARGET_DOMAIN = "huflit.edu.vn";
    private static final int TIMEOUT_MS = 3000; // 3 giây timeout
    private static final int MAX_THREADS = 1000; // Số thread tối đa (tối ưu cho i5 đời 8, có thể tăng/giảm tuỳ máy)
    
    /**
     * Đọc wordlist và kiểm tra subdomain.
     */
    public static List<String> scan(String wordlistPath) {
        List<String> found = new ArrayList<>();
        List<String> subdomains = new ArrayList<>();
        
        // Đọc wordlist
        try (BufferedReader br = new BufferedReader(new FileReader(wordlistPath))) {
            String line;
            //int count = 0;
            while ((line = br.readLine()) != null /*&& count < 1000*/) {
                String subdomain = line.trim();
                if (!subdomain.isEmpty() && !subdomain.startsWith("#")) {
                    subdomains.add(subdomain);
                    //count ++;
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Lỗi đọc wordlist: " + e.getMessage());
            return found;
        }
        
        System.out.println("🔍 Bắt đầu scan " + subdomains.size() + " subdomain...");
        
        // Scan với thread pool
        ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);
        List<Future<String>> futures = new ArrayList<>();
        
        for (String subdomain : subdomains) {
            String fullDomain = subdomain + "." + TARGET_DOMAIN;
            futures.add(executor.submit(() -> checkDomain(fullDomain)));
        }
        
        // Thu thập kết quả
        for (Future<String> future : futures) {
            try {
                String result = future.get(5, TimeUnit.SECONDS); // 5 giây timeout cho mỗi thread
                if (result != null) {
                    found.add(result);
                }
            } catch (Exception e) {
                // Timeout hoặc lỗi - bỏ qua
            }
        }
        
        executor.shutdown();
        System.out.println("✅ Scan hoàn thành! Tìm thấy " + found.size() + " subdomain.");
        
        return found;
    }
    
    /**
     * Kiểm tra một domain có tồn tại không.
     */
    private static String checkDomain(String domain) {
        try {
            // Tạo URL connection với timeout
            URL url = new URL("http://" + domain);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestMethod("HEAD");
            
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 400) {
                return domain;
            }
        } catch (Exception e) {
            // Domain không tồn tại hoặc lỗi kết nối
        }
        return null;
    }
    
    /**
     * Scan nhanh với DNS lookup (không HTTP) và trả về thống kê.
     */
    public static ScanResult scanDNSWithStats(String wordlistPath) {
        List<String> found = new ArrayList<>();
        List<String> subdomains = new ArrayList<>();
        
        // Đọc wordlist
        try (BufferedReader br = new BufferedReader(new FileReader(wordlistPath))) {
            String line;
            //int count = 0;
            while ((line = br.readLine()) != null /*&& count < 1000*/) { // Giới hạn 1000 subdomain để test
                String subdomain = line.trim();
                if (!subdomain.isEmpty() && !subdomain.startsWith("#")) {
                    subdomains.add(subdomain);
                    //count++;
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Lỗi đọc wordlist: " + e.getMessage());
            return new ScanResult(new ArrayList<>(), 0, 0);
        }
        
        System.out.println("🔍 Bắt đầu scan DNS " + subdomains.size() + " subdomain...");
        
        for (String subdomain : subdomains) {
            String fullDomain = subdomain + "." + TARGET_DOMAIN;
            try {
                InetAddress.getByName(fullDomain);
                found.add(fullDomain);
                System.out.println("✅ Tìm thấy: " + fullDomain);
            } catch (UnknownHostException ignored) {
                // Domain không tồn tại
            }
        }
        
        System.out.println("✅ Scan DNS hoàn thành! Tìm thấy " + found.size() + " subdomain.");
        return new ScanResult(found, subdomains.size(), found.size());
    }
    
    /**
     * Scan nhanh với DNS lookup (không HTTP) và trả về thống kê.
     */
    public static ScanResult scanDNSWithStats(String wordlistPath, String targetDomain) {
        List<String> found = new ArrayList<>();
        List<String> subdomains = new ArrayList<>();
        // Đọc wordlist
        try (BufferedReader br = new BufferedReader(new FileReader(wordlistPath))) {
            String line;
            //int count =0;
            while ((line = br.readLine()) != null /*&& count <1000*/) {
                String subdomain = line.trim();
                if (!subdomain.isEmpty() && !subdomain.startsWith("#")) {
                    subdomains.add(subdomain);
                    //count ++;
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Lỗi đọc wordlist: " + e.getMessage());
            return new ScanResult(new ArrayList<>(), 0, 0);
        }
        System.out.println("🔍 Bắt đầu scan DNS " + subdomains.size() + " subdomain cho domain: " + targetDomain);
        // Sử dụng thread pool để tăng tốc
        ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);
        List<Future<String>> futures = new ArrayList<>();
        for (String subdomain : subdomains) {
            String fullDomain = subdomain + "." + targetDomain;
            futures.add(executor.submit(() -> {
                try {
                    InetAddress.getByName(fullDomain);
                    return fullDomain;
                } catch (UnknownHostException ignored) {
                    return null;
                }
            }));
        }
        for (Future<String> future : futures) {
            try {
                String result = future.get(5, TimeUnit.SECONDS); // 5 giây timeout cho mỗi thread
                if (result != null) {
                    found.add(result);
                    System.out.println("✅ Tìm thấy: " + result);
                }
            } catch (Exception e) {
                // Timeout hoặc lỗi - bỏ qua
            }
        }
        executor.shutdown();
        System.out.println("✅ Scan DNS hoàn thành! Tìm thấy " + found.size() + " subdomain.");
        return new ScanResult(found, subdomains.size(), found.size());
    }
    
    /**
     * Kết quả scan với thống kê chi tiết.
     */
    public static class ScanResult {
        public final List<String> foundDomains;
        public final int totalScanned;
        public final int totalFound;
        
        public ScanResult(List<String> foundDomains, int totalScanned, int totalFound) {
            this.foundDomains = foundDomains;
            this.totalScanned = totalScanned;
            this.totalFound = totalFound;
        }
        
        public int getTotalNotFound() {
            return totalScanned - totalFound;
        }
    }
} 