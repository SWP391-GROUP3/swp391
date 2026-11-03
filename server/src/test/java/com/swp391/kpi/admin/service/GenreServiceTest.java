package com.swp391.kpi.admin.service;

import com.swp391.kpi.admin.dto.GenreResponse;
import com.swp391.kpi.admin.entity.Genre;
import com.swp391.kpi.admin.repository.GenreRepository;
import com.swp391.kpi.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Kiem tra doc the loai (he so chuan, danh sach dang dung), khong can DB.
 */
@ExtendWith(MockitoExtension.class)
class GenreServiceTest {

    @Mock
    private GenreRepository genreRepository;

    @InjectMocks
    private GenreService genreService;

    @Test
    void getBaseFactorReturnsFactorOfGenre() {
        Genre reportage = new Genre();
        reportage.setId(3);
        reportage.setBaseFactor(new BigDecimal("5.00"));
        when(genreRepository.findById(3)).thenReturn(Optional.of(reportage));

        BigDecimal factor = genreService.getBaseFactor(3);

        assertEquals(0, new BigDecimal("5.00").compareTo(factor));
    }

    @Test
    void getBaseFactorFailsWhenGenreDoesNotExist() {
        when(genreRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> genreService.getBaseFactor(99));
    }

    @Test
    void getActiveGenresReturnsIdCodeAndNameOfActiveGenres() {
        Genre inDepthNews = new Genre();
        inDepthNews.setId(2);
        inDepthNews.setCode("TIN_SAU");
        inDepthNews.setName("Tin sâu");
        inDepthNews.setBaseFactor(new BigDecimal("2.00"));
        when(genreRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(inDepthNews));

        List<GenreResponse> genres = genreService.getActiveGenres();

        assertEquals(1, genres.size());
        assertEquals(2, genres.get(0).getId());
        assertEquals("TIN_SAU", genres.get(0).getCode());
        assertEquals("Tin sâu", genres.get(0).getName());
    }
}
