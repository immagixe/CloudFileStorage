package ru.immagixe.CloudFileStorage.minioS3.models;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class File {

    private String displayName;

    private String objectName;

    private String url;

    private ObjectType type;

    private int idOnPage;
}
