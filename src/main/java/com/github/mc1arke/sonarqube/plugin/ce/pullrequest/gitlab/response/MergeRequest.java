package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MergeRequest {
    private final String id;
    private final String iid;
    private final DiffRefs diffRefs;

    public MergeRequest(@JsonProperty("id") String id, @JsonProperty("iid") String iid, @JsonProperty("diff_refs") DiffRefs diffRefs) {
        this.id = id;
        this.iid = iid;
        this.diffRefs = diffRefs;
    }

    public String getId() {
        return id;
    }

    public String getIid() {
        return iid;
    }

    public DiffRefs getDiffRefs() {
        return diffRefs;
    }
}
