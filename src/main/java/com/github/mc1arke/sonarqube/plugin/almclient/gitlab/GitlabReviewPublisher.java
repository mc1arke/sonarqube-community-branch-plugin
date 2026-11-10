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
package com.github.mc1arke.sonarqube.plugin.almclient.gitlab;

import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.DraftNote;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.MergeRequestNote;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Stages a review and checks for unrelated drafts before bulk publication. */
public class GitlabReviewPublisher {

    public void publish(GitlabClient client, long projectId, long mergeRequestIid, List<MergeRequestNote> notes) throws IOException {
        if (!client.getMergeRequestDraftNotes(projectId, mergeRequestIid).isEmpty()) {
            throw new IllegalStateException("Cannot publish SonarQube review while the GitLab account has pending draft notes on this merge request");
        }

        List<Long> createdIds = new ArrayList<>();
        try {
            for (MergeRequestNote note : notes) {
                createdIds.add(client.addMergeRequestDraftNote(projectId, mergeRequestIid, note).id());
            }
            List<Long> pendingIds = client.getMergeRequestDraftNotes(projectId, mergeRequestIid).stream().map(DraftNote::id).toList();
            if (pendingIds.size() != createdIds.size() || !new HashSet<>(pendingIds).equals(new HashSet<>(createdIds))) {
                throw new IllegalStateException("GitLab draft notes changed while preparing the SonarQube review; refusing to publish unrelated drafts");
            }
        } catch (IOException | RuntimeException failure) {
            for (Long id : createdIds) {
                try {
                    client.deleteMergeRequestDraftNote(projectId, mergeRequestIid, id);
                } catch (IOException | RuntimeException cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            throw failure;
        }

        // Do not retry or fall back to individual comments after an ambiguous publish response.
        // GitLab may already have published the review even if the response was lost.
        client.publishMergeRequestDraftNotes(projectId, mergeRequestIid);
    }
}
