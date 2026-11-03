package com.swp391.kpi.kpi.service;

import com.swp391.kpi.admin.service.UserReadService;
import com.swp391.kpi.article.repository.ArticleRepository;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.kpi.dto.CsvImportLogResponse;
import com.swp391.kpi.kpi.dto.CsvImportResultResponse;
import com.swp391.kpi.kpi.entity.CsvImportLog;
import com.swp391.kpi.kpi.entity.ReaderInteraction;
import com.swp391.kpi.kpi.repository.CsvImportLogRepository;
import com.swp391.kpi.kpi.repository.ReaderInteractionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Xu ly nghiep vu nhap so lieu tuong tac doc gia tu file CSV (UC-63, BF-05).
 * Tuan thu luat BR-KPI-03 (ghi de ban ghi trung article_id va record_date)
 * va luu nhat ky thuc thi vao bang csv_import_logs.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   51  field                           repository / service duoc inject
 *  ---- Ham public (controller / luong khac goi) ----
 *   62  importCsv()                     Nhap so lieu doc gia tu file CSV (UC-63, BR-KPI-03)
 *  239  getImportLogs()                 Lay danh sach nhat ky import theo thu tu thoi gian moi nhat giam dan
 *  259  getSampleCsvTemplate()          Mau CSV chuan de nguoi dung tai ve tham khao
 *  ---- Ham phu (private, chi dung trong file) ----
 *  269  normalizeHeader()               Bo ky tu BOM va khoang trang o dau/cuoi tieu de cot
 */
@Service
@RequiredArgsConstructor
public class CsvImportService {

    private final CsvImportLogRepository csvImportLogRepository;
    private final ReaderInteractionRepository readerInteractionRepository;
    private final ArticleRepository articleRepository;
    private final UserReadService userReadService;

