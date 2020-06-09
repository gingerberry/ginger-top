package com.gingerberry.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

import java.awt.image.BufferedImage;
import java.awt.Image;
import java.awt.Graphics2D;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.imageio.ImageIO;

public class QRCodeGenerator {
    public QRCodeGenerator() {
    }

    public byte[] getQR(String text, int qrCodeDim, int qrCodeScaledDim) throws IOException, WriterException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, qrCodeDim, qrCodeDim);
        MatrixToImageWriter.writeToStream(bitMatrix, "png", stream);
        stream.flush();
        byte[] data = this.scale(stream.toByteArray(), qrCodeScaledDim, qrCodeScaledDim);

        return data;
    }

    private byte[] scale(byte[] fileData, int width, int height) throws IOException {
        InputStream in = new ByteArrayInputStream(fileData);
        BufferedImage image = ImageIO.read(in);

        Image tmp = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resized, "png", baos);
        baos.flush();
        byte[] imageInBytes = baos.toByteArray();
        baos.close();

        return imageInBytes;
    }
}
