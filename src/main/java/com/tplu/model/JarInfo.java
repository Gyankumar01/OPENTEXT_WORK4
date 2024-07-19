package com.tplu.model;

public class JarInfo {
    private String artifact;
    private String currentVersion;
    private String newVersion;


    public JarInfo(String artifact, String currentVersion, String newVersion) {
        this.artifact = artifact;
        this.currentVersion = currentVersion;
        this.newVersion = newVersion;
    }

    public JarInfo() {
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
}
