package com.example.Projectly.config.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
@Slf4j
public class KeyGeneratorRunner implements CommandLineRunner {

    private static final String KEYS_DIR = "src/main/resources/keys";
    private static final String PRIVATE_KEY_PATH = KEYS_DIR + "/private_key.pem";
    private static final String PUBLIC_KEY_PATH = KEYS_DIR + "/public_key.pem";

    @Override
    public void run(String... args) throws Exception {
        Path keysDir = Paths.get(KEYS_DIR);
        Path privateKeyPath = Paths.get(PRIVATE_KEY_PATH);
        Path publicKeyPath = Paths.get(PUBLIC_KEY_PATH);

        // Check if keys already exist
        if (Files.exists(privateKeyPath) && Files.exists(publicKeyPath)) {
            log.info("RSA keys already exist. Skipping key generation.");
            return;
        }

        log.info("Generating RSA key pair...");
        
        // Create keys directory if it doesn't exist
        if (!Files.exists(keysDir)) {
            Files.createDirectories(keysDir);
        }

        // Generate RSA key pair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        // Save private key
        savePrivateKey(keyPair.getPrivate(), PRIVATE_KEY_PATH);
        
        // Save public key
        savePublicKey(keyPair.getPublic(), PUBLIC_KEY_PATH);

        log.info("RSA key pair generated successfully!");
        log.info("Private key saved to: {}", PRIVATE_KEY_PATH);
        log.info("Public key saved to: {}", PUBLIC_KEY_PATH);
    }

    private void savePrivateKey(PrivateKey privateKey, String filePath) throws IOException {
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
        String base64PrivateKey = Base64.getEncoder().encodeToString(pkcs8EncodedKeySpec.getEncoded());
        
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("-----BEGIN PRIVATE KEY-----\n");
            writer.write(formatBase64(base64PrivateKey));
            writer.write("\n-----END PRIVATE KEY-----\n");
        }
    }

    private void savePublicKey(PublicKey publicKey, String filePath) throws IOException {
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKey.getEncoded());
        String base64PublicKey = Base64.getEncoder().encodeToString(x509EncodedKeySpec.getEncoded());
        
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("-----BEGIN PUBLIC KEY-----\n");
            writer.write(formatBase64(base64PublicKey));
            writer.write("\n-----END PUBLIC KEY-----\n");
        }
    }

    private String formatBase64(String base64) {
        StringBuilder formatted = new StringBuilder();
        int index = 0;
        while (index < base64.length()) {
            formatted.append(base64, index, Math.min(index + 64, base64.length()));
            formatted.append("\n");
            index += 64;
        }
        return formatted.toString().trim();
    }
}
