package com.tplu.controller;
import com.tplu.model.JarInfo;
import com.tplu.model.FilePathRequest;
import com.tplu.service.FileProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
@RestController
@RequestMapping("/api")
public class FileUploadController {

    @Autowired
    private FileProcessingService fileProcessingService;
    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    @PostMapping("/uploadFilePath")
    public List<JarInfo> handleFilePathUpload(@RequestBody FilePathRequest filePathRequest) {
        return fileProcessingService.processFilePath(filePathRequest.getFilePath());
    }

    @PostMapping("/updateDependencies")
    public ResponseEntity<String> updateDependencies(@RequestBody List<JarInfo> selectedJars) {
        try {
            String resultLog = fileProcessingService.updateDependencies(selectedJars,null);
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
