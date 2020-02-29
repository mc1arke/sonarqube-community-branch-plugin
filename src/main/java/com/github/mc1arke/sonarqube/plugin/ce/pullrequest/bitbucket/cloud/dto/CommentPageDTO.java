package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.cloud.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

public class CommentPageDTO implements Serializable {
    private final List<CommentDTO> comments;

    private int size;

    private String next;

    @JsonCreator
    public CommentPageDTO(@JsonProperty("values") List<CommentDTO> comments, @JsonProperty("size") int size, @JsonProperty("next") String next) {
        this.comments = comments;
        this.size = size;
        this.next = next;
    }

    public List<CommentDTO> getComments() {
        return comments;
    }

    public int getSize() {
        return size;
    }

    public String getNext() {
        return next;
    }
}
