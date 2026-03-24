// src/main/java/com/example/rentapp/service/FileStorageService.java
package com.example.rentapp.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path root = Paths.get("uploads");

    public FileStorageService() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать папку для загрузок", e);
        }
    }

    public List<String> savePhotos(List<MultipartFile> files) {
        List<String> fileNames = new ArrayList<>(); // ← теперь только имена файлов

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String originalName = file.getOriginalFilename();
            String fileName = UUID.randomUUID() + "_" + originalName;

            try {
                Path destination = root.resolve(fileName);
                Files.copy(file.getInputStream(), destination);

                // ← ВОЗВРАЩАЕМ ТОЛЬКО ИМЯ ФАЙЛА БЕЗ ПУТИ!
                fileNames.add(fileName);
            } catch (IOException e) {
                throw new RuntimeException("Ошибка сохранения фото: " + fileName, e);
            }
        }
        return fileNames;
    }
}