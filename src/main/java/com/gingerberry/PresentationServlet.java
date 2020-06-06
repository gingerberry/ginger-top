package com.gingerberry;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.Part;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;

@WebServlet("/upload")
@MultipartConfig
public class PresentationServlet extends HttpServlet {
    private static final long serialVersionUID = -4751096228274971485L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Part filePart = request.getPart("presentation");
        InputStream fileContent = filePart.getInputStream();

        try {
            XMLSlideShow ppt = this.editPPT(fileContent);
            this.uploadPPT(ppt, "venko-e-pedal");

            response.getWriter().println("Edited your presentation!");
        } catch (Exception ex) {
            response.getWriter().println("We failed!" + ex.getMessage());
        } finally {
            fileContent.close();
        }
    }

    @Override
    public void init() throws ServletException {
        System.out.println("Servlet " + this.getServletName() + " has started");
    }

    @Override
    public void destroy() {
        System.out.println("Servlet " + this.getServletName() + " has stopped");
    }

    private XMLSlideShow editPPT(InputStream file) throws FileNotFoundException, IOException {
        XMLSlideShow ppt = new XMLSlideShow(file);

        return ppt;
    }

    private void uploadPPT(XMLSlideShow ppt, String fileName) throws IOException {
        FileOutputStream out = new FileOutputStream(fileName);
        ppt.write(out);
        out.close();
    }
}
