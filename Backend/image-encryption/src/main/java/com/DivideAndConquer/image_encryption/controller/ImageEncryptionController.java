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

@CrossOrigin(origins = "*")     //to solve cors issue for now(this solution is not secure)

@RestController
@RequestMapping("/api/image")
public class ImageEncryptionController
{
    private final ImageEncryptor encryptor = new ImageEncryptor();
    private final ImageDecryptor decryptor = new ImageDecryptor();

    @PostMapping("/encrypt")
    public ResponseEntity<byte[]> encryptImage(@RequestParam("file") MultipartFile file, @RequestParam("key") long key) {
        try {
            byte[] imageBytes = file.getBytes();

            // Encrypt the image (replace this with your algorithm)
            byte[] encryptedBytes = encryptor.encrypt(imageBytes, key);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG); // Or the format you return

            return new ResponseEntity<>(encryptedBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/decrypt")
    public ResponseEntity<byte[]> decryptImage(@RequestParam("file") MultipartFile file, @RequestParam("key") long key) {
        try {
            byte[] imageBytes = file.getBytes();
            byte[] decryptedBytes = decryptor.decrypt(imageBytes, key);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);

            return new ResponseEntity<>(decryptedBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /* no use for this as of now
    private byte[] encryptImageBytes(byte[] imageBytes) {
        // Sample encryption: just reversing bytes for demonstration.
        // Replace this with your actual encryption algorithm.
        for (int i = 0; i < imageBytes.length / 2; i++) {
            byte temp = imageBytes[i];
            imageBytes[i] = imageBytes[imageBytes.length - i - 1];
            imageBytes[imageBytes.length - i - 1] = temp;
        }
        return imageBytes;
    }*/
}
