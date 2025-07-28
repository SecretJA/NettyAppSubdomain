package client.crypto;

import java.security.*;
import java.security.spec.*;
import java.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.io.*;
import javax.crypto.KeyGenerator;
import java.io.BufferedReader;
import java.io.FileReader;

/**
 * Tiện ích mã hóa cho client: load khóa, ký, xác thực, mã hóa AES.
 */
public class CryptoUtils {
    
    /**
     * Sinh cặp key RSA 2048 bit.
     */
    public static KeyPair generateRSAKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }
    
    /**
     * Lưu private key vào file PEM.
     */
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
    
    /**
     * Lưu public key vào file DER (raw bytes).
     */
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

    /**
     * Lưu public key vào file PEM (Base64).
     */
    public static void savePublicKeyToPem(PublicKey publicKey, String filePath) throws Exception {
        File file = new File(filePath);
        file.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("-----BEGIN PUBLIC KEY-----\n");
            writer.write(Base64.getEncoder().encodeToString(publicKey.getEncoded()));
            writer.write("\n-----END PUBLIC KEY-----\n");
        }
    }
    
    /**
     * Đọc khóa riêng từ file PEM.
     */
    public static PrivateKey loadPrivateKey(String pemPath) throws Exception {
        try (BufferedReader reader = new BufferedReader(new FileReader(pemPath))) {
            StringBuilder keyBuilder = new StringBuilder();
            String line;
            boolean inKey = false;
            
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("-----BEGIN PRIVATE KEY-----")) {
                    inKey = true;
                    continue;
                }
                if (line.startsWith("-----END PRIVATE KEY-----")) {
                    break;
                }
                if (inKey) {
                    keyBuilder.append(line);
                }
            }
            
            String keyString = keyBuilder.toString().replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(keyString);
            
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
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
     * Mã hóa public key bằng AES-CBC (trả về Base64).
     */
    public static String encryptPublicKeyWithAES(byte[] publicKeyBytes, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        return Base64.getEncoder().encodeToString(cipher.doFinal(publicKeyBytes));
    }

    /**
     * Mã hóa toàn bộ message bằng AES-CBC (trả về Base64).
     */
    public static String encryptMessageWithAES(String message, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        return Base64.getEncoder().encodeToString(cipher.doFinal(message.getBytes()));
    }

    /**
     * Tạo key AES ngẫu nhiên.
     */
    public static SecretKey generateAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        return keyGen.generateKey();
    }

    /**
     * Tạo IV ngẫu nhiên.
     */
    public static byte[] generateIV() {
        byte[] iv = new byte[16];
        new java.security.SecureRandom().nextBytes(iv);
        return iv;
    }

    /**
     * Đọc public key từ chuỗi Base64 (nhận từ server).
     */
    public static PublicKey loadPublicKeyFromBase64(String base64) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    /**
     * Mã hóa dữ liệu (byte[]) bằng public key RSA (trả về Base64).
     */
    public static String encryptWithPublicKey(byte[] data, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encrypted = cipher.doFinal(data);
        return Base64.getEncoder().encodeToString(encrypted);
    }
} 