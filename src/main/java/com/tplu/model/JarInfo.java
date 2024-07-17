package com.tplu.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JarInfo {
    private String artifactId;
    private String currentVersion;
    private String newVersion;


    public JarInfo() {
    }

    // Constructor
    @JsonCreator
    public JarInfo(@JsonProperty("artifactId") String artifactId,
                   @JsonProperty("currentVersion") String currentVersion,
                   @JsonProperty("newVersion") String newVersion) {
        this.artifactId = artifactId;
        this.currentVersion = currentVersion;
        this.newVersion = newVersion;
    }

    // Getters and setters
    public String getArtifactId() { return artifactId; }
    public void setArtifactId(String artifactId) { this.artifactId = artifactId; }

    public String getCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(String currentVersion) { this.currentVersion = currentVersion; }

    public String getNewVersion() { return newVersion; }
    public void setNewVersion(String newVersion) { this.newVersion = newVersion; }
}
