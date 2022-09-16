package ru.immagixe.CloudFileStorage.storage.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Setter
@Getter
public class DirectoryDTO {
    private List<MultipartFile> files;
    private String path;
}
