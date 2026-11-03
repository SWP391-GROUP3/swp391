package com.swp391.kpi.kpi.dto;

import com.swp391.kpi.kpi.entity.CsvImportLog;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO phan hoi nhat ky import CSV so lieu doc gia (UC-63).
 * Map tay bang ham from() theo quy uoc AGENTS.md muc 3.4.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   21  field                           cac truong tra ve cho giao dien
 *   29  from()                          doi entity sang DTO (map tay, AGENTS 3.4)
 */
@Getter
@Setter
public class CsvImportLogResponse {

    private Integer id;
    private String fileName;
    private Integer totalRecords;
    private Integer successRecords;
    private Integer importedBy;
    private String importedByName;
    private LocalDateTime importedAt;

    public static CsvImportLogResponse from(CsvImportLog entity, String importedByName) {
        if (entity == null) {
            return null;
        }
        CsvImportLogResponse res = new CsvImportLogResponse();
        res.setId(entity.getId());
        res.setFileName(entity.getFileName());
        res.setTotalRecords(entity.getTotalRecords());
        res.setSuccessRecords(entity.getSuccessRecords());
        res.setImportedBy(entity.getImportedBy());
        res.setImportedByName(importedByName);
        res.setImportedAt(entity.getImportedAt());
        return res;
    }
}
