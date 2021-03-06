package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MergeRequest {
    private final String id;
    private final String iid;
    private final DiffRefs diffRefs;
    private final String sourceProjectId;

    public MergeRequest(@JsonProperty("id") String id, @JsonProperty("iid") String iid, @JsonProperty("diff_refs") DiffRefs diffRefs, @JsonProperty("source_project_id") String sourceProjectId) {
        this.id = id;
        this.iid = iid;
        this.diffRefs = diffRefs;
        this.sourceProjectId = sourceProjectId;
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

    public String getSourceProjectId() {
        return sourceProjectId;
    }
}
