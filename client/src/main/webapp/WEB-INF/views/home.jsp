<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%-- LUU Y: JSTL ban jakarta dung URI moi. Khai bao kieu cu
     http://java.sun.com/jsp/jstl/core se im lang khong chay. --%>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Hệ thống KPI &amp; Nhuận bút</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
</head>
<body>
<div class="container">
    <h1>Hệ thống đánh giá chất lượng &amp; quản lý nhuận bút</h1>
    <p class="sub">SWP391 &mdash; mô phỏng khâu hậu kỳ toà soạn</p>

    <c:choose>
        <c:when test="${not empty loi}">
            <div class="box box-loi">
                <strong>Chưa kết nối được server</strong>
                <p>${loi}</p>
                <p class="hint">
                    Kiểm tra: server đã chạy ở cổng 8080 chưa
                    (<code>mvn spring-boot:run</code> trong thư mục <code>server</code>).
                </p>
            </div>
        </c:when>
        <c:otherwise>
            <div class="box box-ok">
                <strong>Đã kết nối server</strong>
                <p>${trangThaiServer}</p>
                <p class="hint">Thời gian server: ${thoiGianServer}</p>
            </div>
        </c:otherwise>
    </c:choose>

    <h2>Việc tiếp theo</h2>
    <ul>
        <li>Thống nhất bảng endpoint trong <code>README.md</code></li>
        <li>Làm màn hình đăng nhập, lưu JWT vào <code>HttpSession</code></li>
        <li>Danh sách tác phẩm, form nộp bài</li>
        <li>Màn hình chấm điểm cho thư ký toà soạn</li>
        <li>Bảng kê nhuận bút theo kỳ</li>
    </ul>
</div>
</body>
</html>
