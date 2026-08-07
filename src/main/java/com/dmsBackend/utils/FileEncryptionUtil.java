package com.dmsBackend.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class FileEncryptionUtil {

    private static final String ALGO = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final byte[] secretKey;

    // ✅ Inject Base64 key safely
    public FileEncryptionUtil(@Value("${AES_KEY}") String base64Key) {

        byte[] decoded = Base64.getDecoder().decode(base64Key);

        if (decoded.length != 16) {
            throw new IllegalArgumentException(
                    "AES-128 key must be exactly 16 bytes (Base64-encoded)"
            );
        }

        this.secretKey = decoded;
    }

    public void encrypt(InputStream in, OutputStream out) throws Exception {

        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(
                Cipher.ENCRYPT_MODE,
                new SecretKeySpec(secretKey, ALGO),
                new GCMParameterSpec(GCM_TAG_LENGTH, iv)
        );

        // Write IV at beginning
        out.write(iv);

        try (CipherOutputStream cipherOut = new CipherOutputStream(out, cipher)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                cipherOut.write(buffer, 0, len);
            }
        }
    }

    public CipherInputStream decrypt(InputStream encryptedInput) throws Exception {

        byte[] iv = new byte[GCM_IV_LENGTH];
        if (encryptedInput.read(iv) != GCM_IV_LENGTH) {
            throw new IllegalStateException("Invalid encrypted file (IV missing)");
        }

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(
                Cipher.DECRYPT_MODE,
                new SecretKeySpec(secretKey, ALGO),
                new GCMParameterSpec(GCM_TAG_LENGTH, iv)
        );

        return new CipherInputStream(encryptedInput, cipher);
    }
}
