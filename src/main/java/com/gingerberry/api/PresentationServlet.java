package com.gingerberry.api;

import com.gingerberry.util.Presentation;
import com.gingerberry.util.QRCodeGenerator;
import com.gingerberry.api.model.response.PresentationUploadResponse;
import com.gingerberry.db.DB;

import java.sql.Connection;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.Part;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

@WebServlet("/upload")
@MultipartConfig
public class PresentationServlet extends HttpServlet {
    private static final long serialVersionUID = -4751096228274971485L;
    private static final int QR_CODE_DIMENSION = 70;
    private static final int QR_CODE_SCALED_DIMENSION = 70;

    private QRCodeGenerator qrGenerator;
    private Connection dbConn;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Part filePart = request.getPart("presentation");
        InputStream fileContent = filePart.getInputStream();

        try {
            String partName = this.getFileName(filePart);
            Presentation ppt = new Presentation(this.qrGenerator, fileContent, partName);

            ppt.insertIntoDB(dbConn);

            ppt.addQRCodesToPPT(QR_CODE_DIMENSION, QR_CODE_SCALED_DIMENSION);
            ppt.uploadPPTAsImagesToS3(partName);
            ppt.uploadPPTToS3(partName);

            response.addHeader("Access-Control-Allow-Origin", "*");
            response.addHeader("Access-Control-Allow-Credentials", "true");

            PresentationUploadResponse uploadResp = new PresentationUploadResponse(ppt.getID());
            Gson gson = new Gson();

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            response.getWriter().println(gson.toJson(uploadResp));
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Error: " + ex.getMessage());
        } finally {
            fileContent.close();
        }
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE, HEAD");
        response.addHeader("Access-Control-Allow-Credentials", "true");
        response.addHeader("Access-Control-Allow-Headers",
                "Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");
        response.addHeader("Access-Control-Max-Age", "1728000");
    }

    @Override
    public void init() throws ServletException {
        try {
            this.qrGenerator = new QRCodeGenerator();

            DB db = DB.getInstance();
            this.dbConn = db.getConnection();
        } catch (Exception ex) {
            throw new ServletException(ex.getMessage());
        }
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
}
