package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.diff;

import java.io.Serializable;
import java.util.List;

public class Diff implements Serializable
{
    private String fromHash;

    private String toHash;

    private List<Hunk> hunks;

    private File source;

    private File destination;

    public String getFromHash() {
        return fromHash;
    }

    public void setFromHash(String fromHash) {
        this.fromHash = fromHash;
    }

    public String getToHash() {
        return toHash;
    }

    public void setToHash(String toHash) {
        this.toHash = toHash;
    }

    public List<Hunk> getHunks() {
        return hunks;
    }

    public void setHunks(List<Hunk> hunks) {
        this.hunks = hunks;
    }

    public File getSource() {
        return source;
    }

    public void setSource(File source) {
        this.source = source;
    }

    public File getDestination() {
        return destination;
    }

    public void setDestination(File destination) {
        this.destination = destination;
    }
}