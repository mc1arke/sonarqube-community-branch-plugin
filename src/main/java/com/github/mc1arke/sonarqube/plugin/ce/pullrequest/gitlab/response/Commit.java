/*
 * Copyright (C) 2019 Markus Heberling
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.response;

import java.util.Calendar;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Commit {
    private final String id;
    private final String shortId;
    private final Calendar createdAt;
    private final String title;
    private final String message;
    private final String authorName;
    private final String authorEmail;
    private final Calendar authoredDate;
    private final String committerName;
    private final String committerEmail;
    private final Calendar committedDate;

	@JsonCreator
    public Commit(
    		 @JsonProperty("id") String id, 
    		 @JsonProperty("short_id") String shortId, 
    		 @JsonProperty("created_at") Calendar createdAt, 
    		 @JsonProperty("title") String title,  
    		 @JsonProperty("message") String message, 
    		 @JsonProperty("author_name") String authorName,
    		 @JsonProperty("author_email") String authorEmail, 
    		 @JsonProperty("authored_date") Calendar authoredDate, 
    		 @JsonProperty("committer_name") String committerName, 
    		 @JsonProperty("committer_email") String committerEmail,
    		 @JsonProperty("committed_date") Calendar committedDate) {
		super();
		this.id = id;
		this.shortId = shortId;
		this.createdAt = createdAt;
		this.title = title;
		this.message = message;
		this.authorName = authorName;
		this.authorEmail = authorEmail;
		this.authoredDate = authoredDate;
		this.committerName = committerName;
		this.committerEmail = committerEmail;
		this.committedDate = committedDate;
	}

	public String getId() {
		return id;
	}

	public String getShortId() {
		return shortId;
	}

	public Calendar getCreatedAt() {
		return createdAt;
	}

	public String getTitle() {
		return title;
	}

	public String getMessage() {
		return message;
	}

	public String getAuthorName() {
		return authorName;
	}

	public String getAuthorEmail() {
		return authorEmail;
	}

	public Calendar getAuthoredDate() {
		return authoredDate;
	}

	public String getCommitterName() {
		return committerName;
	}

	public String getCommitterEmail() {
		return committerEmail;
	}

	public Calendar getCommittedDate() {
		return committedDate;
	}

}
