package com.tplu.service;

import com.tplu.model.JarInfo;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FileProcessingService {
    private static final Logger logger = LoggerFactory.getLogger(FileProcessingService.class);

    private static final String START_LINE = "newer versions:";
    private static final Pattern JAR_INFO_PATTERN_SINGLE_LINE = Pattern.compile("\\[INFO\\]\\s*([^:]+):([^\\s]+)\\s+.*\\s([^\\s]+)\\s+->\\s+([^\\s]+)");
    private static final Pattern JAR_INFO_PATTERN_NEXT_LINE = Pattern.compile("\\[INFO\\]\\s*([^:]+):([^\\s]+)\\s+.*\\.{3}");

    String fpath = "";

    public List<JarInfo> processFilePath(String filePath) {
        fpath = filePath;
        logger.info("Processing file path: {}", filePath);
        List<JarInfo> jarInfos = new ArrayList<>();
        Set<String> seenArtifacts = new HashSet<>();

        try {
            StringBuilder command = new StringBuilder();


            command.append("cd / && ");
            command.append("cd ").append(filePath.replace("\\", "/")).append(" && ");
            command.append("mvn versions:display-dependency-updates > test.txt");

            logger.info("Executing command: {}", command.toString());

            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command.toString());
            processBuilder.directory(new File(filePath)); // Set working directory to the specified path
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info("Process output: {}", line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.error("Error executing command. Exit code: {}", exitCode);
            } else {
                logger.info("Command executed successfully.");
            }


            File testFile = new File(filePath, "test.txt");
            try (BufferedReader fileReader = new BufferedReader(new FileReader(testFile))) {
                boolean startProcessing = false;
                String currentArtifact = null;

                while ((line = fileReader.readLine()) != null) {
                    logger.info("Reading line from test.txt: {}", line);  // Log each line read from test.txt
                    if (line.contains(START_LINE)) {
                        startProcessing = true;
                        continue;
                    }

                    if (startProcessing) {
                        Matcher matcherSingleLine = JAR_INFO_PATTERN_SINGLE_LINE.matcher(line);
                        Matcher matcherNextLine = JAR_INFO_PATTERN_NEXT_LINE.matcher(line);

                        if (matcherSingleLine.find()) {
                            String group1 = matcherSingleLine.group(1).trim();
                            String group2 = matcherSingleLine.group(2).trim();
                            String artifact = group1 + ":" + group2;
                            String currentVersion = matcherSingleLine.group(3).trim();
                            String newVersion = matcherSingleLine.group(4).trim();

                            if (seenArtifacts.add(artifact)) {
                                jarInfos.add(new JarInfo(artifact, currentVersion, newVersion));
                                logger.info("Added JarInfo: {}", jarInfos.get(jarInfos.size() - 1));
                            }
                        } else if (matcherNextLine.find()) {
                            currentArtifact = matcherNextLine.group(1).trim() + ":" + matcherNextLine.group(2).trim();
                        } else if (currentArtifact != null) {
                            Matcher versionMatcher = Pattern.compile("\\s*([^\\s]+)\\s+->\\s+([^\\s]+)").matcher(line);
                            if (versionMatcher.find()) {
                                String currentVersion = versionMatcher.group(1).trim();
                                String newVersion = versionMatcher.group(2).trim();

                                if (seenArtifacts.add(currentArtifact)) {
                                    jarInfos.add(new JarInfo(currentArtifact, currentVersion, newVersion));
                                    logger.info("Added JarInfo: {}", jarInfos.get(jarInfos.size() - 1));
                                    currentArtifact = null;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error reading test.txt file: ", e);
            }
        } catch (Exception e) {
            logger.error("Error processing file path: ", e);
        }

        logger.info("Finished processing file path. JarInfos: {}", jarInfos);
        return jarInfos;
    }

    public String updateDependencies(List<JarInfo> selectedJars) throws IOException, InterruptedException {
        logger.info("in update");

        for (JarInfo select : selectedJars) {
            logger.info(String.valueOf(select));
        }

        StringBuilder resultLog = new StringBuilder();

        for (JarInfo jar : selectedJars) {
            String command = String.format("mvn versions:use-dep-version -Dincludes=\"%s\" -DdepVersion=\"%s\"",
                    jar.getArtifact(), jar.getNewVersion());

            logger.info("Executing command: {}", command);
            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", "cd / && cd " + fpath.replace("\\", "/") + " && " + command);
            processBuilder.directory(new File(fpath)); // Set working directory to the specified path

            try {
                Process process = processBuilder.start();
                int exitCode = process.waitFor();

                logger.info(String.valueOf(exitCode));

                if (exitCode != 0) {
                    String errorMsg = readStream(process.getErrorStream());
                    logger.error("Failed to update dependency: {}. Exit code: {}. Error: {}", jar.getArtifact(), exitCode, errorMsg);
                    resultLog.append("Failed to update dependency: ").append(jar.getArtifact())
                            .append(". Exit code: ").append(exitCode)
                            .append(". Error: ").append(errorMsg).append("\n");
                    continue;  // Continue with the next jar even if this one fails
                }

                String output = readStream(process.getInputStream());
                logger.info("Updated {} to {}: \n{}", jar.getArtifact(), jar.getNewVersion(), output);
                resultLog.append("Updated ").append(jar.getArtifact()).append(" to ").append(jar.getNewVersion()).append("\n").append(output).append("\n");
            } catch (IOException | InterruptedException e) {
                logger.error("IOException while updating dependency: {}. Error: {}", jar.getArtifact(), e.getMessage());
                resultLog.append("IOException while updating dependency: ").append(jar.getArtifact())
                        .append(". Error: ").append(e.getMessage()).append("\n");
            }
        }

        logger.info("Dependency update completed.");
        return resultLog.toString();
    }


    private String readStream(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
        }
        return sb.toString();
    }

}
