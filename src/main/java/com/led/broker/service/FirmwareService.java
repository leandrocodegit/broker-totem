package com.led.broker.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FirmwareService {

    private static final Logger logger = LoggerFactory.getLogger(FirmwareService.class);
    private final Path fileStorageLocation;

    @Autowired
    public FirmwareService() throws Exception {
        this.fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();
        Files.createDirectories(this.fileStorageLocation);
    }

    public Mono<String> storeFile(String mac, Mono<FilePart> filePartMono) {
        logger.warn("Salvando arquivo de firmware");
        return filePartMono.flatMap(filePart -> {

            Path targetLocation = fileStorageLocation.resolve(mac);

            return filePart.transferTo(targetLocation)
                    .then(Mono.just(mac))
                    .onErrorMap(ex -> new RuntimeException("Erro ao salvar o arquivo", ex));
        });
    }

//    public String storeFile(MultipartFile file) throws IOException {
//        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
//        if (fileName.contains("..")) {
//            throw new IOException("Invalid file path: " + fileName);
//        }
//        String id = UUID.randomUUID().toString();
//        Path targetLocation = this.fileStorageLocation.resolve(id);
//        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
//        return id;
//    }
    public Resource loadFileAsResource(String fileName) throws IOException {
        Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
        Resource resource = new UrlResource(filePath.toUri());
        if (resource.exists()) {
            return resource;
        } else {
            throw new FileNotFoundException("File not found: " + fileName);
        }
    }
}