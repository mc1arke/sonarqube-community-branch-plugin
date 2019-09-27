package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.activity;

import java.io.Serializable;

public class Activity implements Serializable
{
    private int id;

    private User user;

    private Comment comment;

    public Activity()
    {
        super();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Comment getComment() {
        return comment;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }
}