package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket;

import java.io.Serializable;

public class SummaryComment implements Serializable
{
    private String text;

    public SummaryComment()
    {
        super();
    }

    public SummaryComment(String text)
    {
        super();
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}