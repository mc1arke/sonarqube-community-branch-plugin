package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.response;

import java.util.Calendar;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MergeRequest {
    private final long id;
    private final long iid;
    private final long projectId;
    private final String title;
    private final String description;
    private final String state;
    private final String createdAt;
    private final String updatedAt;
    private final User mergedBy;
    private final Calendar mergedAt;
    private final User closedBy;
    private final Calendar closedAt;
    private final String targetBranch;
    private final String sourceBranch;
    private final Long userNotesCount;
    private final Long upvotes;
    private final Long downvotes;
    private final User author;
    private final User assignee;
    private final Long sourceProjectId;
    private final Long targetProjectId;
    private final boolean workInProgress;
    private final boolean mergeWhenPipelineSucceeds;
    private final String mergeStatus;
    private final String sha;
    private final boolean forceRemoveSourceBranch;
    private final String reference;
    private final String webUrl;
    private final TimeStats timeStats;
    private final boolean squash;
    private final boolean subscribed;
    private final String changesCount;
    private final Calendar latestBuildStartedAt;
    private final Calendar latestBuildFinishedAt;
    private final Calendar firstDeployedToProductionAt;
    private final Pipeline pipeline;
    private final Pipeline headPipeline;
    private final DiffRefs diffRefs;
    private final Long approvalsBeforMerge;

    @JsonCreator
    public MergeRequest(
    		@JsonProperty("id") long id, 
    		@JsonProperty("iid") long iid, 
    		@JsonProperty("project_id") long projectId, 
    		@JsonProperty("title") String title, 
    		@JsonProperty("description") String description, 
    		@JsonProperty("state") String state,
    		@JsonProperty("created_at") String createdAt, 
    		@JsonProperty("updated_at") String updatedAt, 
    		@JsonProperty("merged_by") User mergedBy, 
    		@JsonProperty("merged_at") Calendar mergedAt, 
    		@JsonProperty("closed_by") User closedBy, 
    		@JsonProperty("closed_at") Calendar closedAt,
    		@JsonProperty("target_branch") String targetBranch, 
    		@JsonProperty("source_branch") String sourceBranch, 
    		@JsonProperty("user_notes_count") Long userNotesCount, 
    		@JsonProperty("upvotes") Long upvotes, 
    		@JsonProperty("downvotes") Long downvotes, 
    		@JsonProperty("author") User author,
    		@JsonProperty("assignee") User assignee, 
    		@JsonProperty("source_project_id") Long sourceProjectId, 
    		@JsonProperty("target_project_id") Long targetProjectId, 
    		@JsonProperty("work_in_progress") boolean workInProgress,
    		@JsonProperty("merge_when_pipeline_succeeds") boolean mergeWhenPipelineSucceeds, 
    		@JsonProperty("merge_status") String mergeStatus, 
    		@JsonProperty("sha") String sha, 
    		@JsonProperty("force_remove_source_branch") boolean forceRemoveSourceBranch,
    		@JsonProperty("reference") String reference, 
    		@JsonProperty("web_url") String webUrl, 
    		@JsonProperty("time_stats") TimeStats timeStats, 
    		@JsonProperty("squash") boolean squash, 
    		@JsonProperty("subscribed") boolean subscribed,
    		@JsonProperty("changes_count") String changesCount, 
    		@JsonProperty("latest_build_started_at") Calendar latestBuildStartedAt, 
    		@JsonProperty("latest_build_finished_at") Calendar latestBuildFinishedAt,
    		@JsonProperty("first_deployed_to_production_at") Calendar firstDeployedToProductionAt, 
    		@JsonProperty("pipeline") Pipeline pipeline, 
    		@JsonProperty("head_pipeline") Pipeline headPipeline, 
    		@JsonProperty("diff_refs") DiffRefs diffRefs,
    		@JsonProperty("approvals_before_merge") Long approvalsBeforMerge) {
		this.id = id;
		this.iid = iid;
		this.projectId = projectId;
		this.title = title;
		this.description = description;
		this.state = state;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.mergedBy = mergedBy;
		this.mergedAt = mergedAt;
		this.closedBy = closedBy;
		this.closedAt = closedAt;
		this.targetBranch = targetBranch;
		this.sourceBranch = sourceBranch;
		this.userNotesCount = userNotesCount;
		this.upvotes = upvotes;
		this.downvotes = downvotes;
		this.author = author;
		this.assignee = assignee;
		this.sourceProjectId = sourceProjectId;
		this.targetProjectId = targetProjectId;
		this.workInProgress = workInProgress;
		this.mergeWhenPipelineSucceeds = mergeWhenPipelineSucceeds;
		this.mergeStatus = mergeStatus;
		this.sha = sha;
		this.forceRemoveSourceBranch = forceRemoveSourceBranch;
		this.reference = reference;
		this.webUrl = webUrl;
		this.timeStats = timeStats;
		this.squash = squash;
		this.subscribed = subscribed;
		this.changesCount = changesCount;
		this.latestBuildStartedAt = latestBuildStartedAt;
		this.latestBuildFinishedAt = latestBuildFinishedAt;
		this.firstDeployedToProductionAt = firstDeployedToProductionAt;
		this.pipeline = pipeline;
		this.headPipeline = headPipeline;
		this.diffRefs = diffRefs;
		this.approvalsBeforMerge = approvalsBeforMerge;
	}

	public long getId() {
		return id;
	}

	public long getIid() {
		return iid;
	}

	public long getProjectId() {
		return projectId;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getState() {
		return state;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	public String getUpdatedAt() {
		return updatedAt;
	}

	public User getMergedBy() {
		return mergedBy;
	}

	public Calendar getMergedAt() {
		return mergedAt;
	}

	public User getClosedBy() {
		return closedBy;
	}

	public Calendar getClosedAt() {
		return closedAt;
	}

	public String getTargetBranch() {
		return targetBranch;
	}

	public String getSourceBranch() {
		return sourceBranch;
	}

	public Long getUserNotesCount() {
		return userNotesCount;
	}

	public Long getUpvotes() {
		return upvotes;
	}

	public Long getDownvotes() {
		return downvotes;
	}

	public User getAuthor() {
		return author;
	}

	public User getAssignee() {
		return assignee;
	}

	public Long getSourceProjectId() {
		return sourceProjectId;
	}

	public Long getTargetProjectId() {
		return targetProjectId;
	}

	public boolean isWorkInProgress() {
		return workInProgress;
	}

	public boolean isMergeWhenPipelineSucceeds() {
		return mergeWhenPipelineSucceeds;
	}

	public String getMergeStatus() {
		return mergeStatus;
	}

	public String getSha() {
		return sha;
	}

	public boolean isForceRemoveSourceBranch() {
		return forceRemoveSourceBranch;
	}

	public String getReference() {
		return reference;
	}

	public String getWebUrl() {
		return webUrl;
	}

	public TimeStats getTimeStats() {
		return timeStats;
	}

	public boolean isSquash() {
		return squash;
	}

	public boolean isSubscribed() {
		return subscribed;
	}

	public String getChangesCount() {
		return changesCount;
	}

	public Calendar getLatestBuildStartedAt() {
		return latestBuildStartedAt;
	}

	public Calendar getLatestBuildFinishedAt() {
		return latestBuildFinishedAt;
	}

	public Calendar getFirstDeployedToProductionAt() {
		return firstDeployedToProductionAt;
	}

	public Pipeline getPipeline() {
		return pipeline;
	}

	public Pipeline getHeadPipeline() {
		return headPipeline;
	}

	public DiffRefs getDiffRefs() {
		return diffRefs;
	}

	public Long getApprovalsBeforMerge() {
		return approvalsBeforMerge;
	}

}
