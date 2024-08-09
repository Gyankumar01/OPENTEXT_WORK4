package com.tplu.model;

import java.util.List;
import java.util.Map;

public class JarInfo {
    private String artifact;
    private String currentVersion;
    private String newVersion;
    private String olderVersion;
    private List<String> availableVersions;
    private String propertyName;
    private Map<String, String> artifactPropertyMap; // New field for artifact to property mapping

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getOlderVersion() {
        return olderVersion;
    }

    public void setOlderVersion(String olderVersion) {
        this.olderVersion = olderVersion;
    }

    public JarInfo() {
    }

    public JarInfo(String artifact, String currentVersion, String newVersion, String olderVersion) {
        this.artifact = artifact;
        this.currentVersion = currentVersion;
        this.newVersion = newVersion;
        this.olderVersion = olderVersion;
    }

    public JarInfo(String artifact, String currentVersion, String newVersion, List<String> availableVersions, String olderVersion) {
        this.artifact = artifact;
        this.currentVersion = currentVersion;
        this.newVersion = newVersion;
        this.availableVersions = availableVersions;
        this.olderVersion = olderVersion;
    }

    public String getArtifact() {
        return artifact;
    }

    public void setArtifact(String artifact) {
        this.artifact = artifact;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public String getNewVersion() {
        return newVersion;
    }

    public void setNewVersion(String newVersion) {
        this.newVersion = newVersion;
    }

    public List<String> getAvailableVersions() {
        return availableVersions;
    }

    public void setAvailableVersions(List<String> availableVersions) {
        this.availableVersions = availableVersions;
    }

    public Map<String, String> getArtifactPropertyMap() {
        return artifactPropertyMap;
    }

    public void setArtifactPropertyMap(Map<String, String> artifactPropertyMap) {
        this.artifactPropertyMap = artifactPropertyMap;
    }

    @Override
    public String toString() {
        return "JarInfo{" +
                "artifact='" + artifact + '\'' +
                ", currentVersion='" + currentVersion + '\'' +
                ", newVersion='" + newVersion + '\'' +
                ", olderVersion='" + olderVersion + '\'' +
                ", availableVersions=" + availableVersions +
                ", artifactPropertyMap=" + artifactPropertyMap +
                '}';
    }
}
