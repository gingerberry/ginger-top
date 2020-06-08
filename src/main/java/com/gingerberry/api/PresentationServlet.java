package com.gingerberry.api;

import java.util.List;
import java.util.regex.Pattern;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.File;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.Part;

import javax.imageio.ImageIO;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;

import com.google.zxing.WriterException;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.gingerberry.qr.QRCodeGenerator;

@WebServlet("/upload")
@MultipartConfig
public class PresentationServlet extends HttpServlet {
    private static final long serialVersionUID = -4751096228274971485L;
    private static final int QR_CODE_DIMENSION = 250;
    private static final int QR_CODE_SCALED_DIMENSION = 70;

    private QRCodeGenerator qrGenerator;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Part filePart = request.getPart("presentation");
        InputStream fileContent = filePart.getInputStream();

        try {
            XMLSlideShow ppt = this.fileToPPT(fileContent);
            String partName = this.getFileName(filePart);
            int id = 1;

            this.checkExtension(partName);

            this.addQRCodesToPPT(QR_CODE_DIMENSION, QR_CODE_SCALED_DIMENSION, ppt);

            this.uploadPPTAsImagesToS3(ppt, partName, id);
            this.uploadPPTToS3(ppt, partName, id);

            response.getWriter().println("Edited your presentation!");
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            response.getWriter().println("We failed!" + sw);
        } finally {
            fileContent.close();
        }
    }

    @Override
    public void init() throws ServletException {
        this.qrGenerator = new QRCodeGenerator();
    }

    @Override
    public void destroy() {
        System.out.println("Servlet " + this.getServletName() + " has stopped");
    }

    private String getFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        System.out.println("content-disposition header= " + contentDisp);
        String[] tokens = contentDisp.split(";");
        for (String token : tokens) {
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf("=") + 2, token.length() - 1);
            }
        }
        return "";
    }

    private XMLSlideShow fileToPPT(InputStream file) throws IOException {
        return new XMLSlideShow(file);
    }

    private void addQRCodesToPPT(int qrCodeDimension, int qrCodeScaledDimension, XMLSlideShow ppt)
            throws FileNotFoundException, IOException, InterruptedException, WriterException {
        List<XSLFSlide> slides = ppt.getSlides();

        for (int i = 0; i < slides.size(); i++) {
            String qrCodeData = "venko-e-aljirski-zatvornik-" + i;
            byte[] pictureData = this.qrGenerator.getQR(qrCodeData, qrCodeDimension, qrCodeScaledDimension);

            // Adding the image to the presentation.
            XSLFPictureData idx = ppt.addPicture(pictureData, XSLFPictureData.PictureType.PNG);
            XSLFPictureShape shape = slides.get(i).createPicture(idx);
            shape.setAnchor(new Rectangle(ppt.getPageSize().width - qrCodeDimension,
                    ppt.getPageSize().height - qrCodeDimension, qrCodeDimension, qrCodeDimension));
            shape.setAnchor(new Rectangle(0, 0, qrCodeScaledDimension, qrCodeScaledDimension));
        }
    }

    private void uploadPPTAsImagesToS3(XMLSlideShow ppt, String imagePrefix, int id)
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

    private void uploadPPTToS3(XMLSlideShow ppt, String fileName, int id) throws IOException, AmazonServiceException {
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

    private boolean checkExtension(String fileName) {
        String extention = this.extractExtension(fileName);

        if (extention == "ppt" || extention == "pptx") {
            return true;
        }
        return false;
    }

    private String extractExtension(String fileName) {
        String[] parts = fileName.split(Pattern.quote("."));
        return parts[parts.length - 1];
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
}
