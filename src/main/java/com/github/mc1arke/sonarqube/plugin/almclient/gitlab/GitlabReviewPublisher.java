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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Publishes a set of notes as a single GitLab review (draft notes + {@code bulk_publish}) so that recipients get one
 * notification rather than one per note.
 * <p>
 * GitLab's {@code bulk_publish} publishes <em>every</em> pending draft the authenticated account has on the merge
 * request, so the review is only staged when the account has no other drafts, and only published if the pending
 * drafts are exactly the ones staged here. Whenever batching is not possible, but nothing has been left behind on
 * GitLab, {@link #publish} returns {@code false} so the caller can post the notes as individual discussions instead.
 */
public class GitlabReviewPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitlabReviewPublisher.class);

    /**
     * @return {@code true} if the notes were published as a review, {@code false} if nothing was published and no
     * drafts created by this call remain, meaning the caller should fall back to posting the notes individually.
     * @throws IOException if the state on GitLab is uncertain (cleanup of staged drafts failed, or the publish request
     * failed), in which case falling back could duplicate comments.
     */
    public boolean publish(GitlabClient client, long projectId, long mergeRequestIid, List<MergeRequestNote> notes) throws IOException {
        List<DraftNote> existingDrafts;
        try {
            existingDrafts = client.getMergeRequestDraftNotes(projectId, mergeRequestIid);
        } catch (IOException | IllegalStateException ex) {
            LOGGER.warn("Could not list GitLab draft notes, so the GitLab version may not support the Draft Notes API. Posting comments individually", ex);
            return false;
        }
        if (!existingDrafts.isEmpty()) {
            LOGGER.warn("The GitLab account used for decoration has {} pending draft note(s) on merge request {}. "
                    + "Posting comments individually to avoid publishing them", existingDrafts.size(), mergeRequestIid);
            return false;
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
        } catch (IOException | IllegalStateException failure) {
            deleteDrafts(client, projectId, mergeRequestIid, createdIds, failure);
            if (failure.getSuppressed().length > 0) {
                throw new IOException("Could not prepare SonarQube review on GitLab, and some of its draft notes could not be removed", failure);
            }
            LOGGER.warn("Could not prepare a SonarQube review on GitLab. Posting comments individually", failure);
            return false;
        } catch (RuntimeException failure) {
            deleteDrafts(client, projectId, mergeRequestIid, createdIds, failure);
            throw failure;
        }

        // Do not retry or fall back to individual comments after an ambiguous publish response.
        // GitLab may already have published the review even if the response was lost.
        client.publishMergeRequestDraftNotes(projectId, mergeRequestIid);
        return true;
    }

    private static void deleteDrafts(GitlabClient client, long projectId, long mergeRequestIid, List<Long> ids, Exception failure) {
        for (Long id : ids) {
            try {
                client.deleteMergeRequestDraftNote(projectId, mergeRequestIid, id);
            } catch (IOException | RuntimeException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
        }
    }
}
