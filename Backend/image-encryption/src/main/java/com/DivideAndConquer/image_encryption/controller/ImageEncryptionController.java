package com.DivideAndConquer.image_encryption.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.DivideAndConquer.image_encryption.service.ImageEncryptor;
import com.DivideAndConquer.image_encryption.service.ImageDecryptor;

import java.io.IOException;
import java.util.Map;

@CrossOrigin(origins = "https://image-encryptor-online.onrender.com")

@RestController
@RequestMapping("/api/image")
public class ImageEncryptionController
{
    private final ImageEncryptor encryptor = new ImageEncryptor();
    private final ImageDecryptor decryptor = new ImageDecryptor();

    @PostMapping("/encrypt")
    public ResponseEntity<?> encryptImage(@RequestParam("file") MultipartFile file, @RequestParam("key") long key) {
        try {
            byte[] imageBytes = file.getBytes();

            // Encrypt the image
            byte[] encryptedBytes = encryptor.encrypt(imageBytes, key);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG); // Or the format to return

            return new ResponseEntity<>(encryptedBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Encryption failed: " + e.getMessage()));
        }
    }

    @PostMapping("/decrypt")
    public ResponseEntity<?> decryptImage(@RequestParam("file") MultipartFile file, @RequestParam("key") long key) {
        try {
            byte[] imageBytes = file.getBytes();
            byte[] decryptedBytes = decryptor.decrypt(imageBytes, key);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);

            return new ResponseEntity<>(decryptedBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Decryption failed: " + e.getMessage()));
        }
    }
}