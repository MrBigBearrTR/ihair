package com.bigbear.ihair.service;

import com.bigbear.ihair.exception.BadRequestException;
import com.bigbear.ihair.exception.PayloadTooLargeException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Iterator;

@Component
public class PngLogoValidator {
    public static final long MAX_BYTES = 1024L * 1024L;
    public static final int MAX_DIMENSION = 4096;
    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    public ValidatedPng validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Logo PNG dosyası zorunludur.");
        }
        if (!"image/png".equalsIgnoreCase(file.getContentType())) {
            throw new BadRequestException("Logo yalnızca image/png içerik türünde olmalıdır.");
        }
        if (file.getSize() > MAX_BYTES) throw tooLarge();
        byte[] original = read(file);
        if (original.length > MAX_BYTES) throw tooLarge();
        requirePngSignature(original);

        BufferedImage decoded = decodePng(original);
        if (!decoded.getColorModel().hasAlpha()) {
            throw new BadRequestException("Logo şeffaflık kanalı içeren bir PNG olmalıdır.");
        }
        if (!containsTransparentPixel(decoded)) {
            throw new BadRequestException("Logo en az bir saydam piksel içermelidir.");
        }
        byte[] canonical = canonicalize(decoded);
        if (canonical.length > MAX_BYTES) throw tooLarge();
        return new ValidatedPng(canonical, decoded.getWidth(), decoded.getHeight(), sha256(canonical));
    }

    private byte[] read(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new BadRequestException("Logo dosyası okunamadı.");
        }
    }

    private void requirePngSignature(byte[] data) {
        if (data.length < PNG_SIGNATURE.length) throw invalidPng();
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (data[i] != PNG_SIGNATURE[i]) throw invalidPng();
        }
    }

    private BufferedImage decodePng(byte[] data) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            if (input == null) throw invalidPng();
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalidPng();
            ImageReader reader = readers.next();
            try {
                if (!"png".equalsIgnoreCase(reader.getFormatName())) throw invalidPng();
                reader.setInput(input, true, false);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > MAX_DIMENSION || height > MAX_DIMENSION) {
                    throw new BadRequestException(
                            "Logo boyutları 1x1 ile 4096x4096 piksel arasında olmalıdır.");
                }
                BufferedImage image = reader.read(0);
                if (image == null) throw invalidPng();
                return image;
            } finally {
                reader.dispose();
            }
        } catch (BadRequestException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw invalidPng();
        }
    }

    private boolean containsTransparentPixel(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) < 255) return true;
            }
        }
        return false;
    }

    private byte[] canonicalize(BufferedImage image) {
        BufferedImage canonical =
                new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canonical.createGraphics();
        try {
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(canonical, "png", output)) throw invalidPng();
            return output.toByteArray();
        } catch (IOException ex) {
            throw new BadRequestException("Logo PNG olarak işlenemedi.");
        }
    }

    private String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algoritması kullanılamıyor.", ex);
        }
    }

    private BadRequestException invalidPng() {
        return new BadRequestException("Logo geçerli ve okunabilir bir PNG dosyası olmalıdır.");
    }

    private PayloadTooLargeException tooLarge() {
        return new PayloadTooLargeException("Logo dosyası en fazla 1 MB olabilir.");
    }
}
