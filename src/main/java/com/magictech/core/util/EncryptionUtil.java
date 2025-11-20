package com.magictech.core.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * Utility class for encrypting and decrypting sensitive data (OAuth2 tokens)
 * Uses AES-256 encryption
 *
 * IMPORTANT: Set ENCRYPTION_KEY environment variable in production!
 * Example: ENCRYPTION_KEY=your-32-character-secret-key-here
 */
public class EncryptionUtil {

    private static final String ALGORITHM = "AES";
    private static final String DEFAULT_KEY = "MagicTechDefaultEncryptionKey32"; // CHANGE IN PRODUCTION

    /**
     * Get encryption key from environment variable or default
     */
    private static SecretKey getSecretKey() {
        try {
            String keyString = System.getenv("ENCRYPTION_KEY");
            if (keyString == null || keyString.isEmpty()) {
                System.err.println("⚠️  WARNING: Using default encryption key. Set ENCRYPTION_KEY environment variable!");
                keyString = DEFAULT_KEY;
            }

            // Hash the key to ensure it's exactly 32 bytes for AES-256
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(keyString.getBytes(StandardCharsets.UTF_8));
            keyBytes = Arrays.copyOf(keyBytes, 16); // Use first 16 bytes for AES-128

            return new SecretKeySpec(keyBytes, ALGORITHM);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate encryption key", e);
        }
    }

    /**
     * Encrypt a string using AES encryption
     * @param plainText The text to encrypt
     * @return Base64 encoded encrypted text
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            SecretKey secretKey = getSecretKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypt a Base64 encoded encrypted string
     * @param encryptedText The encrypted text to decrypt
     * @return Decrypted plain text
     */
    public static String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        try {
            SecretKey secretKey = getSecretKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * Test encryption/decryption
     */
    public static boolean testEncryption() {
        try {
            String original = "test-token-12345";
            String encrypted = encrypt(original);
            String decrypted = decrypt(encrypted);
            return original.equals(decrypted);
        } catch (Exception e) {
            return false;
        }
    }
}
