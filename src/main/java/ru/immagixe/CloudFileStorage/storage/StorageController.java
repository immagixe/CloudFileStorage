package ru.immagixe.CloudFileStorage.storage;

import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.immagixe.CloudFileStorage.storage.dto.DirectoryDTO;
import ru.immagixe.CloudFileStorage.storage.dto.FileDTO;
import ru.immagixe.CloudFileStorage.storage.models.File;
import ru.immagixe.CloudFileStorage.security.securityDetails.PersonDetails;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;

@Controller
public class StorageController {

    private final StorageService storageService;


    @Autowired
    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/")
    public String listFiles(@RequestParam(value = "path", required = false, defaultValue = "")
                            String pathToSubdirectory, Model model,
                            @ModelAttribute FileDTO fileDTO,
                            @ModelAttribute DirectoryDTO directoryDTO,
                            @AuthenticationPrincipal PersonDetails personDetails)

            throws ServerException, InsufficientDataException, ErrorResponseException, IOException,
            NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException,
            InternalException {


//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        PersonDetails personDetails = (PersonDetails) authentication.getPrincipal();

        String mainDirectoryName = storageService.getNameOfUserDirectory(personDetails.getPerson().getId());

        String currentDirectoryPath = mainDirectoryName + pathToSubdirectory;

        fileDTO.setPath(currentDirectoryPath);
        directoryDTO.setPath(currentDirectoryPath);

        model.addAttribute("pathToCurrentDirectory", currentDirectoryPath);
        model.addAttribute("breadCrumbs", storageService.getBreadCrumbs(pathToSubdirectory));
        model.addAttribute("listFiles", storageService.getListObjects(
                mainDirectoryName + pathToSubdirectory));

        return "main";
    }

    @PostMapping(value = "/upload-file")
    public String uploadFile(@RequestHeader(value = "referer", required = false) final String referer,
                             @ModelAttribute FileDTO fileDTO)
            throws ServerException, InsufficientDataException, ErrorResponseException,
            NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException,
            InternalException {


        storageService.uploadFile(fileDTO.getFile(), fileDTO.getPath());
        return "redirect:" + referer;
    }

    @PostMapping(value = "/upload-directory")
    public String uploadDirectory(@RequestHeader(value = "referer", required = false) final String referer,
                                  @ModelAttribute DirectoryDTO directoryDTO)
            throws ServerException, InsufficientDataException, ErrorResponseException,
            NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException,
            InternalException {

        storageService.uploadDirectory(directoryDTO.getFiles(), directoryDTO.getPath());
        return "redirect:" + referer;
    }

    @DeleteMapping(value = "/remove")
    public String removeFileOrDirectory(@RequestHeader(value = "referer", required = false) final String referer,
                                        @ModelAttribute File file)
            throws ServerException, InsufficientDataException, ErrorResponseException, IOException,
            NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException,
            InternalException {

        storageService.removeObjects(file.getObjectName());
        return "redirect:" + referer;
    }

    @PatchMapping(value = "/rename")
    public String renameFileOrDirectory(@RequestHeader(value = "referer", required = false) final String referer,
                                        @ModelAttribute File file)
            throws ServerException, InsufficientDataException, ErrorResponseException, IOException,
            NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException,
            InternalException {

        storageService.renameObject(file.getObjectName(), file.getDisplayName());
        return "redirect:" + referer;
    }

    @GetMapping(value = "/search")
    public String foundFiles(@RequestParam(value = "query", required = false) String query, Model model)
            throws ServerException, InsufficientDataException, ErrorResponseException, IOException,
            NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException,
            InternalException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        PersonDetails personDetails = (PersonDetails) authentication.getPrincipal();

        String mainDirectoryName = storageService.getNameOfUserDirectory(personDetails.getPerson().getId());
        model.addAttribute("foundFiles", storageService.findObjectsByName(mainDirectoryName, query));

        return "search";
    }
}
