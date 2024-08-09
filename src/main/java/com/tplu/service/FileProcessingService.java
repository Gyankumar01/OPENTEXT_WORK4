package com.tplu.service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenStrategyStage;
import org.jboss.shrinkwrap.resolver.api.maven.MavenWorkingSession;
import java.io.File;
import java.util.List;
import com.tplu.model.JarInfo;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenDependency;
import org.jboss.shrinkwrap.resolver.impl.maven.MavenWorkingSessionContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.apache.maven.model.Dependency;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
@Service
public class FileProcessingService {
    private static final Logger logger = LoggerFactory.getLogger(FileProcessingService.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private static final String RepoUrl = "https://search.maven.org/solrsearch/select?q=g:%s+AND+a:%s&core=gav&rows=25&wt=json";
    private static final Pattern CurrentLine = Pattern.compile("\\[INFO\\]\\s*([^:]+):([^\\s]+)\\s+.*\\s([^\\s]+)\\s+->\\s+([^\\s]+)");
    private static final Pattern NextLine = Pattern.compile("\\[INFO\\]\\s*([^:]+):([^\\s]+)\\s+.*\\.{3}");
    private static final Pattern SameLinePattern = Pattern.compile("\\[INFO\\]\\s*([^:]+):([^\\s]+)\\s+.*\\s([^\\s]+)\\s+->\\s+([^\\s]+)");
    private static final Pattern NextLinePattern = Pattern.compile("\\[INFO\\]\\s*([^:]+):([^\\s]+)\\s+.*\\.{3}");
    private static final Pattern VersionOnNextLinePattern = Pattern.compile("\\s*([^\\s]+)\\s+->\\s+([^\\s]+)");
    private String fpath = "";
    private String pomPath = "";
    public List<JarInfo> processFilePath(String filePath) {
        fpath = filePath;
        logger.info("Processing file path: {}", filePath);
        List<JarInfo> jarInfos = new ArrayList<>();
        Set<String> seenArtifacts = new HashSet<>();

        try {

            String pomFilePath = filePath + "/pom.xml"; // Update with your pom.xml path
            Map<String, String> artifactToPropertyMap = parsePomFile(pomFilePath);

            // Adjust command to ignore parent POM and process current POM
            String command = String.format("mvn versions:display-dependency-updates > %s/test.txt", filePath.replace("\\", "/"));
            logger.info("Executing command: {}", command);

            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
            processBuilder.directory(new File(filePath));
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info("Process output: {}", line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                logger.error("Error executing command. Exit code: {}", exitCode);
                return jarInfos;
            }

            File testFile = new File(filePath, "test.txt");
            if (!testFile.exists()) {
                logger.error("File test.txt does not exist in the specified path.");
                return jarInfos;
            }

            try (BufferedReader fileReader = new BufferedReader(new FileReader(testFile))) {
                String line;
                boolean startProcessing = false;
                String currentArtifact = null;

                while ((line = fileReader.readLine()) != null) {
                    logger.info("Reading line from test.txt: {}", line);
                    if (line.contains("newer versions:")) {
                        startProcessing = true;
                        continue;
                    }

                    if (startProcessing) {
                        Matcher matcherSingleLine = CurrentLine.matcher(line);
                        Matcher matcherNextLine = NextLine.matcher(line);

                        if (matcherSingleLine.find()) {
                            String group1 = matcherSingleLine.group(1).trim();
                            String group2 = matcherSingleLine.group(2).trim();
                            String artifact = group1 + ":" + group2;
                            String currentVersion = matcherSingleLine.group(3).trim();
                            String newVersion = matcherSingleLine.group(4).trim();

                            if (seenArtifacts.add(artifact)) {
                                String propertyName = artifactToPropertyMap.getOrDefault(group2, null);
                                JarInfo jarInfo = new JarInfo(artifact, currentVersion, newVersion, propertyName);
                                jarInfos.add(jarInfo);
                                logger.info("Added JarInfo: {}", jarInfo);
                            }
                        } else if (matcherNextLine.find()) {
                            currentArtifact = matcherNextLine.group(1).trim() + ":" + matcherNextLine.group(2).trim();
                        } else if (currentArtifact != null) {
                            Matcher versionMatcher = Pattern.compile("\\s*([^\\s]+)\\s+->\\s+([^\\s]+)").matcher(line);
                            if (versionMatcher.find()) {
                                String currentVersion = versionMatcher.group(1).trim();
                                String newVersion = versionMatcher.group(2).trim();

                                if (seenArtifacts.add(currentArtifact)) {
                                    String artifactId = currentArtifact.split(":")[1];
                                    String propertyName = artifactToPropertyMap.getOrDefault(artifactId, null);
                                    JarInfo jarInfo = new JarInfo(currentArtifact, currentVersion, newVersion, propertyName);
                                    jarInfos.add(jarInfo);
                                    logger.info("Added JarInfo: {}", jarInfo);
                                    currentArtifact = null;
                                }
                            }
                        }
                    }
                }
            }

        } catch (IOException | InterruptedException e) {
            logger.error("Error processing file path: ", e);
        }
        logger.info("Finished processing file path. JarInfos: {}", jarInfos);
        fetchAndSetVersions(jarInfos);
        //  executorService.shutdown();
        return jarInfos;
    }
    private Map<String, String> parsePomFile(String pomFilePath) {
        pomPath = pomFilePath;
        Map<String, String> artifactToPropertyMap = new HashMap<>();
        try {
            File pomFile = new File(pomFilePath);

            List<MavenDependency> dependencies = getDependency(pomFile);

            MavenXpp3Reader reader = new MavenXpp3Reader();
            FileReader fileReader = new FileReader(pomFilePath);

            Model model = reader.read(fileReader);

            model.getDependencies().forEach(dependency -> {
                System.out.println("Dependency: "
                        + dependency.getGroupId() +
                        ":"
                        + dependency.getArtifactId() +
                        ":"
                        + dependency.getVersion());
            });

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(pomFile);
            doc.getDocumentElement().normalize();

            NodeList dependencyManagementNodes = doc.getElementsByTagName("dependencyManagement");
            if (dependencyManagementNodes.getLength() > 0) {
                Node dependencyManagementNode = dependencyManagementNodes.item(0);
                NodeList dependencies1 = dependencyManagementNode.getChildNodes();

                for (int i = 0; i < dependencies1.getLength(); i++) {
                    Node dependencyNode = dependencies1.item(i);
                    if (dependencyNode.getNodeName().equals("dependencies")) {
                        NodeList dependencyNodes = dependencyNode.getChildNodes();
                        for (int j = 0; j < dependencyNodes.getLength(); j++) {
                            Node node = dependencyNodes.item(j);
                            if (node.getNodeName().equals("dependency")) {
                                String groupId = null;
                                String artifactId = null;
                                String propertyName = null;

                                NodeList childNodes = node.getChildNodes();
                                for (int k = 0; k < childNodes.getLength(); k++) {
                                    Node childNode = childNodes.item(k);
                                    if ("groupId".equals(childNode.getNodeName())) {
                                        groupId = childNode.getTextContent();
                                    } else if ("artifactId".equals(childNode.getNodeName())) {
                                        artifactId = childNode.getTextContent();
                                    } else if ("version".equals(childNode.getNodeName())) {
                                        String versionText = childNode.getTextContent();
                                        if (versionText.startsWith("${") && versionText.endsWith("}")) {
                                            propertyName = versionText.substring(2, versionText.length() - 1); // Remove ${ and }
                                        }
                                    }
                                }

                                if (groupId != null && artifactId != null && propertyName != null) {
                                    artifactToPropertyMap.put(artifactId, propertyName);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing POM file: ", e);
        }
        return artifactToPropertyMap;
    }
    List<MavenDependency> getDependency(File pomFile) {
        MavenStrategyStage resolve =
                Maven.configureResolver()
                        .workOffline()
                        .loadPomFromFile(pomFile)
                        .importCompileAndRuntimeDependencies()
                        .importRuntimeAndTestDependencies()
                        .resolve();

        MavenWorkingSession mavenWorkingSession =
                ((MavenWorkingSessionContainer) resolve).getMavenWorkingSession();

        List<MavenDependency> dependencies = new ArrayList<>();
        dependencies.addAll(mavenWorkingSession.getDependenciesForResolution());
        dependencies.addAll(mavenWorkingSession.getDependencyManagement());

        return dependencies;
    }
    private List<String> fetchVersionsFromAPI(String groupId, String artifactId) {
        List<String> versions = new ArrayList<>();
        String apiUrl = String.format(RepoUrl, groupId, artifactId);
        ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode docs = root.path("response").path("docs");

                for (JsonNode doc : docs) {
                    String version = doc.path("v").asText();
                    versions.add(version);
                }
            } catch (Exception e) {
                logger.error("Error parsing API response for artifact: {}:{}", groupId, artifactId, e);
            }
        } else {
            logger.error("Failed to fetch versions for artifact: {}:{}", groupId, artifactId);
        }

        return versions;
    }
    private void fetchAndSetVersions(List<JarInfo> jarInfos) {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        List<Future<Void>> futures = new ArrayList<>();

        for (JarInfo jarInfo : jarInfos) {
            futures.add(executorService.submit(() -> {
                String[] parts = jarInfo.getArtifact().split(":");
                String groupId = parts[0];
                String artifactId = parts[1];

                try {
                    if (groupId.startsWith("com.emc") || groupId.startsWith("com.opentext")) {
                        // No API call, read from test.txt
                        List<String> versions = fetchVersionsFromFile(jarInfo.getArtifact());
                        if (!versions.isEmpty()) {
                            jarInfo.setAvailableVersions(versions);
                            // Set the first available version as the new version
                            jarInfo.setNewVersion(versions.get(0));
                            logger.info("Available versions found for artifact {}: {}", jarInfo.getArtifact(), versions);
                        } else {
                            logger.info("No available versions found for artifact: {}", jarInfo.getArtifact());
                        }
                    } else {
                        // Make API call for other dependencies
                        List<String> versions = fetchVersionsFromAPI(groupId, artifactId);
                        jarInfo.setAvailableVersions(versions);
                        // Set the first available version as the new version
                        if (!versions.isEmpty()) {
                            jarInfo.setNewVersion(versions.get(0));
                            logger.info("New version for artifact {}: {}", jarInfo.getArtifact(), jarInfo.getNewVersion());
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error fetching versions for artifact: {}", jarInfo.getArtifact(), e);
                }

                return null;
            }));
        }

        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Error waiting for future to complete", e);
            }
        }


        // shutdown executor Service
        executorService.shutdown();
        try{
            if(!executorService.awaitTermination(60,TimeUnit.SECONDS)){
                executorService.shutdown();
            }
            if(!executorService.awaitTermination(60,TimeUnit.SECONDS)){
                logger.error("Executor Service did not close");
            }
        }
        catch(InterruptedException e){
            executorService.shutdown();
            Thread.currentThread().interrupt();
        }

    }
    private List<String> fetchVersionsFromFile(String artifact) {
        List<String> versions = new ArrayList<>();
        String versionsFilePath = fpath + "/test.txt";
        File versionsFile = new File(versionsFilePath);

        if (versionsFile.exists()) {
            try (BufferedReader fileReader = new BufferedReader(new FileReader(versionsFile))) {
                String line;
                String currentArtifact = null;

                logger.info("Reading versions from file: {}", versionsFilePath);

                while ((line = fileReader.readLine()) != null) {
                    line = line.trim(); // Trim leading and trailing whitespace
                    logger.debug("Processing line: {}", line);

                    Matcher sameLineMatcher = SameLinePattern.matcher(line);
                    Matcher nextLineMatcher = NextLinePattern.matcher(line);
                    Matcher versionMatcher = VersionOnNextLinePattern.matcher(line);

                    if (sameLineMatcher.find()) {
                        // Check if the same line contains the artifact information
                        String artifactFromFile = sameLineMatcher.group(1).trim() + ":" + sameLineMatcher.group(2).trim();
                        logger.debug("Found same line match for artifact: {}", artifactFromFile);
                        if (artifactFromFile.equals(artifact)) {
                            String newVersion = sameLineMatcher.group(4).trim();
                            logger.debug("Adding new version: {}", newVersion);
                            versions.add(newVersion);
                        }
                    } else if (nextLineMatcher.find()) {
                        // Handle the case where version info is on the next line
                        currentArtifact = nextLineMatcher.group(1).trim() + ":" + nextLineMatcher.group(2).trim();
                        logger.debug("Found next line match for artifact: {}", currentArtifact);
                    } else if (currentArtifact != null) {
                        // Check if the currentArtifact matches the jarInfo artifact
                        if (currentArtifact.equals(artifact)) {
                            if (versionMatcher.find()) {
                                String newVersion = versionMatcher.group(2).trim();
                                logger.debug("Adding new version from next line: {}", newVersion);
                                versions.add(newVersion);
                                currentArtifact = null; // Reset after processing
                            }
                        }
                    }
                }
            } catch (IOException e) {
                logger.error("Error reading versions file: {}", versionsFilePath, e);
            }
        } else {
            logger.error("Versions file does not exist at path: {}", versionsFilePath);
        }

        return versions;
    }
    public String updateDependencies(List<JarInfo> selectedJars, String path) throws IOException, InterruptedException, XmlPullParserException {
        path = pomPath;
        MavenXpp3Reader reader1 = new MavenXpp3Reader();
        FileReader fileReader = new FileReader(path);
        Model model = reader1.read(fileReader);

        // Create a map for artifact to property name
        Map<String, String> artifactPropertyMap = new HashMap<>();

        if (model.getDependencyManagement() != null) {
            for (Dependency dependency : model.getDependencyManagement().getDependencies()) {
                String artifactKey = dependency.getGroupId() + ":" + dependency.getArtifactId();
                artifactPropertyMap.put(artifactKey, dependency.getVersion());
            }
        } else {
            logger.warn("DependencyManagement is null");
        }

        for (Dependency dependency : model.getDependencies()) {
            String artifactKey = dependency.getGroupId() + ":" + dependency.getArtifactId();
            artifactPropertyMap.put(artifactKey, dependency.getVersion());
        }

        for (JarInfo jar : selectedJars) {
            jar.setArtifactPropertyMap(artifactPropertyMap);
        }

        Properties properties = model.getProperties();
        logger.info("In update");
        StringBuilder resultLog = new StringBuilder();

        for (JarInfo jar : selectedJars) {
            String versionToUse = jar.getNewVersion() != null ? jar.getNewVersion() : jar.getOlderVersion();
            String artifactIdPart = jar.getArtifact().split(":")[1];
            logger.info("Processing JarInfo: {}", jar); // Log the JarInfo object
            logger.info("Version to use: {}", versionToUse);

            // Extracting property name without ${}
            String propertyNameWithBraces = jar.getArtifactPropertyMap().get(jar.getArtifact());
            logger.info("Property name with braces: {}", propertyNameWithBraces);

            boolean isPropertyNameAValidVersion = propertyNameWithBraces != null && propertyNameWithBraces.matches("\\d+\\.\\d+\\.\\d+");

            if (propertyNameWithBraces == null || isPropertyNameAValidVersion) {
                // Invalid property name or null, run the alternative command
                String command = String.format(
                        "mvn versions:use-dep-version -Dincludes=\"%s\" -DdepVersion=\"%s\"",
                        jar.getArtifact(), versionToUse
                );

                logger.info("Executing alternative command: {}", command);

                ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
                processBuilder.directory(new File(fpath));
                Process process = processBuilder.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        resultLog.append(line).append("\n");
                    }
                }

                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    logger.info("Dependency {} updated successfully to version: {}", jar.getArtifact(), versionToUse);
                } else {
                    logger.error("Error updating dependency for {}: Exit code: {}", jar.getArtifact(), exitCode);
                    logger.error("Error log: {}", resultLog.toString());
                }
            } else {
                String propertyName = propertyNameWithBraces.replaceAll("[${}]", "");
                logger.info("Updating dependency: {} to version: {}", jar.getArtifact(), versionToUse);
                logger.info("Using property name: {}", propertyName);

                String command = String.format(
                        "mvn versions:set-property -Dproperty=\"%s\" -DnewVersion=\"%s\"",
                        propertyName, versionToUse
                );

                logger.info("Executing command: {}", command);

                ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
                processBuilder.directory(new File(fpath));
                Process process = processBuilder.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        resultLog.append(line).append("\n");
                    }
                }

                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    logger.info("Property {} updated successfully to version: {}", propertyName, versionToUse);
                } else {
                    logger.error("Error updating property for {}: Exit code: {}", jar.getArtifact(), exitCode);
                    logger.error("Error log: {}", resultLog.toString());
                }
            }
        }

        return resultLog.toString();
    }
}