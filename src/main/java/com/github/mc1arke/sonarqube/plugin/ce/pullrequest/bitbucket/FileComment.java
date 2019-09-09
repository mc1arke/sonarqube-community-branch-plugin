package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket;

import java.io.Serializable;

public class FileComment implements Serializable
{
    private String text;

    private Anchor anchor;

    public FileComment()
    {
        super();
    }

    public FileComment(String text)
    {
        super();
        this.text = text;
    }

    public FileComment(String text, Anchor anchor) {
        this.text = text;
        this.anchor = anchor;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Anchor getAnchor() {
        return anchor;
    }

    public void setAnchor(Anchor anchor) {
        this.anchor = anchor;
    }
}