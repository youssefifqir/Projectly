package com.example.Projectly.common.util;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class KeyUtils {

    private KeyUtils() {}

    public static PrivateKey loadPrivateKey(final String pemPath) throws Exception {
        final String key = readKeyFromResource(pemPath)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        final byte[] decoded = Base64.getDecoder().decode(key);
        final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }

    public static PublicKey loadPublicKey(final String pemPath) throws Exception {
        final String key = readKeyFromResource(pemPath)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        final byte[] decoded = Base64.getDecoder().decode(key);
        final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }

    public static void saveKeyPair(KeyPair keyPair, String privateKeyPath, String publicKeyPath) throws Exception {
        // Create directory if it doesn't exist
        Path privateKeyFilePath = Paths.get("src/main/resources/" + privateKeyPath);
        Files.createDirectories(privateKeyFilePath.getParent());

        // Save private key
        savePrivateKey(keyPair.getPrivate(), "src/main/resources/" + privateKeyPath);

        // Save public key
        savePublicKey(keyPair.getPublic(), "src/main/resources/" + publicKeyPath);
    }

    private static void savePrivateKey(PrivateKey privateKey, String filePath) throws Exception {
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
        String base64PrivateKey = Base64.getEncoder().encodeToString(pkcs8EncodedKeySpec.getEncoded());

        try (OutputStream os = new FileOutputStream(filePath)) {
            os.write("-----BEGIN PRIVATE KEY-----\n".getBytes());
            os.write(formatBase64(base64PrivateKey).getBytes());
            os.write("\n-----END PRIVATE KEY-----\n".getBytes());
        }
    }

    private static void savePublicKey(PublicKey publicKey, String filePath) throws Exception {
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKey.getEncoded());
        String base64PublicKey = Base64.getEncoder().encodeToString(x509EncodedKeySpec.getEncoded());

        try (OutputStream os = new FileOutputStream(filePath)) {
            os.write("-----BEGIN PUBLIC KEY-----\n".getBytes());
            os.write(formatBase64(base64PublicKey).getBytes());
            os.write("\n-----END PUBLIC KEY-----\n".getBytes());
        }
    }

    private static String formatBase64(String base64) {
        StringBuilder formatted = new StringBuilder();
        int index = 0;
        while (index < base64.length()) {
            formatted.append(base64, index, Math.min(index + 64, base64.length()));
            formatted.append("\n");
            index += 64;
        }
        return formatted.toString().trim();
    }

    private static String readKeyFromResource(final String path) throws Exception {
        try (final InputStream is = KeyUtils.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalArgumentException("Key not found: " + path);
            }
            return new String(is.readAllBytes());
        }
    }
}
