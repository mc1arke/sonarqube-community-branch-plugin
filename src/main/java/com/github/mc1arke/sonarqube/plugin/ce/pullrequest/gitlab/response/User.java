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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.response;

import java.util.Calendar;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class User {
  private final long id;
  private final String name;
  private final String username;
  private final String state;
  private final String avatarUrl;
  private final String webUrl;
  private final Calendar createdAt;
  private final String publicEmail;
  private final String skype;
  private final String linkedin;
  private final String twitter;
  private final String websiteUrl;
  private final Calendar lastSignInAt;
  private final Calendar confirmedAt;
  private final Calendar lastActivityOn;
  private final String email;
  private final Integer themeId;
  private final Integer colorSchemeId;
  private final Integer projectsLimit;
  private final Calendar currentSignInAt;
  private final boolean canCreateGroup;
  private final boolean canCreateProject;
  private final boolean twoFactorEnabled;
  private final boolean external;
  private final boolean isAdmin;

  @JsonCreator
  public User(
		@JsonProperty("id") long id, 
		@JsonProperty("name") String name, 
		@JsonProperty("username") String username, 
		@JsonProperty("state") String state, 
		@JsonProperty("avatar_url") String avatarUrl, 
		@JsonProperty("web_url") String webUrl,
		@JsonProperty("created_at") Calendar createdAt, 
		@JsonProperty("public_email") String publicEmail, 
		@JsonProperty("skype") String skype, 
		@JsonProperty("linkedin") String linkedin, 
		@JsonProperty("twitter") String twitter, 
		@JsonProperty("website_url") String websiteUrl,
		@JsonProperty("last_sign_in_at") Calendar lastSignInAt, 
		@JsonProperty("confirmed_at") Calendar confirmedAt, 
		@JsonProperty("last_activity_on") Calendar lastActivityOn, 
		@JsonProperty("email") String email, 
		@JsonProperty("theme_id") Integer themeId,
		@JsonProperty("color_scheme_id") Integer colorSchemeId, 
		@JsonProperty("projects_limit") Integer projectsLimit, 
		@JsonProperty("current_sign_in_at") Calendar currentSignInAt, 
		@JsonProperty("can_create_group") boolean canCreateGroup,
		@JsonProperty("can_create_project") boolean canCreateProject, 
		@JsonProperty("two_factor_enabled") boolean twoFactorEnabled, 
		@JsonProperty("external") boolean external, 
		@JsonProperty("is_admin") boolean isAdmin) {
	this.id = id;
	this.name = name;
	this.username = username;
	this.state = state;
	this.avatarUrl = avatarUrl;
	this.webUrl = webUrl;
	this.createdAt = createdAt;
	this.publicEmail = publicEmail;
	this.skype = skype;
	this.linkedin = linkedin;
	this.twitter = twitter;
	this.websiteUrl = websiteUrl;
	this.lastSignInAt = lastSignInAt;
	this.confirmedAt = confirmedAt;
	this.lastActivityOn = lastActivityOn;
	this.email = email;
	this.themeId = themeId;
	this.colorSchemeId = colorSchemeId;
	this.projectsLimit = projectsLimit;
	this.currentSignInAt = currentSignInAt;
	this.canCreateGroup = canCreateGroup;
	this.canCreateProject = canCreateProject;
	this.twoFactorEnabled = twoFactorEnabled;
	this.external = external;
	this.isAdmin = isAdmin;
  }

	public long getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getState() {
		return state;
	}
	
	public String getAvatarUrl() {
		return avatarUrl;
	}
	
	public String getWebUrl() {
		return webUrl;
	}
	
	public Calendar getCreatedAt() {
		return createdAt;
	}
	
	public String getPublicEmail() {
		return publicEmail;
	}
	
	public String getSkype() {
		return skype;
	}
	
	public String getLinkedin() {
		return linkedin;
	}
	
	public String getTwitter() {
		return twitter;
	}
	
	public String getWebsiteUrl() {
		return websiteUrl;
	}
	
	public Calendar getLastSignInAt() {
		return lastSignInAt;
	}
	
	public Calendar getConfirmedAt() {
		return confirmedAt;
	}
	
	public Calendar getLastActivityOn() {
		return lastActivityOn;
	}
	
	public String getEmail() {
		return email;
	}
	
	public Integer getThemeId() {
		return themeId;
	}
	
	public Integer getColorSchemeId() {
		return colorSchemeId;
	}
	
	public Integer getProjectsLimit() {
		return projectsLimit;
	}
	
	public Calendar getCurrentSignInAt() {
		return currentSignInAt;
	}
	
	public boolean isCanCreateGroup() {
		return canCreateGroup;
	}
	
	public boolean isCanCreateProject() {
		return canCreateProject;
	}
	
	public boolean isTwoFactorEnabled() {
		return twoFactorEnabled;
	}
	
	public boolean isExternal() {
		return external;
	}
	
	public boolean isAdmin() {
		return isAdmin;
	}

}