    /**
     * Nhap so lieu doc gia tu file CSV (UC-63, BR-KPI-03).
     * Truong hop trung (article_id, record_date) se ghi de du lieu cu.
     * Bo qua cac dong co article_id khong ton tai trong he thong va ghi nhan canh bao.
     */
    @Transactional
    public CsvImportResultResponse importCsv(MultipartFile file, Integer currentUserId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Vui lòng chọn file CSV để tải lên.");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".csv")) {
            throw new BusinessException("Chỉ chấp nhận file có định dạng .csv.");
        }

        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Bo qua cac dong hoan toan trong
                if (!line.trim().isEmpty()) {
                    lines.add(line.trim());
                }
            }
        } catch (java.io.IOException e) {
            // Chi bat loi doc file; loi lap trinh khac de GlobalExceptionHandler xu ly, khong che di
            throw new BusinessException("Lỗi khi đọc file CSV: " + e.getMessage());
        }

        if (lines.isEmpty()) {
            throw new BusinessException("File CSV không có dữ liệu để nhập.");
        }

        // Kiem tra dong tieu de (Headers)
        String headerLine = lines.get(0);
        String[] headers = headerLine.split(",");
        Map<String, Integer> headerMap = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String colName = normalizeHeader(headers[i]);
            headerMap.put(colName.toLowerCase(), i);
        }

        String[] requiredHeaders = {"article_id", "record_date", "page_views", "rating_count", "avg_rating"};
        for (String req : requiredHeaders) {
            if (!headerMap.containsKey(req)) {
                throw new BusinessException("Định dạng CSV không hợp lệ. Các cột bắt buộc: article_id, record_date, page_views, rating_count, avg_rating.");
            }
        }

        if (lines.size() <= 1) {
            throw new BusinessException("File CSV chỉ có tiêu đề, không có dòng dữ liệu nào.");
        }

        // Tao truoc ban ghi log import de lay id gan cho reader_interactions
        CsvImportLog importLog = new CsvImportLog();
        importLog.setFileName(fileName);
        importLog.setTotalRecords(0);
        importLog.setSuccessRecords(0);
        importLog.setImportedBy(currentUserId);
        importLog.setImportedAt(LocalDateTime.now());
        importLog = csvImportLogRepository.save(importLog);

        int totalRecords = 0;
        int successRecords = 0;
        int insertedRecords = 0;
        int overwrittenRecords = 0;
        int skippedRecords = 0;
        List<String> warnings = new ArrayList<>();

        for (int rowIdx = 1; rowIdx < lines.size(); rowIdx++) {
            totalRecords++;
            String row = lines.get(rowIdx);
            String[] parts = row.split(",", -1);

            int articleCol = headerMap.get("article_id");
            int dateCol = headerMap.get("record_date");
            int viewsCol = headerMap.get("page_views");
            int countCol = headerMap.get("rating_count");
            int avgCol = headerMap.get("avg_rating");

            if (parts.length <= Math.max(Math.max(articleCol, dateCol), Math.max(Math.max(viewsCol, countCol), avgCol))) {
                warnings.add("Dòng " + (rowIdx + 1) + ": Thiếu cột dữ liệu, bỏ qua.");
                skippedRecords++;
                continue;
            }

            Integer articleId;
            LocalDate recordDate;
            int pageViews;
            int ratingCount;
            BigDecimal avgRating;

            try {
                articleId = Integer.valueOf(parts[articleCol].trim());
                recordDate = LocalDate.parse(parts[dateCol].trim());
                pageViews = Integer.parseInt(parts[viewsCol].trim());
                ratingCount = Integer.parseInt(parts[countCol].trim());
                avgRating = new BigDecimal(parts[avgCol].trim());
            } catch (DateTimeParseException | NumberFormatException ex) {
                warnings.add("Dòng " + (rowIdx + 1) + ": Dữ liệu không đúng định dạng số/ngày (YYYY-MM-DD), bỏ qua.");
                skippedRecords++;
                continue;
            }

            // Kiem tra gia tri hop le
            if (pageViews < 0 || ratingCount < 0) {
                warnings.add("Dòng " + (rowIdx + 1) + ": Lượt xem hoặc số lượt đánh giá không được âm, bỏ qua.");
                skippedRecords++;
                continue;
            }

            if (ratingCount > 0 && (avgRating.compareTo(BigDecimal.valueOf(1.0)) < 0 || avgRating.compareTo(BigDecimal.valueOf(5.0)) > 0)) {
                warnings.add("Dòng " + (rowIdx + 1) + ": Điểm đánh giá trung bình phải từ 1.0 đến 5.0 sao, bỏ qua.");
                skippedRecords++;
                continue;
            }

            // Kiem tra article_id ton tai trong he thong (UC-63 E2)
            if (!articleRepository.existsById(articleId)) {
                warnings.add("Dòng " + (rowIdx + 1) + ": Bài viết mã " + articleId + " không tồn tại trong hệ thống, bỏ qua.");
                skippedRecords++;
                continue;
            }

            // Tinh tong diem danh gia rating_sum de thoa man check constraint schema.sql:
            // rating_sum BETWEEN rating_count AND rating_count * 5 (khi rating_count = 0 thi rating_sum = 0)
            int ratingSum = 0;
            if (ratingCount > 0) {
                ratingSum = (int) Math.round(avgRating.doubleValue() * ratingCount);
                if (ratingSum < ratingCount) {
                    ratingSum = ratingCount;
                } else if (ratingSum > ratingCount * 5) {
                    ratingSum = ratingCount * 5;
                }
            }

            // Kiem tra trung (article_id, record_date) de ghi de (BR-KPI-03)
            Optional<ReaderInteraction> existingOpt = readerInteractionRepository.findByArticleIdAndRecordDate(articleId, recordDate);
            if (existingOpt.isPresent()) {
                ReaderInteraction existing = existingOpt.get();
                existing.setPageViews(pageViews);
                existing.setRatingCount(ratingCount);
                existing.setRatingSum(ratingSum);
                existing.setImportLogId(importLog.getId());
                readerInteractionRepository.save(existing);
                overwrittenRecords++;
            } else {
                ReaderInteraction interaction = new ReaderInteraction();
                interaction.setArticleId(articleId);
                interaction.setRecordDate(recordDate);
                interaction.setPageViews(pageViews);
                interaction.setRatingCount(ratingCount);
                interaction.setRatingSum(ratingSum);
                interaction.setImportLogId(importLog.getId());
                interaction.setCreatedAt(LocalDateTime.now());
                readerInteractionRepository.save(interaction);
                insertedRecords++;
            }
            successRecords++;
        }

        // Cap nhat lai so lieu thong ke cho ban ghi log import
        importLog.setTotalRecords(totalRecords);
        importLog.setSuccessRecords(successRecords);
        csvImportLogRepository.save(importLog);

        CsvImportResultResponse result = new CsvImportResultResponse();
        result.setLogId(importLog.getId());
        result.setFileName(fileName);
        result.setTotalRecords(totalRecords);
        result.setSuccessRecords(successRecords);
        result.setInsertedRecords(insertedRecords);
        result.setOverwrittenRecords(overwrittenRecords);
        result.setSkippedRecords(skippedRecords);
        result.setWarnings(warnings);

        return result;
    }

    /**
     * Lay danh sach nhat ky import theo thu tu thoi gian moi nhat giam dan.
     */
    @Transactional(readOnly = true)
    public Page<CsvImportLogResponse> getImportLogs(Pageable pageable) {
        // Lay thong tin user de map ten nguoi thuc hien
        // Ho ten nguoi nhap file; khong nuot loi im lang nua (loi doc user se bao ra ngoai)
        Map<Integer, String> userNames = userReadService.getUserNames();

        Page<CsvImportLog> logs = csvImportLogRepository.findAllByOrderByImportedAtDesc(pageable);
        return logs.map(log -> {
            String name = userNames.getOrDefault(log.getImportedBy(), "ID " + log.getImportedBy());
            return CsvImportLogResponse.from(log, name);
        });
    }

    /**
     * Mau CSV chuan de nguoi dung tai ve tham khao.
     */
    public String getSampleCsvTemplate() {
        return "article_id,record_date,page_views,rating_count,avg_rating\n"
                + "1,2026-10-01,1520,15,4.6\n"
                + "2,2026-10-01,840,8,4.2\n"
                + "3,2026-10-02,2300,30,4.8\n";
    }

    /**
     * Bo ky tu BOM va khoang trang o dau/cuoi tieu de cot.
     */
    private String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        String clean = header.trim();
        if (clean.startsWith("\uFEFF")) {
            clean = clean.substring(1).trim();
        }
        return clean;
    }
}
