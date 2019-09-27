package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.diff;

import java.io.Serializable;
import java.util.List;

public class Line implements Serializable
{
    private int source;

    private int destination;

    private String line;

    private boolean truncated;

    private List<Integer> commentIds;

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public int getDestination() {
        return destination;
    }

    public void setDestination(int destination) {
        this.destination = destination;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }

    public List<Integer> getCommentIds() {
        return commentIds;
    }

    public void setCommentIds(List<Integer> commentIds) {
        this.commentIds = commentIds;
    }
}