package com.swp391.kpi.service;

import com.swp391.kpi.entity.Genre;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.repository.GenreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Kiem tra lay he so chuan cua the loai, khong can DB.
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
}
