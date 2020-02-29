/*
 * Copyright (C) 2020 Artemy Osipov
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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket;

import java.io.Serializable;

public class PullRequestParticipant implements Serializable  {

    public enum Status {
        APPROVED, NEEDS_WORK, UNAPPROVED
    }

    private final Status status;

    public PullRequestParticipant(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }
}
