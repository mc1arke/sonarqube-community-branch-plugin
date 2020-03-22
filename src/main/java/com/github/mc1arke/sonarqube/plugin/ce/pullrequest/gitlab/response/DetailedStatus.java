package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DetailedStatus {
	 private final String icon;
	 private final String text;
	 private final String label;
	 private final String group;
	 private final String tooltip;
	 private final boolean hasDetails;
	 private final String detailsPath;
	 private final String favicon;
	 
	 @JsonCreator
	 public DetailedStatus(
			@JsonProperty("icon") String icon, 
			@JsonProperty("text") String text, 
			@JsonProperty("label") String label, 
			@JsonProperty("group") String group, 
			@JsonProperty("tooltip") String tooltip, 
			@JsonProperty("has_details") boolean hasDetails,
			@JsonProperty("details_path") String detailsPath, 
			@JsonProperty("favicon") String favicon) {
		this.icon = icon;
		this.text = text;
		this.label = label;
		this.group = group;
		this.tooltip = tooltip;
		this.hasDetails = hasDetails;
		this.detailsPath = detailsPath;
		this.favicon = favicon;
	}

	public String getIcon() {
		return icon;
	}

	public String getText() {
		return text;
	}

	public String getLabel() {
		return label;
	}

	public String getGroup() {
		return group;
	}

	public String getTooltip() {
		return tooltip;
	}

	public boolean isHasDetails() {
		return hasDetails;
	}

	public String getDetailsPath() {
		return detailsPath;
	}

	public String getFavicon() {
		return favicon;
	}

}
