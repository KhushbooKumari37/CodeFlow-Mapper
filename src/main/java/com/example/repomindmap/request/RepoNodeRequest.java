package com.example.repomindmap.request;

public class RepoNodeRequest {
    private String repoUrl;
    private String nodeKey;

    // Getters and Setters
    public String getRepoUrl() {
        return repoUrl;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }

    public String getNodeKey() {
        return nodeKey;
    }

    public void setNodeKey(String nodeKey) {
        this.nodeKey = nodeKey;
    }
}
