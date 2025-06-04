package com.example.matchup.matchupbackend.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.example.matchup.matchupbackend.dto.UploadFile;
import com.example.matchup.matchupbackend.error.exception.FileEx.FileExtensionException;
import com.example.matchup.matchupbackend.error.exception.FileEx.FileUploadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileServiceTest {

    private AmazonS3 amazonS3;
    private FileService fileService;

    @BeforeEach
    void setUp() {
        amazonS3 = mock(AmazonS3.class);
        fileService = new FileService(amazonS3);
        ReflectionTestUtils.setField(fileService, "bucket", "test-bucket");
    }

    @Test
    void storeFile_success() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "test".getBytes());
        URL url = new URL("http://test-bucket/test.png");
        when(amazonS3.getUrl(anyString(), anyString())).thenReturn(url);

        UploadFile uploadFile = fileService.storeFile(file);

        assertEquals("test.png", uploadFile.getUploadFileName());
        assertTrue(uploadFile.getStoreFileName().endsWith(".png"));
        assertEquals(url, uploadFile.getS3Url());
        verify(amazonS3).putObject(any(PutObjectRequest.class));
    }

    @Test
    void storeFile_emptyFile_throwsException() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);
        assertThrows(FileUploadException.class, () -> fileService.storeFile(file));
    }

    @Test
    void storeFile_invalidExtension_throwsException() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "test".getBytes());
        assertThrows(FileExtensionException.class, () -> fileService.storeFile(file));
    }

    @Test
    void storeBase64ToFile_success() {
        String base64 = java.util.Base64.getEncoder().encodeToString("test".getBytes());
        URL url = mock(URL.class);
        when(amazonS3.getUrl(anyString(), anyString())).thenReturn(url);

        UploadFile uploadFile = fileService.storeBase64ToFile(base64, "test.png");

        assertEquals("test.png", uploadFile.getUploadFileName());
        assertTrue(uploadFile.getStoreFileName().endsWith(".png"));
        assertEquals(url, uploadFile.getS3Url());
        verify(amazonS3).putObject(any(PutObjectRequest.class));
    }

    @Test
    void base64ToFile_and_removeNewFile() {
        String base64 = java.util.Base64.getEncoder().encodeToString("test".getBytes());
        String fileName = "testfile.png";
        File file = fileService.base64ToFile(base64, fileName);
        assertTrue(file.exists());
        file.delete(); // 정리
    }
}