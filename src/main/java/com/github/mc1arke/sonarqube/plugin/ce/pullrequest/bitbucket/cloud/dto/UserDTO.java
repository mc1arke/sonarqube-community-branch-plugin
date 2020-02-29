package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.cloud.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class UserDTO implements Serializable {
    private final String uuid;

    @JsonCreator
    public UserDTO(@JsonProperty("uuid") String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }
}
