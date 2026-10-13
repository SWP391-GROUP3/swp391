package com.swp391.kpi.service;

import com.swp391.kpi.dto.MediaFactorResponse;
import com.swp391.kpi.dto.MediaFactorUpdateRequest;
import com.swp391.kpi.entity.MediaFactor;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.repository.MediaFactorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kiem tra sua he so anh/video (UC-56), khong can DB.
 */
@ExtendWith(MockitoExtension.class)
class MediaFactorServiceTest {

    @Mock
    private MediaFactorRepository mediaFactorRepository;

    @InjectMocks
    private MediaFactorService mediaFactorService;

    private static MediaFactorUpdateRequest request(String factor) {
        MediaFactorUpdateRequest request = new MediaFactorUpdateRequest();
        request.setFactor(new BigDecimal(factor));
        return request;
    }

    @Test
    void updateFactorSavesNewFactor() {
        MediaFactor photo = new MediaFactor();
        photo.setId(1);
        photo.setMediaKind("PHOTO");
        photo.setFactor(new BigDecimal("1.00"));
        when(mediaFactorRepository.findById(1)).thenReturn(Optional.of(photo));
        when(mediaFactorRepository.save(photo)).thenReturn(photo);

        MediaFactorResponse response = mediaFactorService.updateFactor(1, request("1.50"));

        assertEquals("PHOTO", response.getMediaKind());
        assertEquals(0, new BigDecimal("1.50").compareTo(response.getFactor()));
    }

    @Test
    void updateFactorThrowsWhenIdNotFound() {
        when(mediaFactorRepository.findById(99)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> mediaFactorService.updateFactor(99, request("1.50")));

        assertEquals("Không tìm thấy loại ảnh/video có ID: 99", ex.getMessage());
        verify(mediaFactorRepository, never()).save(any());
    }
}
