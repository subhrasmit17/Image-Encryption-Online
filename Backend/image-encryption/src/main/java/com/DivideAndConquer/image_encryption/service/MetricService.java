package com.DivideAndConquer.image_encryption.service;

import java.awt.image.BufferedImage;

public class MetricService {

    public static double calculateNPCR(BufferedImage img1, BufferedImage img2) {
        int width = img1.getWidth();
        int height = img1.getHeight();
        int diffPixels = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel1 = img1.getRGB(x, y) & 0xFF;
                int pixel2 = img2.getRGB(x, y) & 0xFF;

                if (pixel1 != pixel2) diffPixels++;
            }
        }

        int totalPixels = width * height;
        return (diffPixels * 100.0) / totalPixels;
    }
}