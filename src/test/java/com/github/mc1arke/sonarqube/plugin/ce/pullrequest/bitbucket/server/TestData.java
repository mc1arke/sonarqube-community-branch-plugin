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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.server;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.SummaryComment;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.activity.Activity;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.activity.ActivityPage;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.activity.Comment;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.activity.User;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.diff.Diff;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.diff.DiffLine;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.diff.DiffPage;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.diff.File;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.diff.Hunk;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.diff.Segment;
import com.google.common.collect.Lists;

import java.util.stream.Stream;

public class TestData {

    public static final String PROJECT_KEY = "sample-project";
    public static final String REPOSITORY_SLUG = "sample-repo";
    public static final String PULL_REQUEST_ID = "11";
    public static final String COMMENT_USER_SLUG = "commentUser";
    public static final BitbucketServerRepository REPOSITORY = BitbucketServerRepository.projectRepository(PROJECT_KEY, REPOSITORY_SLUG);

    public static final String BITBUCKET_HOST = "http://localhost:8089";
    public static final String BITBUCKET_TOKEN = "APITOKEN";

    public static final String ISSUE_FILE_PATH = "src/com/github/Sample.java";
    public static final int ISSUE_LINE = 1;

    public static Comment comment() {
        return new Comment(1, 1, "", new User(COMMENT_USER_SLUG, COMMENT_USER_SLUG), null);
    }

    public static SummaryComment summaryComment() {
        return new SummaryComment("summaryComment");
    }

    public static ActivityPage activityPageWithComments(Comment... comments) {
        Activity[] activities = Stream.of(comments)
                .map(comment -> new Activity(1, null, comment))
                .toArray(Activity[]::new);

        return new ActivityPage(2, 250, true, 0, 0, activities);
    }

    public static DiffPage diffPage() {
        return new DiffPage(null, null, false, Lists.newArrayList(diff()));
    }

    public static Diff diff() {
        return new Diff(null, null,
                Lists.newArrayList(hunk()),
                null,
                new File(null, null, null, ISSUE_FILE_PATH, null));
    }

    public static Hunk hunk() {
        return new Hunk(null, 1, 5, 5, 0, Lists.newArrayList(segment()));
    }

    public static Segment segment() {
        return new Segment("ADDED", Lists.newArrayList(new DiffLine(1, ISSUE_LINE, null, false, null)), false);
    }
}
