package ru.immagixe.CloudFileStorage.storage;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import java.io.IOException;
import java.util.*;

import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import ru.immagixe.CloudFileStorage.storage.models.BreadCrumb;
import ru.immagixe.CloudFileStorage.storage.models.File;

import javax.validation.constraints.NotNull;

@Service
public class StorageService {

    public static final String FILE_STORAGE_DISPLAY_DIRECTORY = "File Storage";

    @Value("${url.schema}")
    private String schema;

    @Value("${url.host}")
    private String hostname;

    @Value("${url.port}")
    private String port;

    @Value("${minio.bucketName}")
    private String bucketName;

    private final StorageDAO storageDAO;

    @Autowired
    public StorageService(StorageDAO storageDAO) {
        this.storageDAO = storageDAO;
    }

    public String getNameOfUserDirectory(int personId) {
        return "user-" + personId + "-files/";
    }

    public void uploadFile(MultipartFile file, String filePath) throws ServerException, InsufficientDataException,
            ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException,
            InvalidResponseException, XmlParserException, InternalException {

        putFileToStorage(file, filePath);
    }

    public void uploadDirectory(List<MultipartFile> files, String filePath) throws ServerException, InsufficientDataException,
            ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException,
            XmlParserException, InternalException {

        for (MultipartFile file : files) {
            putFileToStorage(file, filePath);
        }
    }

    private void putFileToStorage(MultipartFile file, String filePath) throws ErrorResponseException, InsufficientDataException, InternalException, InvalidKeyException, InvalidResponseException, NoSuchAlgorithmException, ServerException, XmlParserException {

        String fileName = file.getOriginalFilename();

        try (InputStream inputStream = new BufferedInputStream(file.getInputStream())) {
            storageDAO.putObject(bucketName, fileName, filePath, inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeObjects(String objectName) throws ServerException, InsufficientDataException,
            ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException,
            InvalidResponseException, XmlParserException, InternalException {

        List<DeleteObject> objectsToDelete = new LinkedList<>();
        Iterable<Result<Item>> listObjects = storageDAO.getListObjects(bucketName, objectName);

        for (Result<Item> object : listObjects) {
            String objectNameToDelete = object.get().objectName();
            objectsToDelete.add(new DeleteObject(objectNameToDelete));

            // If object is directory -> recursive function
            if (isDirectory(objectNameToDelete)) {
                removeObjects(objectNameToDelete);
            }
        }

        Iterable<Result<DeleteError>> results = storageDAO.removeObjects(bucketName, objectsToDelete);

        for (Result<DeleteError> result : results) {
            DeleteError error = result.get();
            System.out.println("Error in deleting object " + error.objectName() + "; " + error.message());
        }
    }

    public void renameObject(String objectName, String displayName) throws ServerException, InsufficientDataException,
            ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException,
            InvalidResponseException, XmlParserException, InternalException {

        String newPath = (isDirectory(objectName) ?
                getPathOfCurrentDirectory(objectName) + displayName + "/" :
                getPathOfCurrentDirectory(objectName) + displayName);

        int countOfSeparator = StringUtils.countOccurrencesOf(newPath, "/");

        copyObjects(objectName, newPath, countOfSeparator);
        removeObjects(objectName);
    }

    public void copyObjects(String objectName, String newPath, int countOfSeparator) throws ServerException,
            InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException,
            InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        Iterable<Result<Item>> listObjects = storageDAO.getListObjects(bucketName, objectName);

        for (Result<Item> object : listObjects) {
            String oldPath = object.get().objectName();

            if (isDirectory(newPath)) {
                String newFileName = oldPath;

                if (isDirectory(oldPath)) {
                    copyObjects(oldPath, newPath, countOfSeparator);
                } else {
                    for (int i = 0; i < countOfSeparator; i++) {
                        newFileName = truncateFirstPart(newFileName);
                    }
                    String newPathToFile = newPath + newFileName;
                    storageDAO.copyObject(bucketName, newPathToFile, oldPath);
                }
            } else {
                storageDAO.copyObject(bucketName, newPath, oldPath);
            }
        }
    }

    public List<File> getListObjects(String objectName) throws ServerException,
            InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException,
            InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        List<File> listFiles = new ArrayList<>();
        int idOnPage = 0;

        Iterable<Result<Item>> minioObjects = storageDAO.getListObjects(bucketName, objectName);

        for (Result<Item> minioObj : minioObjects) {
            Item item = minioObj.get();

            String innerDirectoryPath = truncateFirstPart(item.objectName());
            String displayName = Paths.get(innerDirectoryPath).getFileName().toString();

            if (displayName.length() > 0) {

                // Get URL of current directory
                String fullPath = item.objectName();
                String path = truncateFirstPart(fullPath);
                String currentDirectoryPath = getPathOfCurrentDirectory(path);
                String encodedCurrentDirectoryPath = URLEncoder.encode(currentDirectoryPath, StandardCharsets.UTF_8.toString());
                String urlCurEncoded = getUrlForDirectory(encodedCurrentDirectoryPath);

                // Get URL of file or subdirectory
                String urlSubEncoded;

                if (isDirectory(fullPath)) {
                    String encodedSubDirectoryPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString());
                    urlSubEncoded = getUrlForDirectory(encodedSubDirectoryPath);
                } else {
                    urlSubEncoded = storageDAO.getPresignedObjectUrl(bucketName, fullPath);
                }

                // Create file-info with params. idOnPage needs for correct Thymeleaf foreach loop in modal form
                listFiles.add(new File(++idOnPage, displayName, fullPath, urlSubEncoded, urlCurEncoded));
            }
        }
        return listFiles;
    }

