package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.cloud.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentDTO implements Serializable {
    private final String id;
    private final ContentDTO content;
    private final InlineDTO inline;
    private final UserDTO user;

    @JsonCreator
    public CommentDTO(@JsonProperty("id") String id, @JsonProperty("content") ContentDTO content, @JsonProperty("inline") InlineDTO inline, @JsonProperty("user") UserDTO user) {
        this.id = id;
        this.content = content;
        this.inline = inline;
        this.user = user;
    }

    public ContentDTO getContent() {
        return content;
    }

    public InlineDTO getInline() {
        return inline;
    }

    public UserDTO getUser() {
        return user;
    }

    public String getId() {
        return id;
    }
}
