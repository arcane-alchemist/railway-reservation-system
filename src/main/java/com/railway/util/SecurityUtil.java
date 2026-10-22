package com.railway.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Security utility for cryptographic password hashing and verification.
 * Uses SHA-256 with cryptographically secure random salt to prevent rainbow table attacks.
 */
public class SecurityUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generates a 16-byte cryptographically secure random salt (Base64 encoded).
     */
    public static String generateSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hashes password using SHA-256 with provided salt.
     * Returns format: "salt:hash"
     */
    public static String hashPassword(String password) {
        String salt = generateSalt();
        String hash = hashWithSalt(password, salt);
        return salt + ":" + hash;
    }

    /**
     * Hashes password with fixed salt (useful for deterministic seeds or verification).
     */
    public static String hashWithSalt(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hashedPassword = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verifies plain password against stored "salt:hash" or fallback plain-text (for backwards compatibility).
     */
    public static boolean verifyPassword(String inputPassword, String storedValue) {
        if (inputPassword == null || storedValue == null) return false;

        // Check if storedValue contains salt:hash format
        if (storedValue.contains(":")) {
            String[] parts = storedValue.split(":", 2);
            String salt = parts[0];
            String expectedHash = parts[1];
            String actualHash = hashWithSalt(inputPassword, salt);
            return MessageDigest.isEqual(
                    expectedHash.getBytes(StandardCharsets.UTF_8),
                    actualHash.getBytes(StandardCharsets.UTF_8)
            );
        }

        // Backward compatibility fallback for initial plain demo seeds
        return inputPassword.equals(storedValue);
    }
}
