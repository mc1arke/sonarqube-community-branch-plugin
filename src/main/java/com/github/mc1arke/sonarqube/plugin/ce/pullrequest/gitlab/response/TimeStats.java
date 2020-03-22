package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.response;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TimeStats {
	
	private final BigDecimal timeEstimate;
	private final BigDecimal totalTimeSpent;
	 
	@JsonCreator
	public TimeStats(
			@JsonProperty("time_estimate") BigDecimal timeEstimate, 
			@JsonProperty("total_time_spent") BigDecimal totalTimeSpent) {
		this.timeEstimate = timeEstimate;
		this.totalTimeSpent = totalTimeSpent;
	}

	public BigDecimal getTimeEstimate() {
		return timeEstimate;
	}

	public BigDecimal getTotalTimeSpent() {
		return totalTimeSpent;
	}
 
}
