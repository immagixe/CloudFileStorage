package ru.immagixe.CloudFileStorage.minioS3;

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
import ru.immagixe.CloudFileStorage.minioS3.models.BreadCrumb;
import ru.immagixe.CloudFileStorage.minioS3.models.File;
import ru.immagixe.CloudFileStorage.minioS3.models.ObjectType;

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

    @Value("${s3.bucketName}")
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
            if (objectNameToDelete.endsWith("/")) {
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

        String newPath = getPathOfCurrentDirectory(objectName) + displayName + "/";
        int countOfSeparator = StringUtils.countOccurrencesOf(newPath, "/");

        copyDirectory(objectName, newPath, countOfSeparator);
        removeObjects(objectName);
    }

    public void copyDirectory(String objectName, String newPath, int countOfSeparator) throws ServerException,
            InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException,
            InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        Iterable<Result<Item>> listObjects = storageDAO.getListObjects(bucketName, objectName);

        for (Result<Item> object : listObjects) {

            String oldPath = object.get().objectName();
            String suffixOfNewPath = oldPath;

            // If object is directory -> recursive function
            if (oldPath.endsWith("/")) {
                copyDirectory(oldPath, newPath, countOfSeparator);
            } else {

                for (int i = 0; i < countOfSeparator; i++) {
                    suffixOfNewPath = truncateFirstDirectoryName(suffixOfNewPath);
                }

                String newObjectName = newPath + suffixOfNewPath;
                storageDAO.copyObject(bucketName, newObjectName, oldPath);
            }
        }
    }

    public List<File> getListOfFilesAndDirectories(String objectName) throws ServerException,
            InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException,
            InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        List<File> listFiles = new ArrayList<>();

        Map<String, String> reqParams = new HashMap<>();
        reqParams.put("response-content-type", "application/octet-stream");

        int idCount = 0;

        // List all objects
        Iterable<Result<Item>> minioObjects = storageDAO.getListObjects(bucketName, objectName);

        // Iterate over each element and set file url
        for (Result<Item> minioObj : minioObjects) {

            Item item = minioObj.get();

            // Create a new File Object
            File file = new File();

            file.setIdOnPage(++idCount);
            file.setObjectName(item.objectName());

            // Set display names of files and directories
            String pathWithoutMainDirectory = truncateFirstDirectoryName(item.objectName());
            String displayFileOrDirectoryName = Paths.get(pathWithoutMainDirectory).getFileName().toString();
            file.setDisplayName(displayFileOrDirectoryName);

            // Set the URL in the directory and in the file
            // If object is directory
            if (item.objectName().endsWith("/")) {

                file.setType(ObjectType.DIRECTORY);

                String subDirectoryName = item.objectName();

                String subDirectoryPath = truncateFirstDirectoryName(subDirectoryName);
                String encodedSubDirectoryPath = URLEncoder.encode(subDirectoryPath, StandardCharsets.UTF_8.toString());

                // Set the URL in the directory
                String urlencoded = getUrlForDirectory(encodedSubDirectoryPath);
                file.setUrl(urlencoded);

                // If object is file
            } else {
                file.setType(ObjectType.FILE);
                // Set the presigned URL in the file
                file.setUrl(storageDAO.getPresignedObjectUrl(bucketName, item.objectName(), reqParams));
            }
            // Add the File object to the list holding File objects
            if (file.getDisplayName().length() > 0) {
                listFiles.add(file);
            }
        }

        // Return list of directories and files
        return listFiles;
    }

    private String truncateFirstDirectoryName(String path) {
        return path.substring(path.indexOf("/") + 1);
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

    private String getPathOfCurrentDirectory(String objectName) {

        String withoutLastSeparator = objectName.substring(0, objectName.lastIndexOf("/"));
        String withoutPenultimateSeparator = withoutLastSeparator.substring(0, withoutLastSeparator.lastIndexOf("/") + 1);

        if (objectName.endsWith("/")) {
            return withoutPenultimateSeparator;
        } else {
            return objectName.substring(0, objectName.lastIndexOf("/") + 1);
        }
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

//    public void createDirectory(String pathToCurrentDirectory, String directoryName) throws ServerException, InsufficientDataException,
//            ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException,
//            InvalidResponseException, XmlParserException, InternalException {
//
//        String objectName = pathToCurrentDirectory + directoryName + "/";
//        storageDAO.createDirectory(bucketName, objectName);
//    }
//}