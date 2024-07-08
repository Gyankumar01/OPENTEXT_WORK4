package com.tplu.controller;

import com.tplu.model.JarInfo;
import com.tplu.service.FileProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
public class FileUploadController {

    private final FileProcessingService fileProcessingService;

    @Autowired
    public FileUploadController(FileProcessingService fileProcessingService) {
        this.fileProcessingService = fileProcessingService;
    }

    @PostMapping("/upload")
    public ResponseEntity<List<JarInfo>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            fileProcessingService.processPomFile(file);
            List<JarInfo> jarInfos = fileProcessingService.parseTestFile();
            return ResponseEntity.ok().body(jarInfos);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null); // Handle error response as needed
        }
    }
}