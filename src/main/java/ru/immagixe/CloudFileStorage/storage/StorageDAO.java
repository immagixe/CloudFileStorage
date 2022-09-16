package ru.immagixe.CloudFileStorage.storage;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class StorageDAO {

    private final MinioClient minioClient;

    @Autowired
    public StorageDAO(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public Iterable<Result<Item>> getListObjects(String bucketName, String objectName) {

        return minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(objectName)
                        .build());
    }

    public void putObject(String bucketName, String fileName, String filePath, InputStream inputStream)
            throws ErrorResponseException, InsufficientDataException, InternalException, InvalidKeyException,
            InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException, XmlParserException {

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(filePath + fileName)
                        .stream(inputStream, -1, 10485760)
                        .contentType("application/octet-stream")
                        .build());
    }

    public void copyObject(String bucketName, String newPath, String oldPath) throws ServerException,
            InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException,
            InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        minioClient.copyObject(
                CopyObjectArgs.builder()
                        .bucket(bucketName)
                        .object(newPath)
                        .source(CopySource.builder()
                                .bucket(bucketName)
                                .object(oldPath)
                                .build())
                        .build());
    }

    public Iterable<Result<DeleteError>> removeObjects(String bucketName, List<DeleteObject> filesToDelete) {

        return minioClient.removeObjects(
                RemoveObjectsArgs.builder()
                        .bucket(bucketName)
                        .objects(filesToDelete)
                        .build());
    }

    public String getPresignedObjectUrl(String bucketName, String objectName)
            throws ServerException, InsufficientDataException, ErrorResponseException, IOException,
            NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException,
            InternalException {

        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(2, TimeUnit.HOURS)
                        .extraQueryParams(new HashMap<>(
                                Map.of("response-content-type", "application/octet-stream")))
                        .build());
    }
}
