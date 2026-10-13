package com.swp391.kpi.service;

import com.swp391.kpi.dto.RoyaltyRateCreateRequest;
import com.swp391.kpi.dto.RoyaltyRateResponse;
import com.swp391.kpi.entity.RoyaltyRate;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.repository.RoyaltyRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Quan ly don gia nhuan but (UC-55, bang royalty_rates).
 * Chi co XEM va THEM: khong co sua/xoa, vi don gia cu phai giu nguyen
 * de bai dang truoc do van tinh theo gia cu (BR-RY-03).
 */
@Service
@RequiredArgsConstructor
public class RoyaltyRateService {

    private static final String ROYALTY_FLOW = "BF-04";  // gia tri cot system_parameters.flow cua luong nhuan but
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final RoyaltyRateRepository royaltyRateRepository;
    private final SystemParameterService systemParameterService;

    // Lich su don gia, moi nhat len dau
    @Transactional(readOnly = true)
    public List<RoyaltyRateResponse> getAllRates() {
        List<RoyaltyRateResponse> result = new ArrayList<>();
        for (RoyaltyRate rate : royaltyRateRepository.findAllByOrderByEffectiveFromDesc()) {
            result.add(RoyaltyRateResponse.from(rate));
        }
        return result;
    }

    // Them don gia moi: Ke toan nhap luong co so + ngay hieu luc, he thong tu tinh don gia
    @Transactional
    public RoyaltyRateResponse createRate(RoyaltyRateCreateRequest request, Integer currentUserId) {
        // Ngay hieu luc phai SAU don gia moi nhat: khong chen vao giua lich su (nhom da chot)
        Optional<RoyaltyRate> latest = royaltyRateRepository.findFirstByOrderByEffectiveFromDesc();
        if (latest.isPresent() && !request.getEffectiveFrom().isAfter(latest.get().getEffectiveFrom())) {
            throw new BusinessException("Ngày hiệu lực phải sau ngày "
                    + latest.get().getEffectiveFrom().format(DATE_FORMAT) + " của đơn giá mới nhất.");
        }

        // Don gia = luong co so x UNIT_PRICE_PERCENT (lay tu system_parameters)
        RoyaltyCalculator calculator = new RoyaltyCalculator(systemParameterService.getValuesByFlow(ROYALTY_FLOW));
        BigDecimal amount = calculator.unitPrice(request.getBaseSalary());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Lương cơ sở quá nhỏ, đơn giá tính ra bằng 0 đồng.");
        }

        RoyaltyRate rate = new RoyaltyRate();
        rate.setBaseSalary(request.getBaseSalary());
        rate.setAmount(amount);
        rate.setEffectiveFrom(request.getEffectiveFrom());
        rate.setCreatedBy(currentUserId);

        RoyaltyRate saved = royaltyRateRepository.save(rate);
        return RoyaltyRateResponse.from(saved);
    }
}
