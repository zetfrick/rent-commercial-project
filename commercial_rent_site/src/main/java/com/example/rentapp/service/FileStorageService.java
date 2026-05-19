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
    private final Path chatUploads = Paths.get("chat-uploads");

    public FileStorageService() {
        try {
            Files.createDirectories(root);
            Files.createDirectories(chatUploads);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать папку для загрузок", e);
        }
    }

    public List<String> savePhotos(List<MultipartFile> files) {
        List<String> fileNames = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String originalName = file.getOriginalFilename();
            String fileName = UUID.randomUUID() + "_" + originalName;

            try {
                Path destination = root.resolve(fileName);
                Files.copy(file.getInputStream(), destination);
                fileNames.add(fileName);
            } catch (IOException e) {
                throw new RuntimeException("Ошибка сохранения фото: " + fileName, e);
            }
        }
        return fileNames;
    }

    /**
     * Сохраняет файл из чата в папку chat-uploads
     * @param file загружаемый файл
     * @return имя сохранённого файла
     */
    public String saveChatFile(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        // Очищаем имя файла от опасных символов
        String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String fileName = UUID.randomUUID() + "_" + safeName;

        try {
            Path destination = chatUploads.resolve(fileName);
            Files.copy(file.getInputStream(), destination);
            return fileName;
        } catch (IOException e) {
            throw new RuntimeException("Ошибка сохранения файла: " + fileName, e);
        }
    }
}