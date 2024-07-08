package com.tplu.service;

import com.tplu.model.JarInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FileProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(FileProcessingService.class);
    private static final String START_LINE = "The following dependencies in Dependencies have newer versions:";
    private static final Pattern JAR_INFO_PATTERN = Pattern.compile("\\s*([^:]+):([^\\s]+)\\s+.*\\s([^\\s]+)\\s+->\\s+([^\\s]+)");

    public void processPomFile(MultipartFile file) throws IOException, InterruptedException {
        File tempFile = new File("pom.xml");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(file.getBytes());
        }

        String[] command = {"cmd.exe", "/c", "mvn versions:display-dependency-updates > test.txt"};
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File("."));
        Process process = processBuilder.start();

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String errorMsg = readStream(process.getErrorStream());
            throw new RuntimeException("Failed to execute Maven command. Exit code: " + exitCode + ". Error: " + errorMsg);
        }
    }

    public List<JarInfo> parseTestFile() {
        List<JarInfo> jarInfos = new ArrayList<>();
        Set<String> seenArtifacts = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new FileReader("test.txt"))) {
            String line;
            boolean startProcessing = false;

            while ((line = reader.readLine()) != null) {
                if (line.contains(START_LINE)) {
                    startProcessing = true;
                    continue;
                }

                if (startProcessing) {
                    Matcher matcher = JAR_INFO_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        String group1 = matcher.group(1).trim();
                        String group2 = matcher.group(2).trim();
                        String artifact = group1 + ":" + group2;
                        String currentVersion = matcher.group(3).trim();
                        String newVersion = matcher.group(4).trim();

                        if (seenArtifacts.add(artifact)) {
                            jarInfos.add(new JarInfo(artifact, currentVersion, newVersion));
                            logger.info("Processed artifact: {}, Current Version: {}, New Version: {}", artifact, currentVersion, newVersion);
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error reading file:", e);
            throw new RuntimeException("Error reading file", e);
        }

        return jarInfos;
    }

    private String readStream(InputStream stream) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }
}
