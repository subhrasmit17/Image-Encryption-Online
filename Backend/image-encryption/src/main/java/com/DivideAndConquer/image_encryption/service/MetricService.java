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

    public static double calculateUACI(BufferedImage original, BufferedImage encrypted) {
        if (original.getWidth() != encrypted.getWidth() ||
                original.getHeight() != encrypted.getHeight()) {
            throw new IllegalArgumentException("Image dimensions must match");
        }

        double totalDiff = 0;
        int width = original.getWidth();
        int height = original.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int origRGB = original.getRGB(x, y);
                int encRGB = encrypted.getRGB(x, y);

                totalDiff += Math.abs(((origRGB >> 16) & 0xFF) - ((encRGB >> 16) & 0xFF)) / 255.0;
                totalDiff += Math.abs(((origRGB >> 8) & 0xFF) - ((encRGB >> 8) & 0xFF)) / 255.0;
                totalDiff += Math.abs((origRGB & 0xFF) - (encRGB & 0xFF)) / 255.0;
            }
        }

        return (totalDiff / (width * height * 3)) * 100;
    }

}