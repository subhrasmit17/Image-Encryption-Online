package com.DivideAndConquer.image_encryption.service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;
import java.io.IOException;

public class ImageDecryptor{
    // Must match encryptor's rules exactly
    private static final String[] DNA_RULES = {
        "ACTG", "ACGT", "AGCT", "AGTC", "ATCG", "ATGC",
        "CAGT", "CATG", "CGAT", "CGTA", "CTAG", "CTGA",
        "GACT", "GATC", "GCAT", "GCTA", "GTAC", "GTCA",
        "TACG", "TAGC", "TCAG", "TCGA", "TGAC", "TGCA"
    };

    /*  main method is not required in web api
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("IMAGE DECRYPTION");
        
        try {
            // Get user inputs
            System.out.print("Encrypted image path: ");
            BufferedImage img = ImageIO.read(new File(sc.nextLine()));
            
            System.out.print("Output path: ");
            String outputPath = sc.nextLine();
            
            System.out.print("Decryption key: ");
            long key = sc.nextLong();

            // Decrypt the image
            processImage(img, key);
            
            // Save decrypted image
            ImageIO.write(img, "PNG", new File(outputPath));
            System.out.println("Decryption successful !!!");
            
        } catch (Exception e) {
            System.err.println("Decryption failed: " + e.getMessage());
            e.printStackTrace();
            System.err.println("Possible causes: Wrong key or corrupted image");
        } finally {
            sc.close();
        }
    }*/

    public byte[] decrypt(byte[] imageBytes, long key) throws IOException {
        // Convert byte[] to BufferedImage
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));

        // Decrypt image
        processImage(img, key);

        // Convert BufferedImage back to byte[]
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        return baos.toByteArray();
    }

    private static void processImage(BufferedImage img, long key) {
        int width = img.getWidth();
        int height = img.getHeight();
        int prevPixel = 0;

        // First process all pixels
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = img.getRGB(x, y);
                
                // Process each color channel
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                
                // Generate pixel-specific key
                long pixelKey = key ^ (x << 32) ^ (y << 16);

                // Decrypt in reverse order (B → G → R)
                b = inverseTransform(b, pixelKey + 2, g);
                g = inverseTransform(g, pixelKey + 1, r);
                r = inverseTransform(r, pixelKey, prevPixel);
                prevPixel = rgb & 0xFF; // Use original encrypted value

                // Update pixel
                img.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        
        manipulateBitPlanes(img, key);

        // Unscramble after pixel processing
        scrambleImage(img, key, false);
    }

    private static int inverseTransform(int pixel, long key, int prevPixel) {
        // 1. Reverse DNA substitution
        pixel = inverseDnaSubstitute(pixel, key);
        
        // 2. Reverse XOR with key byte
        pixel ^= (int)(key & 0xFF);
        
        // 3. Circular right shift by 1 bit
        pixel = ((pixel >> 1) | ((pixel & 1) << 7)) & 0xFF;
        
        // 4. Reverse diffusion (XOR)
        pixel ^= prevPixel;
        
        return pixel;
    }

    private static int inverseDnaSubstitute(int pixel, long key) {
        int ruleIdx = (int)((key >> 8) % DNA_RULES.length);
        String rule = DNA_RULES[ruleIdx];
    
        // Apply rotation to DNA rule (same as forward operation)
        int rotate = (int)(key & 0b11);
        rule = rule.substring(rotate) + rule.substring(0, rotate);
    
        // Extract the four 2-bit components
        int new0 = (pixel >> 6) & 0b11;  
        int new1 = (pixel >> 4) & 0b11;   
        int new2 = (pixel >> 2) & 0b11;   
        int new3 = pixel & 0b11;          
    
        int base3 = new3 ^ new0;         
        int base2 = new2 ^ base3;         
        int base1 = new1 ^ base2;         
        int base0 = new0 ^ base1;         
    
        // Reconstruct the DNA sequence
        StringBuilder dna = new StringBuilder();
        dna.append(rule.charAt(base0));
        dna.append(rule.charAt(base1));
        dna.append(rule.charAt(base2));
        dna.append(rule.charAt(base3));
    
        // Reverse the DNA string (undoing the forward operation)
        dna.reverse();
    
        // Convert back to binary pixel value
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result = (result << 2) | rule.indexOf(dna.charAt(i));
        }
    
        return result;
    }

    private static void scrambleImage(BufferedImage img, long seed, boolean scramble) {
        int width = img.getWidth();
        int height = img.getHeight();
        int totalPixels = width * height;
        
        int[] permutation = new int[totalPixels];
        for (int i = 0; i < totalPixels; i++) {
            permutation[i] = i;
        }
        
        Random rand = new Random(seed);
        for (int i = totalPixels - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            int temp = permutation[i];
            permutation[i] = permutation[j];
            permutation[j] = temp;
        }
        
        if (!scramble) {
            int[] inverse = new int[totalPixels];
            for (int i = 0; i < totalPixels; i++) {
                inverse[permutation[i]] = i;
            }
            permutation = inverse;
        }
        
        int[] pixels = img.getRGB(0, 0, width, height, null, 0, width);
        int[] scrambled = new int[totalPixels];
        for (int i = 0; i < totalPixels; i++) {
            scrambled[permutation[i]] = pixels[i];
        }
        img.setRGB(0, 0, width, height, scrambled, 0, width);
    }

    private static void manipulateBitPlanes(BufferedImage img, long key) {
        int width = img.getWidth();
        int height = img.getHeight();
        
        // Create bit planes for each channel
        int[][][] rPlanes = new int[8][height][width];
        int[][][] gPlanes = new int[8][height][width];
        int[][][] bPlanes = new int[8][height][width];
        
        // Extract bit planes
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                
                for (int bit = 0; bit < 8; bit++) {
                    rPlanes[bit][y][x] = (r >> bit) & 1;
                    gPlanes[bit][y][x] = (g >> bit) & 1;
                    bPlanes[bit][y][x] = (b >> bit) & 1;
                }
            }
        }
        
        // Seed RNG for consistent operations
        Random rand = new Random(key);
        
        // Apply different operations to each bit plane
        for (int bit = 0; bit < 8; bit++) {
            int operation = rand.nextInt(4);
            
            // Apply operation to all channels
            applyBitOperation(rPlanes[bit], operation);
            applyBitOperation(gPlanes[bit], operation);
            applyBitOperation(bPlanes[bit], operation);
        }
        
        // Recombine bit planes
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = 0, g = 0, b = 0;
                
                for (int bit = 0; bit < 8; bit++) {
                    r |= (rPlanes[bit][y][x] << bit);
                    g |= (gPlanes[bit][y][x] << bit);
                    b |= (bPlanes[bit][y][x] << bit);
                }
                
                img.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
    }

    private static void applyBitOperation(int[][] plane, int operation) {
        int height = plane.length;
        int width = plane[0].length;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                switch (operation) {
                    case 0: 
                        plane[y][x] ^= (x ^ y) & 1;
                        break;

                    case 1: 
                        plane[y][x] ^= 1;
                        break;

                    case 2: 
                        plane[y][x] ^= (x + y) % 2;
                        break;

                    case 3: 
                        plane[y][x] ^= (x * y) & 1;
                        break;
                }
            }
        }
    }
}
