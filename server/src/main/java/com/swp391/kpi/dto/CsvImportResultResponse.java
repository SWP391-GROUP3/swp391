package com.swp391.kpi.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO phan hoi ket qua sau khi thuc hien import file CSV (UC-63).
 *
 * MUC LUC (so dong)
 *   18  field                        - cac truong thong ke va danh sach canh bao/loi
 */
@Getter
@Setter
public class CsvImportResultResponse {

    private Integer logId;
    private String fileName;
    private int totalRecords;
    private int successRecords;
    private int insertedRecords;
    private int overwrittenRecords;
    private int skippedRecords;
    private List<String> warnings = new ArrayList<>();
}
