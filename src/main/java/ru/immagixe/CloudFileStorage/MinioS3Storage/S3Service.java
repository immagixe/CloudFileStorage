package ru.immagixe.CloudFileStorage.MinioS3Storage;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;


import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.immagixe.CloudFileStorage.MinioS3Storage.models.BreadCrumb;
import ru.immagixe.CloudFileStorage.MinioS3Storage.models.File;


@Service
public class S3Service {

    public static final String FILE_STORAGE_DIRECTORY = "FileStorage";

    private final FileDAO fileDAO;

    private final MinioClient minioClient;

    @Value("${s3.bucketName}")
    private String bucketName;

    //    private static String keyName = "user-1-files/docs/summer.jpg";
    private static String uploadFileName = "C:\\summer.jpg";

    @Autowired
    public S3Service(FileDAO fileDAO, MinioClient minioClient) {
        this.fileDAO = fileDAO;
        this.minioClient = minioClient;
    }

    public void createBucket() {
        try {
            boolean bucketFound = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());

            if (!bucketFound) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (MinioException e) {
            System.out.println("Error occurred: " + e);
            System.out.println("HTTP trace: " + e.httpTrace());
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public void uploadFile(String keyName) {

        System.out.println("Uploading a new object to S3 from a file\n");

        UploadObjectArgs args;
        try {
            args = UploadObjectArgs.builder()
                    .bucket(bucketName)
                    .object(keyName)
                    .filename(uploadFileName)
                    .build();
            minioClient.uploadObject(args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void downloadFile(String keyName) {

        Map<String, String> reqParams = new HashMap<>();
        reqParams.put("response-content-type", "application/octet-stream");
        try {
//            String url =
//                    minioClient.getPresignedObjectUrl(
//                            GetPresignedObjectUrlArgs.builder()
//                                    .method(Method.HEAD)
//                                    .bucket(bucketName)
//                                    .object(keyName)
//                                    .expiry(2, TimeUnit.HOURS)
//                                    .extraQueryParams(reqParams)
//                                    .build());
//            System.out.println(url);

            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(keyName)
                            .expiry(2, TimeUnit.HOURS)
                            .extraQueryParams(reqParams)
                            .build());
            System.out.println(url);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        java.io.File localFile = new java.io.File(System.getProperty("user.home"), "/Desktop/" + "summer355.png");
//            File localFile = new File("summer355.jpg");

//        try {
//            minioClient.downloadObject(
//                    DownloadObjectArgs.builder()
//                            .bucket(bucketName)
//                            .object(keyName)
//                            .filename(String.valueOf(localFile))
//                            .build());
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }

//            minioClient.getObject(
////                    new GetObjectRequest(bucketName, keyName),
////                    localFile
////            );

    }


//            // Download file
//            GetObjectRequest rangeObjectRequest = new GetObjectRequest(bucketName, keyName);
//            S3Object objectPortion = minioClient.getObject(rangeObjectRequest);
//            System.out.println("Printing bytes retrieved:");
//            displayTextInputStream(objectPortion.getObjectContent());
//        } catch (AmazonServiceException ase) {
//            printInfoExceptionASE(ase);
//        } catch (AmazonClientException ace) {
//            printInfoExceptionACE(ace);
//        }
//    }


    public void createDirectoryIfDoesntExist(String directoryName) {

        PutObjectArgs args = PutObjectArgs.builder().bucket(bucketName).object(directoryName).stream(
                new ByteArrayInputStream(new byte[]{}), 0, -1).build();
        try {
            minioClient.putObject(args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getNameOfUserDirectory(int personId) {
        return "user-" + personId + "-files/";
    }

//        if (!isDirectoryExists(minioClient, directoryName)) {
//
//            // Create meta-data for new folder and set content-length to 0
//            ObjectMetadata metadata = new ObjectMetadata();
////            metadata.setContentLength(0);
//
//            // Create empty content
//            InputStream emptyContent = new ByteArrayInputStream(new byte[0]);
//
//            // Create a PutObjectRequest passing the directory name suffixed by /
//            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName,
//                    directoryName + SUFFIX, emptyContent, metadata);
//
//            minioClient.putObject(bucketName, objectName, inputStream, contentType);
//            // Send request to S3 to create folder
//            minioClient.putObject(bucketName, directoryName + SUFFIX, emptyContent, );
//        } else {
//            System.out.println("Such directory already exists");
//        }

//
//    private boolean isDirectoryExists(MinioClient s3Client, String directoryName) {
//        ListObjectsV2Result result = s3Client.listObjectsV2(bucketName, directoryName);
//        return result.getKeyCount() > 0;
//    }

    public List<File> listFiles(String mainDirectoryName, String pathToSubdirectory) throws ServerException,
            InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException,
            InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        return fileDAO.getListOfFilesAndDirectories(mainDirectoryName, pathToSubdirectory);
    }

    public Deque<BreadCrumb> getBreadCrumbs(String pathToSubdirectory) throws UnsupportedEncodingException {

        Deque<BreadCrumb> breadCrumbs = new ArrayDeque<>();
        boolean currentBreadCrumb = false;

        if (pathToSubdirectory.length() != 0) {

            // Convert PathToSubdirectory to bread crumbs with setting name, url and isCurrent status
            List<String> directoryNames = Arrays.asList(pathToSubdirectory.split("/"));

            for (int i = directoryNames.size() - 1; i >= 0; i--) {
                BreadCrumb breadCrumb = new BreadCrumb();

                String name = directoryNames.get(i);
                String encodedSubDirectoryPath = URLEncoder.encode(pathToSubdirectory, StandardCharsets.UTF_8.toString());
                String url = fileDAO.getUrlForDirectory(encodedSubDirectoryPath);

                if (i > 0) {
                    pathToSubdirectory = pathToSubdirectory
                            .substring(0, pathToSubdirectory.length() - directoryNames.get(i).length() - 1);
                }

                breadCrumb.setName(name);
                breadCrumb.setUrl(url);

                if (!currentBreadCrumb) {
                    breadCrumb.setCurrent(true);
                    currentBreadCrumb = true;
                }
                breadCrumbs.push(breadCrumb);
            }
        }

        // Add main page bread crumb
        BreadCrumb breadCrumbMainPage = new BreadCrumb();

        breadCrumbMainPage.setName(FILE_STORAGE_DIRECTORY);
        breadCrumbMainPage.setUrl(fileDAO.getUrlForMainPage());

        if (!currentBreadCrumb) {
            breadCrumbMainPage.setCurrent(true);
        }
        breadCrumbs.push(breadCrumbMainPage);

        return breadCrumbs;
    }
}

