package com.swp391.kpi.service;

import com.swp391.kpi.entity.ReviewerDeviationIndex;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class ReviewerDeviationCalculator {
    public ReviewerDeviationIndex calculate(List<BigDecimal> differences, int appealCount,
                                             int overturnedCount, int workloadCount) {
        if (differences.isEmpty()) throw new IllegalArgumentException("Không có dữ liệu so sánh.");
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal difference : differences) sum = sum.add(difference.abs());
        var result = new ReviewerDeviationIndex();
        result.setAvgDeviation(sum.divide(BigDecimal.valueOf(differences.size()), 2, RoundingMode.HALF_UP));
        result.setAppealCount(appealCount);
        result.setOverturnRate(appealCount == 0 ? new BigDecimal("0.00")
                : BigDecimal.valueOf(overturnedCount).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(appealCount), 2, RoundingMode.HALF_UP));
        result.setWorkloadCount(workloadCount);
        return result;
    }
}
