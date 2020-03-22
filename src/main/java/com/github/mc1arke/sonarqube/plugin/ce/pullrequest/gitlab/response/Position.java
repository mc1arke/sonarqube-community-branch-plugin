package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Position {
	
    private final String baseSha;
    private final String startSha;
    private final String headSha;
    private final String oldPath;
    private final String newPath;
    private final String positionType;
    private final String oldLine;
    private final String newLine;

    @JsonCreator
    public Position(
    		@JsonProperty("base_sha") String baseSha, 
    		@JsonProperty("start_sha") String startSha, 
    		@JsonProperty("head_sha") String headSha, 
    		@JsonProperty("old_path") String oldPath, 
    		@JsonProperty("new_path") String newPath, 
    		@JsonProperty("position_type") String positionType, 
    		@JsonProperty("old_line") String oldLine, 
    		@JsonProperty("new_line") String newLine) {
		this.baseSha = baseSha;
		this.startSha = startSha;
		this.headSha = headSha;
		this.oldPath = oldPath;
		this.newPath = newPath;
		this.positionType = positionType;
		this.oldLine = oldLine;
		this.newLine = newLine;
	}

	public String getBaseSha() {
		return baseSha;
	}

	public String getStartSha() {
		return startSha;
	}

	public String getHeadSha() {
		return headSha;
	}

	public String getOldPath() {
		return oldPath;
	}

	public String getNewPath() {
		return newPath;
	}

	public String getPositionType() {
		return positionType;
	}

	public String getOldLine() {
		return oldLine;
	}

	public String getNewLine() {
		return newLine;
	}

}

