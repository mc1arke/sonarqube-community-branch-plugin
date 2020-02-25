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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Note {
    private final long id;
    
    private final String type;

    private final String body;

    private final boolean system;

    private final User author;

    private final Position position;
    
    private String discussionId;

    @JsonCreator
    public Note(@JsonProperty("id") long id, @JsonProperty("type") String type,  @JsonProperty("body") String body, @JsonProperty("system") boolean system, @JsonProperty("author") User author, @JsonProperty("position") Position position) {
        this.id = id;
        this.body = body;
        this.type = type;
        this.system = system;
        this.author = author;
        this.position = position;
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

	public boolean isSystem() {
        return system;
    }

    public User getAuthor() {
        return author;
    }

	public Position getPosition() {
		return position;
	}

	public String getDiscussionId() {
		return discussionId;
	}

	public void setDiscussionId(String discussionId) {
		this.discussionId = discussionId;
	}
    
}
