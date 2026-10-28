package com.swp391.kpi.service;

import com.swp391.kpi.dto.GenreResponse;
import com.swp391.kpi.entity.Genre;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Doc the loai bai (luong admin). Luong khac goi service nay,
 * khong goi thang GenreRepository (AGENTS.md muc 2).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   28  field                           repository / service duoc inject
 *   33  getBaseFactor()                 He so chuan cua 1 the loai
 *   41  getActiveGenres()               Cac the loai dang dung cho o chon the loai (vd man soan bai UC-03 cua BF-01)
 */
@Service
@RequiredArgsConstructor
public class GenreService {

    private final GenreRepository genreRepository;

    // He so chuan cua 1 the loai. BF-04 dung khi cham nhuan but:
    // he so = baseFactor x diem / 100 (BR-RY-01, RoyaltyCalculator.suggestedFactor)
    @Transactional(readOnly = true)
    public BigDecimal getBaseFactor(Integer genreId) {
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thể loại có ID: " + genreId));
        return genre.getBaseFactor();
    }

    // Cac the loai dang dung cho o chon the loai (vd man soan bai UC-03 cua BF-01)
    @Transactional(readOnly = true)
    public List<GenreResponse> getActiveGenres() {
        List<GenreResponse> result = new ArrayList<>();
        for (Genre genre : genreRepository.findByIsActiveTrueOrderByNameAsc()) {
            result.add(GenreResponse.from(genre));
        }
        return result;
    }
}
