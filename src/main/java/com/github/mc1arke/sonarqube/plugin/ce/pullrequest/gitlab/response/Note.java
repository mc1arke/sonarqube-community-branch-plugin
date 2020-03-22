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

public class Note {
    private final long id;
    private final String type;
    private final String body;
    private final User author;
	private final Calendar createdAt;
	private final Calendar updatedAt;
    private final boolean system;
    private final Long noteableId;
    private final Long noteableIid;
    private final String noteableType;
    private final Position position;
    private final boolean resolvable;
    private final boolean resolved;
    private final User resolvedBy;
    
    // Reference to the discussion, set during reading notes
	private String discussionId;


    @JsonCreator
    public Note(@JsonProperty("id") long id,
    		@JsonProperty("type") String type,  
    		@JsonProperty("body") String body, 
    		@JsonProperty("author") User author,
    		@JsonProperty("created_at") Calendar createdAt, 
    		@JsonProperty("updated_at") Calendar updatedAt,
    		@JsonProperty("system") boolean system, 
    		@JsonProperty("noteable_id") Long noteableId,  
    		@JsonProperty("noteable_iid") Long noteableIid,  
    		@JsonProperty("noteable_type") String noteableType,  
    		@JsonProperty("position") Position position, 
    		@JsonProperty("resolvable") boolean resolvable, 
    		@JsonProperty("resolved") boolean resolved, 
    		@JsonProperty("resolved_by") User resolvedBy) {
        this.id = id;
        this.body = body;
        this.type = type;
        this.author = author;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.system = system;
        this.noteableId = noteableId;
        this.noteableIid = noteableIid;
        this.noteableType = noteableType;
        this.position = position;
        this.resolvable = resolvable;
        this.resolved = resolved;
        this.resolvedBy = resolvedBy;
    }

    public long getId() {
        return id;
    }

    public String getType() {
		return type;
	}

	public String getBody() {
		return body;
	}

	public User getAuthor() {
		return author;
	}

	public Calendar getCreatedAt() {
		return createdAt;
	}

	public Calendar getUpdatedAt() {
		return updatedAt;
	}

	public boolean isSystem() {
		return system;
	}

	public Long getNoteableId() {
		return noteableId;
	}

	public Long getNoteableIid() {
		return noteableIid;
	}

	public String getNoteableType() {
		return noteableType;
	}
	
	public Position getPosition() {
		return position;
	}

	public boolean isResolvable() {
		return resolvable;
	}

	public boolean isResolved() {
		return resolved;
	}

	public User getResolvedBy() {
		return resolvedBy;
	}

	public String getDiscussionId() {
		return discussionId;
	}

	public void setDiscussionId(String discussionId) {
		this.discussionId = discussionId;
	}
	
}
