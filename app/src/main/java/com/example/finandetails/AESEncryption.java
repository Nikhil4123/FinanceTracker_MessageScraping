package com.example.finandetails;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class AESEncryption {

    private static final String AES = "AES";
    private static final String MY_SECRET_KEY = "@7djsridher"; // Your custom key

    // Method to generate a secret key from the custom string key
    private static SecretKey generateKey() throws Exception {
        byte[] key = MY_SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        key = sha.digest(key);
        key = Arrays.copyOf(key, 16); // Use only first 128 bit (16 bytes)
        return new SecretKeySpec(key, AES);
    }

    // Method to encrypt a plain text using AES
    public static String encrypt(String plainText) throws Exception {
        SecretKey secretKey = generateKey();
        Cipher cipher = Cipher.getInstance(AES);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
    }

    // Method to decrypt a cipher text using AES
    public static String decrypt(String encryptedText) throws Exception {
        SecretKey secretKey = generateKey();
        Cipher cipher = Cipher.getInstance(AES);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decodedBytes = Base64.decode(encryptedText, Base64.DEFAULT);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}
