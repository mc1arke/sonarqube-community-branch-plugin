package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response;

import java.io.Serializable;

public class Comment implements Serializable
{
    private int id;

    private int version;

    private String text;

    private User author;

    public Comment()
    {
        super();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }
}