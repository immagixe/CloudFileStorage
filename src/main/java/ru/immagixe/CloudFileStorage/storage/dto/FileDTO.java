package ru.immagixe.CloudFileStorage.storage.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Setter
@Getter
public class FileDTO {
    private MultipartFile file;
    private String path;
}
