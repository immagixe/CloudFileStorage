package ru.immagixe.CloudFileStorage.MinioS3Storage;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import ru.immagixe.CloudFileStorage.MinioS3Storage.models.File;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class FileDAO {

    private final MinioClient minioClient;

    @Value("${s3.bucketName}")
    private String bucketName;

    @Value("${url.schema}")
    private String schema;

    @Value("${url.host}")
    private String hostname;

    @Value("${url.port}")
    private String port;


    @Autowired
    public FileDAO(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public List<File> getListOfFilesAndDirectories(String mainDirectoryName, String pathToSubdirectory) throws ServerException,
            InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException,
            InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        List<File> listFiles = new ArrayList<>();

        Map<String, String> reqParams = new HashMap<>();
        reqParams.put("response-content-type", "application/octet-stream");

        // List all objects
        Iterable<Result<Item>> minioObjects = minioClient
                .listObjects(ListObjectsArgs.builder().bucket(bucketName).prefix(mainDirectoryName + pathToSubdirectory).build());

        // Iterate over each element and set file url
        for (Result<Item> minioObj : minioObjects) {

            Item item = minioObj.get();

            // Create a new File Object
            File file = new File();

            // Set display names of files and directories
            String pathWithoutMainDirectory = item.objectName().substring(mainDirectoryName.length());
            String displayFileOrDirectoryName = Paths.get(pathWithoutMainDirectory).getFileName().toString();
            file.setName(displayFileOrDirectoryName);

            // Set the URL in the directory and in the file
            // If object is directory
            if (item.objectName().endsWith("/")) {

                String subDirectoryName = item.objectName();

                String subDirectoryPath = subDirectoryName.substring(subDirectoryName.indexOf("/") + 1);
                String encodedSubDirectoryPath = URLEncoder.encode(subDirectoryPath, StandardCharsets.UTF_8.toString());

                // Set the URL in the directory
                String urlencoded = getUrlForDirectory(encodedSubDirectoryPath);
                file.setUrl(urlencoded);

                // If object is file
            } else {
                // Set the presigned URL in the file
                file.setUrl(minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(item.objectName())
                        .expiry(2, TimeUnit.HOURS)
                        .extraQueryParams(reqParams)
                        .build()));
            }
            // Add the FileModel object to the list holding FileModel objects
            listFiles.add(file);
        }

        // Return list of directories and files
        return listFiles;
    }

    @NotNull
    public String getUrlForDirectory(String encodedSubDirectoryPath) {
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
    public String getUrlForMainPage() {
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
