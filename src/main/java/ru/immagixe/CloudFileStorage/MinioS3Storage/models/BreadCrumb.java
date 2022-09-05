package ru.immagixe.CloudFileStorage.MinioS3Storage.models;

public class BreadCrumb {

    private String name;

    private String url;

    private boolean current;

    public BreadCrumb() {
        this.current = false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isCurrent() {
        return current;
    }

    public void setCurrent(boolean current) {
        this.current = current;
    }
}
