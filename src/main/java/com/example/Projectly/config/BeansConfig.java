package com.example.Projectly.config;

import com.example.Projectly.common.util.KeyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

@Configuration
@EnableCaching
@EnableAsync
@Slf4j
public class BeansConfig {

    private KeyPair cachedKeyPair;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(final AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuditorAware<String> auditorAware() {
        return new ApplicationAuditorAware();
    }

    @Bean
    public PrivateKey privateKey() throws Exception {
        return getOrGenerateKeyPair().getPrivate();
    }

    @Bean
    public PublicKey publicKey() throws Exception {
        return getOrGenerateKeyPair().getPublic();
    }

    private synchronized KeyPair getOrGenerateKeyPair() throws Exception {
        if (cachedKeyPair != null) {
            return cachedKeyPair;
        }

        try {
            // Try to load existing keys
            PrivateKey privateKey = KeyUtils.loadPrivateKey("keys/private_key.pem");
            PublicKey publicKey = KeyUtils.loadPublicKey("keys/public_key.pem");
            cachedKeyPair = new KeyPair(publicKey, privateKey);
            log.info("Successfully loaded RSA key pair from classpath");
            return cachedKeyPair;
        } catch (IllegalArgumentException e) {
            // Keys not found, generate new key pair
            log.info("RSA keys not found on classpath, generating new key pair...");
            cachedKeyPair = generateKeyPair();
            // Save keys for future use
            KeyUtils.saveKeyPair(cachedKeyPair, "keys/private_key.pem", "keys/public_key.pem");
            log.info("RSA key pair generated and saved successfully");
            return cachedKeyPair;
        }
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }
}
