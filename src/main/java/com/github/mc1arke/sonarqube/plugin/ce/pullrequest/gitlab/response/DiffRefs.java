package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DiffRefs {
    private final String baseSha;
    private final String headSha;
    private final String startSha;

    public DiffRefs(@JsonProperty("base_sha") String baseSha, @JsonProperty("head_sha") String headSha, @JsonProperty("start_sha") String startSha) {
        this.baseSha = baseSha;
        this.headSha = headSha;
        this.startSha = startSha;
    }

    public String getBaseSha() {
        return baseSha;
    }

    public String getHeadSha() {
        return headSha;
    }

    public String getStartSha() {
        return startSha;
    }
}
