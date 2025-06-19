package com.DivideAndConquer.image_encryption.service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;
import java.io.IOException;

public class ImageEncryptor {
    // All 24 permutations of ACTG (4! = 24)
    private static final String[] DNA_RULES = {
        "ACTG", "ACGT", "AGCT", "AGTC", "ATCG", "ATGC",
        "CAGT", "CATG", "CGAT", "CGTA", "CTAG", "CTGA",
        "GACT", "GATC", "GCAT", "GCTA", "GTAC", "GTCA",
        "TACG", "TAGC", "TCAG", "TCGA", "TGAC", "TGCA"
    };

    /*  main method is not required in web api
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("IMAGE ENCRYPTION");
        
        try {
            // Get user inputs
            System.out.print("Input image path: ");
            BufferedImage img = ImageIO.read(new File(sc.nextLine()));
            
            System.out.print("Output path: ");
            String outputPath = sc.nextLine();
            
            System.out.print("Encryption key: ");
            long key = sc.nextLong();

            // Encrypt the image
            encryptImage(img, key);
            
            // Save encrypted image
            ImageIO.write(img, "PNG", new File(outputPath));
            System.out.println("Encryption successful !!!");
            
        } catch (Exception e) {
            System.err.println("Encryption failed: " + e.getMessage());
        } finally {
            sc.close();
        }
    }*/

    //to give output in bytes instead of BufferedImage, for the web api
    public byte[] encrypt(byte[] imageBytes, long key) throws IOException {
        // Convert byte[] to BufferedImage
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));

        // Encrypt image
        encryptImage(img, key);

        // Convert BufferedImage back to byte[]
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        return baos.toByteArray();
    }

    private static void encryptImage(BufferedImage img, long key) {
        // Step 1: Scramble the image
        scrambleImage(img, key);

        // Step 2: Apply bit-plane slicing and manipulation
        manipulateBitPlanes(img, key);

        int width = img.getWidth();
        int height = img.getHeight();
        int prevPixel = 0;

        // Step 3: Encrypt each pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = img.getRGB(x, y);
                
                // Split RGB channels
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Generate pixel-specific key
                long pixelKey = key ^ (x << 32) ^ (y << 16);

                // Encrypt each channel
                r = transformPixel(r, pixelKey, prevPixel);
                g = transformPixel(g, pixelKey + 1, r);
                b = transformPixel(b, pixelKey + 2, g);
                prevPixel = b;

                // Set encrypted pixel
                img.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
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

    private static int transformPixel(int pixel, long key, int prevPixel) {
        // 1. XOR with previous pixel for diffusion
        pixel ^= prevPixel;

        // 2. Circular left shift by 1 bit
        pixel = ((pixel << 1) | (pixel >> 7)) & 0xFF;

        // 3. XOR with key byte
        pixel ^= (int)(key & 0xFF);

        // 4. DNA substitution using selected rule
        return dnaSubstitute(pixel, key);
    }

    private static int dnaSubstitute(int pixel, long key) {
        int ruleIdx = (int)((key >> 8) % DNA_RULES.length);
        String rule = DNA_RULES[ruleIdx];

        // Apply rotation to DNA rule
        int rotate = (int)(key & 0b11);
        rule = rule.substring(rotate) + rule.substring(0, rotate);

        // Convert pixel to DNA bases
        StringBuilder dna = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            int bits = (pixel >> (6 - 2 * i)) & 0b11;
            dna.append(rule.charAt(bits));
        }

        // Reverse the DNA sequence
        dna.reverse();
    
        // Get the DNA bases after reversal
        int[] baseIndices = new int[4];
        for (int i = 0; i < 4; i++) {
            baseIndices[i] = rule.indexOf(dna.charAt(i));
        }


        int new0 = baseIndices[0] ^ baseIndices[1]; 
        int new1 = baseIndices[1] ^ baseIndices[2]; 
        int new2 = baseIndices[2] ^ baseIndices[3]; 
        int new3 = baseIndices[3] ^ new0;           

    
        int result = (new0 << 6) | (new1 << 4) | (new2 << 2) | new3;
        return result;
    }

    private static void scrambleImage(BufferedImage img, long seed) {
        int width = img.getWidth();
        int height = img.getHeight();
        int totalPixels = width * height;

        // Generate initial pixel index array
        int[] permutation = new int[totalPixels];
        for (int i = 0; i < totalPixels; i++) {
            permutation[i] = i;
        }

        // Shuffle indices using seeded RNG
        Random rand = new Random(seed);
        for (int i = totalPixels - 1; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            int temp = permutation[i];
            permutation[i] = permutation[j];
            permutation[j] = temp;
        }

        // Apply pixel scrambling
        int[] pixels = img.getRGB(0, 0, width, height, null, 0, width);
        int[] scrambled = new int[totalPixels];
        for (int i = 0; i < totalPixels; i++) {
            scrambled[permutation[i]] = pixels[i];
        }
        img.setRGB(0, 0, width, height, scrambled, 0, width);
    }
}