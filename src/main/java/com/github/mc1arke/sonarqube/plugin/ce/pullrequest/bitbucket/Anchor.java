package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.server.BitbucketServerPullRequestDecorator;

import java.io.Serializable;

public class Anchor implements Serializable
{
    private int line;

    private String lineType;

    private String path;

    private String fileType = "TO";

    public Anchor()
    {
        super();
    }

    public Anchor(int line, String lineType, String path, String fileType) {
        this.line = line;
        this.lineType = lineType;
        this.path = path;
        this.fileType = fileType;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public String getLineType() {
        return lineType;
    }

    public void setLineType(String lineType) {
        this.lineType = lineType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
}