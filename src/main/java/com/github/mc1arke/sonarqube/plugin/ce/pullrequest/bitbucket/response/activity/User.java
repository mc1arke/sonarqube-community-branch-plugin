package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.activity;

import java.io.Serializable;

public class User implements Serializable
{
    private String name;

    private String slug;

    public User()
    {
        super();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
}