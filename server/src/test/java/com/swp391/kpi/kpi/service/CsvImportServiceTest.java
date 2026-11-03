package com.swp391.kpi.kpi.service;

import com.swp391.kpi.admin.service.UserReadService;
import com.swp391.kpi.article.repository.ArticleRepository;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.kpi.dto.CsvImportResultResponse;
import com.swp391.kpi.kpi.entity.CsvImportLog;
import com.swp391.kpi.kpi.entity.ReaderInteraction;
import com.swp391.kpi.kpi.repository.CsvImportLogRepository;
import com.swp391.kpi.kpi.repository.ReaderInteractionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho CsvImportService (UC-63, BR-KPI-03).
 */
@ExtendWith(MockitoExtension.class)
class CsvImportServiceTest {

    @Mock
    private CsvImportLogRepository csvImportLogRepository;

    @Mock
    private ReaderInteractionRepository readerInteractionRepository;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private UserReadService userReadService;

    @InjectMocks
    private CsvImportService csvImportService;

    @Test
    void importCsv_whenEmptyFile_throwsBusinessException() {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", new byte[0]);
        BusinessException ex = assertThrows(BusinessException.class, () -> csvImportService.importCsv(file, 1));
        assertTrue(ex.getMessage().contains("Vui lòng chọn file CSV"));
    }

    @Test
    void importCsv_whenNotCsvExtension_throwsBusinessException() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "data".getBytes(StandardCharsets.UTF_8));
        BusinessException ex = assertThrows(BusinessException.class, () -> csvImportService.importCsv(file, 1));
        assertTrue(ex.getMessage().contains("Chỉ chấp nhận file có định dạng .csv"));
    }

    @Test
    void importCsv_whenMissingHeaders_throwsBusinessException() {
        String csvContent = "article_id,record_date,page_views\n1,2026-10-01,100\n";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        BusinessException ex = assertThrows(BusinessException.class, () -> csvImportService.importCsv(file, 1));
        assertTrue(ex.getMessage().contains("Định dạng CSV không hợp lệ"));
    }

    @Test
    void importCsv_whenValidData_insertsAndOverwritesCorrectly() {
        String csvContent = "article_id,record_date,page_views,rating_count,avg_rating\n"
                + "10,2026-10-01,500,10,4.5\n"   // Ban ghi moi
                + "10,2026-10-02,800,20,4.0\n"   // Ban ghi da ton tai (ghi de)
                + "99,2026-10-01,300,5,3.0\n";    // Bai viet 99 khong ton tai (bo qua)

        MockMultipartFile file = new MockMultipartFile("file", "readership.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        CsvImportLog savedLog = new CsvImportLog();
        savedLog.setId(1);
        when(csvImportLogRepository.save(any(CsvImportLog.class))).thenReturn(savedLog);

        when(articleRepository.existsById(10)).thenReturn(true);
        when(articleRepository.existsById(99)).thenReturn(false);

        // Dong 1: Chua ton tai
        when(readerInteractionRepository.findByArticleIdAndRecordDate(10, LocalDate.of(2026, 10, 1)))
                .thenReturn(Optional.empty());

        // Dong 2: Da ton tai -> ghi de (BR-KPI-03)
        ReaderInteraction existingInteraction = new ReaderInteraction();
        existingInteraction.setId(100);
        existingInteraction.setArticleId(10);
        existingInteraction.setRecordDate(LocalDate.of(2026, 10, 2));
        existingInteraction.setPageViews(100);
        existingInteraction.setRatingCount(5);
        existingInteraction.setRatingSum(20);
        when(readerInteractionRepository.findByArticleIdAndRecordDate(10, LocalDate.of(2026, 10, 2)))
                .thenReturn(Optional.of(existingInteraction));

        CsvImportResultResponse result = csvImportService.importCsv(file, 1);

        assertNotNull(result);
        assertEquals(3, result.getTotalRecords());
        assertEquals(2, result.getSuccessRecords());
        assertEquals(1, result.getInsertedRecords());
        assertEquals(1, result.getOverwrittenRecords());
        assertEquals(1, result.getSkippedRecords());
        assertFalse(result.getWarnings().isEmpty());
        assertTrue(result.getWarnings().get(0).contains("mã 99 không tồn tại"));

        verify(readerInteractionRepository, times(2)).save(any(ReaderInteraction.class));
    }
}
