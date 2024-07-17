package com.tplu.service;

import com.tplu.model.JarInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FileProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(FileProcessingService.class);
    private static final String START_LINE = "newer versions:";
    private static final Pattern JAR_INFO_PATTERN_SINGLE_LINE = Pattern.compile("\\[INFO\\]\\s*([^:]+):([^\\s]+)\\s+.*\\s([^\\s]+)\\s+->\\s+([^\\s]+)");
    private static final Pattern JAR_INFO_PATTERN_NEXT_LINE = Pattern.compile("\\[INFO\\]\\s*([^:]+):([^\\s]+)\\s+.*\\.{3}");

    // Method to process the POM file at the provided file path
    public void processPomFile(String filePath) throws IOException, InterruptedException {
        Path absolutePath = Paths.get(filePath).toAbsolutePath();
        logger.info("Processing POM file: {}", absolutePath);
        executeMavenCommand(((Path) absolutePath).toString());
    }

    private void executeMavenCommand(String filePath) throws IOException, InterruptedException {
        String[] command;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            command = new String[]{"cmd.exe", "/c", "mvn -f " + filePath + " versions:display-dependency-updates > test.txt"};
            logger.info(Arrays.toString(command));
        } else {
            command = new String[]{"/bin/sh", "-c", "mvn -f " + filePath + " versions:display-dependency-updates > test.txt"};
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File("C:/Users/gkumar12/IdeaProjects/ThirdPartyLibraryUpgrade_backend"));
        Process process = processBuilder.start();

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String errorMsg = readStream(process.getErrorStream());
            logger.error("Failed to execute Maven command. Exit code: {}. Error: {}", exitCode, errorMsg);
            throw new RuntimeException("Failed to execute Maven command. Exit code: " + exitCode + ". Error: " + errorMsg);
        }

        logger.info("Maven command executed successfully.");
    }

    public List<JarInfo> parseTestFile() {
        List<JarInfo> jarInfos = new ArrayList<>();
        Set<String> seenArtifacts = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new FileReader("test.txt"))) {
            String line;
            boolean startProcessing = false;
            String currentArtifact = null;

            while ((line = reader.readLine()) != null) {
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
                                currentArtifact = null;
                            }
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

    // Helper method to read stream contents to a string
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

    // Method to update dependencies based on selected JARs
    public String updateDependencies(List<JarInfo> selectedJars) throws IOException, InterruptedException {
        StringBuilder resultLog = new StringBuilder();

        for (JarInfo jar : selectedJars) {
            String command = String.format("mvnw.cmd -f \"%s\" versions:use-dep-version -Dincludes=\"%s\" -DdepVersion=\"%s\" ",
                    Paths.get("pom.xml").toAbsolutePath().toString(), jar.getArtifactId(), jar.getNewVersion());

            logger.info("Executing command: {}", command);
            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
            processBuilder.directory(new File("C:/Users/gkumar12/IdeaProjects/ThirdPartyLibraryUpgrade_backend"));
            Process process = processBuilder.start();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String errorMsg = readStream(process.getErrorStream());
                logger.error("Failed to update dependency: {}. Exit code: {}. Error: {}", jar.getArtifactId(), exitCode, errorMsg);
                throw new RuntimeException("Failed to update dependency: " + jar.getArtifactId() + ". Exit code: " + exitCode + ". Error: " + errorMsg);
            }

            String output = readStream(process.getInputStream());
            logger.info("Updated {} to {}: \n{}", jar.getArtifactId(), jar.getNewVersion(), output);
            resultLog.append("Updated ").append(jar.getArtifactId()).append(" to ").append(jar.getNewVersion()).append("\n").append(output).append("\n");
        }

        logger.info("Dependency update completed.");
        return resultLog.toString();
    }
}
