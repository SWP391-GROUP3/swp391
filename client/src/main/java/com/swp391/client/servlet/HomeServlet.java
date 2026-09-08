package com.swp391.client.servlet;

import com.fasterxml.jackson.databind.JsonNode;
import com.swp391.client.util.ApiClient;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Trang chu. Goi thu /api/ping de xac nhan client noi duoc toi server.
 *
 * <p>Mau cho cac servlet khac: nhan request -> goi API -> dat ket qua vao
 * request attribute -> forward sang JSP. Khong viet HTML trong servlet.
 */
// Khong map vao "/" - pattern do se nuot ca request file tinh (CSS, anh)
// vi no ghi de default servlet cua Tomcat. Trang goc dung index.jsp de chuyen huong.
@WebServlet(name = "HomeServlet", urlPatterns = {"/home"})
public class HomeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            JsonNode ketQua = ApiClient.get("/api/ping");
            req.setAttribute("trangThaiServer", ketQua.path("thongDiep").asText());
            req.setAttribute("thoiGianServer", ketQua.path("thoiGian").asText());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            req.setAttribute("loi", "Khong ket noi duoc toi server: " + e.getMessage());
        }

        req.getRequestDispatcher("/WEB-INF/views/home.jsp").forward(req, resp);
    }
}
