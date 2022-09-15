package ru.immagixe.CloudFileStorage.minioS3.models;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class BreadCrumb {

    private String name;

    private String url;

    private boolean current;
}
