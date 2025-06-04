package com.example.matchup.matchupbackend.service;

import com.example.matchup.matchupbackend.dto.request.alert.AlertFilterRequest;
import com.example.matchup.matchupbackend.dto.response.alert.SliceAlertResponse;
import com.example.matchup.matchupbackend.entity.Alert;
import com.example.matchup.matchupbackend.entity.AlertType;
import com.example.matchup.matchupbackend.error.exception.InvalidValueEx.AlertDeletedException;
import com.example.matchup.matchupbackend.error.exception.ResourceNotFoundEx.AlertNotFoundException;
import com.example.matchup.matchupbackend.repository.alert.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class AlertServiceTest {

    private AlertService alertService;
    private AlertRepository alertRepository;

    @BeforeEach
    void setUp() {
        alertRepository = mock(AlertRepository.class);
        alertService = new AlertService(alertRepository);
    }

    @Test
    void testGetSliceAlertResponse() {
        Long userId = 1L;
        AlertFilterRequest alertFilterRequest = new AlertFilterRequest();
        Pageable pageable = PageRequest.of(0, 10);
        Alert alert = mock(Alert.class);

        when(alert.getAlertType()).thenReturn(AlertType.PROJECT);
        when(alert.getId()).thenReturn(1L);
        when(alert.getContent()).thenReturn("Test Alert");
        when(alert.isDeleted()).thenReturn(false);

        when(alertRepository.findAlertSliceByAlertRequest(userId, alertFilterRequest, pageable))
                .thenReturn(new SliceImpl<>(List.of(alert), pageable, true));

        SliceAlertResponse response = alertService.getSliceAlertResponse(userId, alertFilterRequest, pageable);

        assertNotNull(response);
        assertEquals(10, response.getSize());
        assertTrue(response.isHasNextSlice());
        verify(alertRepository, times(1)).findAlertSliceByAlertRequest(userId, alertFilterRequest, pageable);
    }

    @Test
    void testSetAlertStatusRead() {
        Long alertId = 1L;
        Long userId = 1L;
        Alert alert = mock(Alert.class);

        when(alertRepository.findByIdAndUserId(alertId, userId)).thenReturn(Optional.of(alert));

        alertService.setAlertStatusRead(alertId, userId);

        verify(alert, times(1)).readAlert();
    }

    @Test
    void testDeleteAlert() {
        Long alertId = 1L;
        Long userId = 1L;
        Alert alert = mock(Alert.class);

        when(alertRepository.findByIdAndUserId(alertId, userId)).thenReturn(Optional.of(alert));

        alertService.deleteAlert(alertId, userId);

        verify(alert, times(1)).deleteAlert();
    }

    @Test
    void testGetAlertWithValid_ThrowsAlertNotFoundException() {
        Long alertId = 1L;
        Long userId = 1L;

        when(alertRepository.findByIdAndUserId(alertId, userId)).thenReturn(Optional.empty());

        assertThrows(AlertNotFoundException.class, () -> alertService.getAlertWithValid(alertId, userId));
    }

    @Test
    void testGetAlertWithValid_ThrowsAlertDeletedException() {
        Long alertId = 1L;
        Long userId = 1L;
        Alert alert = mock(Alert.class);

        when(alertRepository.findByIdAndUserId(alertId, userId)).thenReturn(Optional.of(alert));
        when(alert.isDeleted()).thenReturn(true);

        assertThrows(AlertDeletedException.class, () -> alertService.getAlertWithValid(alertId, userId));
    }

    @Test
    void testGetAlertWithValid_ReturnsAlert() {
        Long alertId = 1L;
        Long userId = 1L;
        Alert alert = mock(Alert.class);

        when(alertRepository.findByIdAndUserId(alertId, userId)).thenReturn(Optional.of(alert));
        when(alert.isDeleted()).thenReturn(false);

        Alert result = alertService.getAlertWithValid(alertId, userId);

        assertNotNull(result);
        assertEquals(alert, result);
    }
}