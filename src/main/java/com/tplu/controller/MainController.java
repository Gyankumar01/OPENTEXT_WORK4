package com.tplu.controller;

import com.tplu.model.JarInfo;
import com.tplu.service.FileProcessingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MainController {

    private final FileProcessingService fileProcessingService;

    @Autowired
    public MainController(FileProcessingService fileProcessingService) {
        this.fileProcessingService = fileProcessingService;
    }

    @GetMapping("/dependencies")
    public ResponseEntity<List<JarInfo>> getDependencyUpdates() {
        List<JarInfo> jarInfos = fileProcessingService.parseTestFile();
        return ResponseEntity.ok().body(jarInfos);
    }
}
