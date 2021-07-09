package com.github.mc1arke.sonarqube.plugin.almclient.azuredevops.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Status context that uniquely identifies the status.
 */
public class GitStatusContext {
    
    private final String name;
    private final String genre;

    @JsonCreator
    public GitStatusContext(@JsonProperty("genre") String genre, @JsonProperty("name") String name){
        this.genre = genre;
        this.name = name;
    }

    /**
     *  Genre of the status. Typically name of the service/tool generating the status, can be empty.
     */
    public String getGenre() {
        return this.genre;
    }

    /**
     * Name identifier of the status, cannot be null or empty.
     */
    public String getName() {
        return this.name;
    }
}
