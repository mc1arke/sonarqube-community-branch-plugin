package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.activity;

import java.io.Serializable;

public class ActivityPage implements Serializable
{
    private int size;

    private int limit;

    private boolean isLastPage;

    private int start;

    private int nextPageStart;

    private Activity[] values;

    public ActivityPage()
    {
        super();
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public boolean isLastPage() {
        return isLastPage;
    }

    public void setLastPage(boolean lastPage) {
        isLastPage = lastPage;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getNextPageStart() {
        return nextPageStart;
    }

    public void setNextPageStart(int nextPageStart) {
        this.nextPageStart = nextPageStart;
    }

    public Activity[] getValues() {
        return values;
    }

    public void setValues(Activity[] values) {
        this.values = values;
    }
}