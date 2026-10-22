package com.railway.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityUtilTest {

    @Test
    public void testHashAndVerifyCorrectPassword() {
        String rawPassword = "SuperSecretPassword123!";
        String storedHash = SecurityUtil.hashPassword(rawPassword);

        assertNotNull(storedHash);
        assertTrue(storedHash.contains(":"));

        boolean verified = SecurityUtil.verifyPassword(rawPassword, storedHash);
        assertTrue(verified, "Password should verify successfully against stored salt:hash");
    }

    @Test
    public void testVerifyIncorrectPasswordFails() {
        String rawPassword = "CorrectPassword";
        String storedHash = SecurityUtil.hashPassword(rawPassword);

        boolean verified = SecurityUtil.verifyPassword("WrongPassword", storedHash);
        assertFalse(verified, "Incorrect password must be rejected");
    }

    @Test
    public void testUniqueSaltsPerHash() {
        String password = "SamePassword";
        String hash1 = SecurityUtil.hashPassword(password);
        String hash2 = SecurityUtil.hashPassword(password);

        assertNotEquals(hash1, hash2, "Different salts should produce distinct stored hashes");
    }
}
