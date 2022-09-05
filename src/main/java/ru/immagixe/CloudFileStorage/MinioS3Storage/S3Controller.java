package ru.immagixe.CloudFileStorage.MinioS3Storage;

import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.immagixe.CloudFileStorage.MinioS3Storage.models.BreadCrumb;
import ru.immagixe.CloudFileStorage.MinioS3Storage.models.File;
import ru.immagixe.CloudFileStorage.security.PersonDetails;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Deque;
import java.util.List;

@Controller
public class S3Controller {

    private final S3Service s3Service;

    @Autowired
    public S3Controller(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @GetMapping("/")
    public String listUserFiles(@RequestParam(value = "path", required = false, defaultValue = "")
                                    String pathToSubdirectory, Model model)

            throws ServerException, InsufficientDataException, ErrorResponseException, IOException,
            NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException,
            InternalException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        PersonDetails personDetails = (PersonDetails) authentication.getPrincipal();

        String mainDirectoryName = s3Service.getNameOfUserDirectory(personDetails.getPerson().getId());
        s3Service.createDirectoryIfDoesntExist(mainDirectoryName);

        List<File> listFiles = s3Service.listFiles(mainDirectoryName, pathToSubdirectory);

        Deque<BreadCrumb> breadCrumbs = s3Service.getBreadCrumbs(pathToSubdirectory);

        for (BreadCrumb breadCrumb : breadCrumbs) {
            System.out.println(breadCrumb.getName() + "      " + breadCrumb.getUrl());
        }

//        System.out.println(pathToSubdirectory);


        for (File listFile : listFiles) {
            System.out.println(listFile.toString());
        }


        model.addAttribute("breadCrumbs", s3Service.getBreadCrumbs(pathToSubdirectory));
        model.addAttribute("listFiles", s3Service.listFiles(mainDirectoryName, pathToSubdirectory));


        s3Service.createDirectoryIfDoesntExist(mainDirectoryName);

        return "main";
    }


    @GetMapping("/upload")
    public String startApp() throws IOException {

        String keyName1 = "user-2-files/docs/summer.jpg";

        String keyName2 = "user-5-files/1.png";

        s3Service.createBucket();
        s3Service.uploadFile(keyName1);
        s3Service.downloadFile(keyName2);
//
        String directoryName = "user-6-files/";
//
        s3Service.createDirectoryIfDoesntExist(directoryName);


//        s3Service.getListObjects ("user-files");

//        s3Service.isDirectoryExists(s3Client, "user-6-files/");

        return "main";
    }
}
