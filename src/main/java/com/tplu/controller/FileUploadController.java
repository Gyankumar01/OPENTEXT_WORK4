package com.tplu.controller;

import com.tplu.model.JarInfo;
import com.tplu.service.FileProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class FileUploadController {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    private final FileProcessingService fileProcessingService;

    @Autowired
    public FileUploadController(FileProcessingService fileProcessingService) {
        this.fileProcessingService = fileProcessingService;
    }

    @PostMapping("/upload")
    public ResponseEntity<List<JarInfo>> uploadFile(@RequestBody Map<String, String> requestBody) {
        String filePath = requestBody.get("filePath");
        try {
            logger.info("File path received: {}", filePath);
            fileProcessingService.processPomFile(filePath);
            List<JarInfo> jarInfos = fileProcessingService.parseTestFile();
            logger.info("File processed successfully.");
            return ResponseEntity.ok().body(jarInfos);
        } catch (Exception e) {
            logger.error("Error during file processing: {}", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping("/update")
    public ResponseEntity<String> updateDependencies(@RequestBody List<JarInfo> selectedJars) {
        try {
            String resultLog = fileProcessingService.updateDependencies(selectedJars);
            return ResponseEntity.ok(resultLog);
        } catch (IOException e) {
            logger.error("IO Exception while updating dependencies", e);
            return ResponseEntity.status(500).body("Failed to update dependencies due to IO Exception: " + e.getMessage());
        } catch (InterruptedException e) {
            logger.error("Interrupted Exception while updating dependencies", e);
            return ResponseEntity.status(500).body("Failed to update dependencies due to Interrupted Exception: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error while updating dependencies", e);
            return ResponseEntity.status(500).body("Failed to update dependencies due to unexpected error: " + e.getMessage());
        }
    }
}