    public List<File> findObjectsByName(String objectName, String searchName) throws ServerException,
            InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException,
            InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        List<File> listFiles = new ArrayList<>();
        int idOnPage = 0;

        recursiveSearching(objectName, searchName, listFiles, idOnPage);
        return listFiles;
    }

    private void recursiveSearching(String objectName, String searchName, List<File> listFiles, int idOnPage)
            throws ErrorResponseException, InsufficientDataException, InternalException, InvalidKeyException,
            InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException, XmlParserException {

        Iterable<Result<Item>> minioObjects = storageDAO.getListObjects(bucketName, objectName);

        // Iterate over each element and set parameters
        for (Result<Item> minioObj : minioObjects) {
            Item item = minioObj.get();

            String innerDirectoryPath = truncateFirstPart(item.objectName());
            String displayName = Paths.get(innerDirectoryPath).getFileName().toString();

            if (displayName.length() > 0) {

                // Get URL of current directory

                String fullPath = item.objectName();
                String path = truncateFirstPart(fullPath);
                String currentDirectoryPath = getPathOfCurrentDirectory(path);
                String encodedCurrentDirectoryPath = URLEncoder.encode(currentDirectoryPath, StandardCharsets.UTF_8.toString());
                String urlCurEncoded = getUrlForDirectory(encodedCurrentDirectoryPath);

                // Get URL of file or subdirectory
                String urlSubEncoded;

                if (isDirectory(fullPath)) {
                    String encodedSubDirectoryPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString());
                    urlSubEncoded = getUrlForDirectory(encodedSubDirectoryPath);

                    // When object is directory -> recursive function
                    recursiveSearching(fullPath, searchName, listFiles, idOnPage);
                } else {
                    urlSubEncoded = storageDAO.getPresignedObjectUrl(bucketName, fullPath);
                }

                if (displayName.toUpperCase().contains(searchName.toUpperCase())) {
                    listFiles.add(new File(++idOnPage, displayName, fullPath, urlSubEncoded, urlCurEncoded));
                }
            }
        }
    }

    public Deque<BreadCrumb> getBreadCrumbs(String pathToSubdirectory) throws UnsupportedEncodingException {

        Deque<BreadCrumb> breadCrumbs = new ArrayDeque<>();
        boolean currentBreadCrumb = false;

        if (pathToSubdirectory.length() != 0) {

            // Convert PathToSubdirectory to bread crumbs with setting name, url and isCurrent status
            List<String> directoryNames = Arrays.asList(pathToSubdirectory.split("/"));

            for (int i = directoryNames.size() - 1; i >= 0; i--) {
                BreadCrumb breadCrumb = new BreadCrumb();

                String encodedSubDirectoryPath = URLEncoder.encode(pathToSubdirectory, StandardCharsets.UTF_8.toString());

                // Cut last directory name in path
                if (i > 0) {
                    pathToSubdirectory = pathToSubdirectory
                            .substring(0, pathToSubdirectory.length() - directoryNames.get(i).length() - 1);
                }

                String name = directoryNames.get(i);
                String url = getUrlForDirectory(encodedSubDirectoryPath);

                breadCrumb.setName(name);
                breadCrumb.setUrl(url);

                // Set current bread crumb
                if (!currentBreadCrumb) {
                    breadCrumb.setCurrent(true);
                    currentBreadCrumb = true;
                }
                breadCrumbs.push(breadCrumb);
            }
        }

        // Add main page bread crumb
        BreadCrumb breadCrumbMainPage = new BreadCrumb();

        breadCrumbMainPage.setName(FILE_STORAGE_DISPLAY_DIRECTORY);
        breadCrumbMainPage.setUrl(getUrlForMainPage());

        if (!currentBreadCrumb) {
            breadCrumbMainPage.setCurrent(true);
        }
        breadCrumbs.push(breadCrumbMainPage);

        return breadCrumbs;
    }

    private String truncateFirstPart(String path) {
        return path.substring(path.indexOf("/") + 1);
    }

    private boolean isDirectory(String item) {
        return item.endsWith("/");
    }

    private String getPathOfCurrentDirectory(String objectName) {

        if (objectName.contains("/")) {
            String withoutLastSeparator = objectName.substring(0, objectName.lastIndexOf("/"));
            String withoutPenultimateSeparator = withoutLastSeparator.substring(0, withoutLastSeparator.lastIndexOf("/") + 1);

            if (isDirectory(objectName)) {
                return withoutPenultimateSeparator;
            } else {
                return (objectName.substring(0, objectName.lastIndexOf("/") + 1));
            }
        }
        return "";
    }

    @NotNull
    private String getUrlForDirectory(String encodedSubDirectoryPath) {

        return UriComponentsBuilder
                .newInstance()
                .scheme(schema)
                .host(hostname)
                .port(port)
                .path("/")
                .queryParam("path", encodedSubDirectoryPath)
                .build()
                .toUriString();
    }

    @NotNull
    private String getUrlForMainPage() {

        return UriComponentsBuilder
                .newInstance()
                .scheme(schema)
                .host(hostname)
                .port(port)
                .path("/")
                .build()
                .toUriString();
    }
}