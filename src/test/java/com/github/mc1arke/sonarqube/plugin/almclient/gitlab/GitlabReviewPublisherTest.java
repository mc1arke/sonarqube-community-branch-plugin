/*
 * Copyright (C) 2021-2024 Michael Clarke
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
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GitlabReviewPublisherTest {
    private final GitlabClient client = mock();
    private final GitlabReviewPublisher publisher = new GitlabReviewPublisher();
    private final List<MergeRequestNote> notes = List.of(new MergeRequestNote("issue"), new MergeRequestNote("summary"));

    @Test
    void publishesAllNewNotesInOneReview() throws IOException {
        when(client.getMergeRequestDraftNotes(1, 2)).thenReturn(List.of()).thenReturn(List.of(new DraftNote(12), new DraftNote(11)));
        when(client.addMergeRequestDraftNote(eq(1L), eq(2L), any())).thenReturn(new DraftNote(11), new DraftNote(12));
        publisher.publish(client, 1, 2, notes);
        var order = inOrder(client);
        order.verify(client).getMergeRequestDraftNotes(1, 2);
        order.verify(client).addMergeRequestDraftNote(1, 2, notes.getFirst());
        order.verify(client).addMergeRequestDraftNote(1, 2, notes.getLast());
        order.verify(client).getMergeRequestDraftNotes(1, 2);
        order.verify(client).publishMergeRequestDraftNotes(1, 2);
        verify(client, never()).addMergeRequestDiscussion(anyLong(), anyLong(), any());
    }

    @Test
    void refusesExistingDraftsWithoutChangingThem() throws IOException {
        when(client.getMergeRequestDraftNotes(1, 2)).thenReturn(List.of(new DraftNote(99)));
        assertThatThrownBy(() -> publisher.publish(client, 1, 2, notes)).hasMessageContaining("pending draft notes");
        verify(client, never()).addMergeRequestDraftNote(anyLong(), anyLong(), any());
        verify(client, never()).deleteMergeRequestDraftNote(anyLong(), anyLong(), anyLong());
        verify(client, never()).publishMergeRequestDraftNotes(anyLong(), anyLong());
    }

    @Test
    void removesOnlyOwnDraftsIfAnotherOperationAddsADraft() throws IOException {
        when(client.getMergeRequestDraftNotes(1, 2)).thenReturn(List.of()).thenReturn(List.of(new DraftNote(11), new DraftNote(12), new DraftNote(99)));
        when(client.addMergeRequestDraftNote(eq(1L), eq(2L), any())).thenReturn(new DraftNote(11), new DraftNote(12));
        assertThatThrownBy(() -> publisher.publish(client, 1, 2, notes)).hasMessageContaining("refusing to publish unrelated drafts");
        verify(client).deleteMergeRequestDraftNote(1, 2, 11);
        verify(client).deleteMergeRequestDraftNote(1, 2, 12);
        verify(client, never()).deleteMergeRequestDraftNote(1, 2, 99);
        verify(client, never()).publishMergeRequestDraftNotes(anyLong(), anyLong());
    }

    @Test
    void cleansUpAfterPartialPreparationFailureAndPreservesOriginalError() throws IOException {
        IOException failure = new IOException("creation failed");
        IOException cleanupFailure = new IOException("cleanup failed");
        when(client.addMergeRequestDraftNote(eq(1L), eq(2L), any()))
                .thenReturn(new DraftNote(11), new DraftNote(12)).thenThrow(failure);
        doThrow(cleanupFailure).when(client).deleteMergeRequestDraftNote(1, 2, 11);
        assertThatThrownBy(() -> publisher.publish(client, 1, 2,
                List.of(notes.getFirst(), notes.getLast(), new MergeRequestNote("third"))))
                .isSameAs(failure).hasSuppressedException(cleanupFailure);
        verify(client).deleteMergeRequestDraftNote(1, 2, 12);
        verify(client, never()).publishMergeRequestDraftNotes(anyLong(), anyLong());
    }

    @Test
    void doesNotRetryOrDeleteDraftsAfterAmbiguousPublicationFailure() throws IOException {
        when(client.getMergeRequestDraftNotes(1, 2)).thenReturn(List.of()).thenReturn(List.of(new DraftNote(11), new DraftNote(12)));
        when(client.addMergeRequestDraftNote(eq(1L), eq(2L), any())).thenReturn(new DraftNote(11), new DraftNote(12));
        doThrow(new IOException("response lost")).when(client).publishMergeRequestDraftNotes(1, 2);
        assertThatThrownBy(() -> publisher.publish(client, 1, 2, notes)).hasMessage("response lost");
        verify(client).publishMergeRequestDraftNotes(1, 2);
        verify(client, never()).deleteMergeRequestDraftNote(anyLong(), anyLong(), anyLong());
        verify(client, never()).addMergeRequestDiscussion(anyLong(), anyLong(), any());
    }
}
