package com.example.matchup.matchupbackend.service;

import com.example.matchup.matchupbackend.dto.UploadFile;
import com.example.matchup.matchupbackend.dto.request.jobposting.JobPostingRequest;
import com.example.matchup.matchupbackend.dto.request.jobposting.JobPostingSearchRequest;
import com.example.matchup.matchupbackend.dto.response.jobposting.JobPostingDetailResponse;
import com.example.matchup.matchupbackend.dto.response.jobposting.JobPostingPageResponse;
import com.example.matchup.matchupbackend.dto.response.jobposting.JobPostingResponse;
import com.example.matchup.matchupbackend.entity.JobPosting;
import com.example.matchup.matchupbackend.error.ErrorCode;
import com.example.matchup.matchupbackend.error.exception.ResourceNotFoundEx.ResourceNotFoundException;
import com.example.matchup.matchupbackend.repository.jobposting.JobPostingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.net.URL;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JobPostingServiceTest {

    private JobPostingRepository jobPostingRepository;
    private FileService fileService;
    private JobPostingService jobPostingService;

    @BeforeEach
    void setUp() {
        jobPostingRepository = mock(JobPostingRepository.class);
        fileService = mock(FileService.class);
        jobPostingService = new JobPostingService(jobPostingRepository, fileService);
    }

    @Test
    void searchJobPosting_success() {
        JobPostingSearchRequest searchRequest = mock(JobPostingSearchRequest.class);
        Pageable pageable = PageRequest.of(0, 10);
        JobPosting jobPosting = mock(JobPosting.class);
        Page<JobPosting> page = new PageImpl<>(List.of(jobPosting), pageable, 1);

        when(jobPostingRepository.findJobPostingBySearchRequest(searchRequest, pageable)).thenReturn(page);

        JobPostingPageResponse response = jobPostingService.searchJobPosting(searchRequest, pageable);

        assertEquals(1, response.getResponseList().size());
        assertEquals(1, response.getTotalDataCount());
        assertEquals(1, response.getTotalPageCount());
        assertEquals(1, response.getPage());
    }

    @Test
    void saveJobPosting_withImage_success() throws Exception {
        JobPostingRequest request = mock(JobPostingRequest.class);
        when(request.getImageBase64()).thenReturn("base64");
        when(request.getImageName()).thenReturn("test.png");

        UploadFile uploadFile = new UploadFile("test.png", "uuid.png", new URL("http://test-bucket/uuid.png"));
        when(fileService.storeBase64ToFile("base64", "test.png")).thenReturn(uploadFile);

        JobPosting jobPosting = mock(JobPosting.class);
        try (MockedStatic<JobPosting> staticMock = mockStatic(JobPosting.class)) {
            staticMock.when(() -> JobPosting.from(uploadFile, request)).thenReturn(jobPosting);
            when(jobPostingRepository.save(jobPosting)).thenReturn(jobPosting);

            JobPosting result = jobPostingService.saveJobPosting(request);

            assertEquals(jobPosting, result);
            verify(jobPostingRepository).save(jobPosting);
        }
    }

    @Test
    void saveJobPosting_withoutImage_success() {
        JobPostingRequest request = mock(JobPostingRequest.class);
        when(request.getImageBase64()).thenReturn(null);

        JobPosting jobPosting = mock(JobPosting.class);
        try (MockedStatic<JobPosting> staticMock = mockStatic(JobPosting.class)) {
            staticMock.when(() -> JobPosting.from(request)).thenReturn(jobPosting);
            when(jobPostingRepository.save(jobPosting)).thenReturn(jobPosting);

            JobPosting result = jobPostingService.saveJobPosting(request);

            assertEquals(jobPosting, result);
            verify(jobPostingRepository).save(jobPosting);
        }
    }

    @Test
    void getDetailJobPosting_success() {
        Long id = 1L;
        JobPosting jobPosting = mock(JobPosting.class);
        when(jobPostingRepository.findById(id)).thenReturn(Optional.of(jobPosting));
        JobPostingDetailResponse response = mock(JobPostingDetailResponse.class);

        try (MockedStatic<JobPostingDetailResponse> staticMock = mockStatic(JobPostingDetailResponse.class)) {
            staticMock.when(() -> JobPostingDetailResponse.fromEntity(jobPosting)).thenReturn(response);

            JobPostingDetailResponse result = jobPostingService.getDetailJobPosting(id);

            assertEquals(response, result);
        }
    }

    @Test
    void getDetailJobPosting_notFound() {
        Long id = 1L;
        when(jobPostingRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> jobPostingService.getDetailJobPosting(id));
    }

    @Test
    void deleteJobPosting_success() {
        Long id = 1L;
        JobPosting jobPosting = mock(JobPosting.class);
        when(jobPostingRepository.findById(id)).thenReturn(Optional.of(jobPosting));
        when(jobPosting.getTitle()).thenReturn("title");

        String result = jobPostingService.deleteJobPosting(id);

        assertTrue(result.contains("title"));
        verify(jobPostingRepository).delete(jobPosting);
    }

    @Test
    void deleteJobPosting_notFound() {
        Long id = 1L;
        when(jobPostingRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> jobPostingService.deleteJobPosting(id));
    }
}