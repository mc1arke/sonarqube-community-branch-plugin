package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model;

/**
 * Status context that uniquely identifies the status.
 */
public class GitStatusContext {
    /**
     * Name identifier of the status, cannot be null or empty.
     */
    public final String name;
    /**
     *  Genre of the status. Typically name of the service/tool generating the status, can be empty.
     */
    public final String genre;
    public GitStatusContext(String genre, String name){
        this.genre = genre;
        this.name = name;
    }
}
