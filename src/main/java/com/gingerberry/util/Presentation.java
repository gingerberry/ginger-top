package com.gingerberry.util;

import java.util.List;
import java.util.regex.Pattern;

import java.io.InputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileOutputStream;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.PreparedStatement;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.AmazonServiceException;

public class Presentation {
    private QRCodeGenerator qrGenerator;
    private XMLSlideShow ppt;
    private String name;
    private int id;

    public Presentation(QRCodeGenerator qrGenerator, InputStream fileContent, String fileName) throws Exception {
        this.qrGenerator = qrGenerator;

        this.checkExtension(fileName);
        this.name = fileName;

        this.ppt = this.fileToPPT(fileContent);
    }

    public void addQRCodesToPPT(int qrCodeDimension, int qrCodeScaledDimension) throws Exception {
        List<XSLFSlide> slides = ppt.getSlides();

        if (this.id == 0) {
            throw new Exception("The presentation hasn't been inserted into the DB.");
        }

        for (int i = 0; i < slides.size(); i++) {
            String qrCodeData = String.valueOf(i);
            byte[] pictureData = this.qrGenerator.getQR(qrCodeData, qrCodeDimension, qrCodeScaledDimension);

            // Adding the image to the presentation.
            XSLFPictureData idx = ppt.addPicture(pictureData, XSLFPictureData.PictureType.PNG);
            XSLFPictureShape shape = slides.get(i).createPicture(idx);
            shape.setAnchor(new Rectangle(ppt.getPageSize().width - qrCodeDimension,
                    ppt.getPageSize().height - qrCodeDimension, qrCodeDimension, qrCodeDimension));
            shape.setAnchor(new Rectangle(0, 0, qrCodeScaledDimension, qrCodeScaledDimension));
        }
    }

    public void uploadPPTAsImagesToS3(String imagePrefix)
            throws FileNotFoundException, IOException, InterruptedException {
        List<XSLFSlide> slides = ppt.getSlides();

        AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).build();

        for (int i = 0; i < slides.size(); i++) {
            BufferedImage img = this.getSlideImage(ppt, slides.get(i));

            String fileName = i + ".png";
            String keyName = "presentation/" + id + "/" + fileName;
            String imageName = imagePrefix + fileName;

            this.saveSlideImageAsPNG(ppt, imageName, img);

            s3.putObject("gingerberry", keyName, new File(imageName));

            File file = new File(imageName);
            file.delete();
        }
    }

    public void uploadPPTToS3(String fileName) throws IOException, AmazonServiceException {
        String extension = this.extractExtension(fileName);
        String fileDest = id + "." + extension;
        String keyName = "presentation/" + id + "/" + fileDest;

        FileOutputStream out = new FileOutputStream(fileDest);
        ppt.write(out);
        out.close();

        AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
        s3.putObject("gingerberry", keyName, new File(fileDest));

        File file = new File(fileDest);
        file.delete();
    }

    public void checkExtension(String fileName) throws Exception {
        String extension = this.extractExtension(fileName);

        if (!extension.equals("ppt") && !extension.equals("pptx")) {
            throw new Exception(
                    "Only .ppt and .pptx files are supported! Extension is " + extension + " filename " + fileName);
        }
    }

    public void insertIntoDB(Connection dbConn) throws Exception {
        this.insertPresentationsIntoDB(dbConn);
        this.insertPresentationSlides(dbConn, this.id);
    }

    private void insertPresentationsIntoDB(Connection dbConn) throws Exception {
        String query = "INSERT INTO presentations (presentation_name) VALUES (?)";
        PreparedStatement stmt = dbConn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

        stmt.setString(1, this.name);

        stmt.executeUpdate();
        ResultSet generatedKeys = stmt.getGeneratedKeys();

        if (!generatedKeys.next()) {
            throw new Exception("Error inserting!");
        }

        this.id = (int) generatedKeys.getLong(1);
    }

    private void insertPresentationSlides(Connection dbConn, int presentationID) throws Exception {
        String query = "INSERT INTO slides (presentation_id, title) VALUES (?, ?)";
        PreparedStatement stmt = dbConn.prepareStatement(query);

        List<XSLFSlide> slides = ppt.getSlides();

        for (int i = 0; i < slides.size(); i++) {
            stmt.setInt(1, presentationID);

            String title = slides.get(i).getTitle();

            if (title == null) {
                title = "Слайд #" + i;
            }

            stmt.setString(2, title);

            stmt.executeUpdate();
        }
    }

    public int getID() {
        return id;
    }

    private XMLSlideShow fileToPPT(InputStream file) throws IOException {
        return new XMLSlideShow(file);
    }

    private BufferedImage getSlideImage(XMLSlideShow ppt, XSLFSlide slide) {
        Dimension pgsize = ppt.getPageSize();
        BufferedImage img = new BufferedImage(pgsize.width, pgsize.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = img.createGraphics();

        // clear the drawing area
        graphics.setPaint(Color.white);
        graphics.fill(new Rectangle2D.Float(0, 0, pgsize.width, pgsize.height));

        // render
        slide.draw(graphics);

        return img;
    }

    private void saveSlideImageAsPNG(XMLSlideShow ppt, String name, BufferedImage img) throws IOException {
        FileOutputStream out = new FileOutputStream(name);
        ImageIO.write(img, "png", out);
        ppt.write(out);

        System.out.println("Image " + name + "successfully created");
        out.close();
    }

    private String extractExtension(String fileName) {
        String[] parts = fileName.split(Pattern.quote("."));
        return parts[parts.length - 1];
    }
}