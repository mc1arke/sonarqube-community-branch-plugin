package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.response;

import java.util.Calendar;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Pipeline {

	 private final long id;
	 private final String sha;
	 private final String ref;
	 private final String status;
	 private final String webUrl;
	 private final String beforeSha;
	 private final boolean tag;
	 private final User user;
	 private final Calendar createdAt;
	 private final Calendar updatedAt;
	 private final Calendar startedAt;
	 private final Calendar finishedAt;
	 private final Calendar committedAt;
	 private final String coverage;
	 private final DetailedStatus detailedStatus;
	
	 public Pipeline(
			 @JsonProperty("id") long id, 
			 @JsonProperty("sha") String sha, 
			 @JsonProperty("ref") String ref, 
			 @JsonProperty("status") String status, 
			 @JsonProperty("web_url") String webUrl, 
			 @JsonProperty("before_sha") String beforeSha, 
			 @JsonProperty("tag") boolean tag,
			 @JsonProperty("user") User user, 
			 @JsonProperty("created_at") Calendar createdAt, 
			 @JsonProperty("updated_at") Calendar updatedAt, 
			 @JsonProperty("started_at") Calendar startedAt, 
			 @JsonProperty("finished_at") Calendar finishedAt,
			 @JsonProperty("committed_at") Calendar committedAt, 
			 @JsonProperty("coverage") String coverage, 
			 @JsonProperty("detailed_status") DetailedStatus detailedStatus) {
		this.id = id;
		this.sha = sha;
		this.ref = ref;
		this.status = status;
		this.webUrl = webUrl;
		this.beforeSha = beforeSha;
		this.tag = tag;
		this.user = user;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.startedAt = startedAt;
		this.finishedAt = finishedAt;
		this.committedAt = committedAt;
		this.coverage = coverage;
		this.detailedStatus = detailedStatus;
	}

	public long getId() {
		return id;
	}

	public String getSha() {
		return sha;
	}

	public String getRef() {
		return ref;
	}

	public String getStatus() {
		return status;
	}

	public String getWebUrl() {
		return webUrl;
	}

	public String getBeforeSha() {
		return beforeSha;
	}

	public boolean isTag() {
		return tag;
	}

	public User getUser() {
		return user;
	}

	public Calendar getCreatedAt() {
		return createdAt;
	}

	public Calendar getUpdatedAt() {
		return updatedAt;
	}

	public Calendar getStartedAt() {
		return startedAt;
	}

	public Calendar getFinishedAt() {
		return finishedAt;
	}

	public Calendar getCommittedAt() {
		return committedAt;
	}

	public String getCoverage() {
		return coverage;
	}

	public DetailedStatus getDetailedStatus() {
		return detailedStatus;
	}

}
