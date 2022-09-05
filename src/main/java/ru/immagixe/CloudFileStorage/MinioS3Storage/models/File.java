package ru.immagixe.CloudFileStorage.MinioS3Storage.models;

import java.time.ZonedDateTime;

public class File {

    private String name;

    private long size;

    private ZonedDateTime lastModified;

    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public ZonedDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(ZonedDateTime lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public String toString() {
        return "FileModel{" +
                "name='" + name + '\'' +
                ", size=" + size +
                ", lastModified=" + lastModified +
                ", url='" + url + '\'' +
                '}';
    }
}
