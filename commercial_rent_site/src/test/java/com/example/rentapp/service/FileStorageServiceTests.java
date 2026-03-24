package com.example.rentapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTests {

    private FileStorageService service;

    @TempDir
    Path tempFolder;

    @BeforeEach
    void prepare() throws Exception {
        service = new FileStorageService();

        // Подмена приватного поля root через reflection
        var field = FileStorageService.class.getDeclaredField("root");
        field.setAccessible(true);
        field.set(service, tempFolder);
    }

    @Test
    void savePhotos_oneValidFile() throws Exception {
        MultipartFile file = new MockMultipartFile(
                "photos", "office-1.jpg", "image/jpeg", "fake image".getBytes()
        );

        List<String> names = service.savePhotos(List.of(file));

        assertEquals(1, names.size());
        String name = names.get(0);
        assertTrue(name.contains("_office-1.jpg"));
        assertTrue(Files.exists(tempFolder.resolve(name)));
    }

    @Test
    void savePhotos_twoFiles() throws Exception {
        var f1 = new MockMultipartFile("f", "plan.pdf", null, "pdf".getBytes());
        var f2 = new MockMultipartFile("f", "photo.png", null, "png".getBytes());

        List<String> saved = service.savePhotos(List.of(f1, f2));

        assertEquals(2, saved.size());
        assertTrue(Files.exists(tempFolder.resolve(saved.get(0))));
        assertTrue(Files.exists(tempFolder.resolve(saved.get(1))));
    }

    @Test
    void savePhotos_emptyList_returnsEmptyList() {
        assertTrue(service.savePhotos(List.of()).isEmpty());
    }

    @Test
    void savePhotos_emptyFile_isSkipped() throws Exception {
        var empty = new MockMultipartFile("empty", "zero.jpg", "image/jpeg", new byte[0]);

        List<String> result = service.savePhotos(List.of(empty));

        assertTrue(result.isEmpty());
    }

    @Test
    void savePhotos_nullFileList_throwsNPE() {
        assertThrows(NullPointerException.class, () -> service.savePhotos(null));
    }

    @Test
    void savePhotos_fileWithVeryLongName_stillSaved() throws Exception {
        String longName = "a".repeat(150) + ".jpeg";
        var file = new MockMultipartFile("photo", longName, "image/jpeg", "data".getBytes());

        List<String> saved = service.savePhotos(List.of(file));

        assertEquals(1, saved.size());
        assertTrue(Files.exists(tempFolder.resolve(saved.get(0))));
    }

    @Test
    void constructor_directoryAlreadyExists_noException() throws IOException {
        Files.createDirectories(tempFolder);
        assertDoesNotThrow(() -> new FileStorageService());
    }
}