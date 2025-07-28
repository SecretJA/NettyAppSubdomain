package server.crypto;

import java.security.*;
import java.security.spec.*;
import java.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.io.*;

/**
 * Tiện ích mã hóa cho server: load khóa, ký, xác thực, mã hóa AES.
 */
public class CryptoUtils {
    /**
     * Đọc khóa riêng từ file PEM.
     */
    public static PrivateKey loadPrivateKey(String pemPath) throws Exception {
        try (BufferedReader br = new BufferedReader(new FileReader(pemPath))) {
            StringBuilder keyBuilder = new StringBuilder();
            String line;
            boolean inKey = false;
            while ((line = br.readLine()) != null) {
                if (line.contains("-----BEGIN PRIVATE KEY-----")) {
                    inKey = true;
                } else if (line.contains("-----END PRIVATE KEY-----")) {
                    break;
                } else if (inKey) {
                    keyBuilder.append(line);
                }
            }
            byte[] keyBytes = Base64.getDecoder().decode(keyBuilder.toString());
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        } catch (Exception e) {
            System.err.println("[CryptoUtils] Lỗi loadPrivateKey: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Ký dữ liệu bằng SHA256withRSA, trả về Base64.
     */
    public static String sign(String data, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(data.getBytes());
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    /**
     * Xác thực chữ ký với public key.
     */
    public static boolean verify(String data, String base64Signature, PublicKey publicKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(data.getBytes());
        return signature.verify(Base64.getDecoder().decode(base64Signature));
    }

    /**
     * Mã hóa AES-CBC với key và IV ngẫu nhiên.
     */
    public static byte[] encryptAES(byte[] data, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        return cipher.doFinal(data);
    }

    /**
     * Giải mã AES-CBC.
     */
    public static byte[] decryptAES(byte[] encrypted, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        return cipher.doFinal(encrypted);
    }
    
    /**
     * Giải mã dữ liệu đã mã hóa AES (tương thích với client).
     */
    public static byte[] decryptWithAES(byte[] encryptedData, SecretKey key, byte[] iv) throws Exception {
        return decryptAES(encryptedData, key, iv);
    }

    /**
     * Sinh cặp khóa RSA mới.
     */
    public static KeyPair generateRSAKeyPair(int keySize) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(keySize);
        return keyGen.generateKeyPair();
    }

    /**
     * Lưu private key ra file PEM.
     */
    public static void savePrivateKey(PrivateKey privateKey, String filePath) throws Exception {
        try {
            File file = new File(filePath);
            file.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("-----BEGIN PRIVATE KEY-----\n");
                writer.write(Base64.getEncoder().encodeToString(privateKey.getEncoded()));
                writer.write("\n-----END PRIVATE KEY-----\n");
            }
        } catch (Exception e) {
            System.err.println("[CryptoUtils] Lỗi savePrivateKey: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Lưu public key ra file PEM.
     */
    public static void savePublicKey(PublicKey publicKey, String filePath) throws Exception {
        byte[] encoded = publicKey.getEncoded();
        String base64 = Base64.getEncoder().encodeToString(encoded);
        try (PrintWriter out = new PrintWriter(filePath)) {
            out.println("-----BEGIN PUBLIC KEY-----");
            for (int i = 0; i < base64.length(); i += 64) {
                out.println(base64.substring(i, Math.min(i + 64, base64.length())));
            }
            out.println("-----END PUBLIC KEY-----");
        }
    }

    /**
     * Đọc public key từ file PEM.
     */
    public static PublicKey loadPublicKey(String pemPath) throws Exception {
        StringBuilder keyBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(pemPath))) {
            String line;
            boolean inKey = false;
            while ((line = br.readLine()) != null) {
                if (line.contains("-----BEGIN PUBLIC KEY-----")) {
                    inKey = true;
                } else if (line.contains("-----END PUBLIC KEY-----")) {
                    break;
                } else if (inKey) {
                    keyBuilder.append(line);
                }
            }
        }
        byte[] keyBytes = Base64.getDecoder().decode(keyBuilder.toString());
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    public static void savePublicKeyToDer(PublicKey publicKey, String filePath) throws Exception {
        try {
            File file = new File(filePath);
            file.getParentFile().mkdirs();
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(publicKey.getEncoded());
            }
        } catch (Exception e) {
            System.err.println("[CryptoUtils] Lỗi savePublicKeyToDer: " + e.getMessage());
            throw e;
        }
    }

    public static void savePrivateKeyToFile(PrivateKey privateKey, String filePath) throws Exception {
        try {
            File file = new File(filePath);
            file.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("-----BEGIN PRIVATE KEY-----\n");
                writer.write(Base64.getEncoder().encodeToString(privateKey.getEncoded()));
                writer.write("\n-----END PRIVATE KEY-----\n");
            }
        } catch (Exception e) {
            System.err.println("[CryptoUtils] Lỗi savePrivateKeyToFile: " + e.getMessage());
            throw e;
        }
    }
} 