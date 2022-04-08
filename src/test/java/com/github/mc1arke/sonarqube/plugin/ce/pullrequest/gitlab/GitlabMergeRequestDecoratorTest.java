/*
 * Copyright (C) 2021-2022 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab;

import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.GitlabClient;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.GitlabClientFactory;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.DecorationResult;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.Commit;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.CommitNote;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.DiffRefs;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.Discussion;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.MergeRequest;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.MergeRequestNote;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.Note;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.PipelineStatus;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.User;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.MarkdownFormatterFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.issue.Issue;
import org.sonar.api.platform.Server;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.scm.Changeset;
import org.sonar.ce.task.projectanalysis.scm.ScmInfo;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GitlabMergeRequestDecoratorTest {

    private static final long MERGE_REQUEST_IID = 123;
    private static final long PROJECT_ID = 101;
    private static final String PROJECT_PATH = "dummy/repo";
    private static final String PROJECT_KEY = "projectKey";
    private static final String ANALYSIS_UUID = "analysis-uuid";
    private static final String SONARQUBE_USERNAME = "sonarqube@gitlab.dummy";
    private static final String BASE_SHA = "baseSha";
    private static final String HEAD_SHA = "headSha";
    private static final String START_SHA = "startSha";
    private static final String MERGE_REQUEST_WEB_URL = "https://gitlab.dummy/path/to/mr";
    private static final String OLD_SONARQUBE_ISSUE_COMMENT = "This issue no longer exists in SonarQube, " +
            "but due to other comments being present in this discussion, " +
            "the discussion is not being being closed automatically. " +
            "Please manually resolve this discussion once the other comments have been reviewed.";

    private final GitlabClient gitlabClient = mock(GitlabClient.class);
    private final GitlabClientFactory gitlabClientFactory = mock(GitlabClientFactory.class);
    private final Server server = mock(Server.class);
    private final ScmInfoRepository scmInfoRepository = mock(ScmInfoRepository.class);
    private final AnalysisDetails analysisDetails = mock(AnalysisDetails.class);
    private final AlmSettingDto almSettingDto = mock(AlmSettingDto.class);
    private final ProjectAlmSettingDto projectAlmSettingDto = mock(ProjectAlmSettingDto.class);
    private final MergeRequest mergeRequest = mock(MergeRequest.class);
    private final User sonarqubeUser = mock(User.class);
    private final PostAnalysisIssueVisitor postAnalysisIssueVisitor = mock(PostAnalysisIssueVisitor.class);
    private final DiffRefs diffRefs = mock(DiffRefs.class);
    private final MarkdownFormatterFactory markdownFormatterFactory = mock(MarkdownFormatterFactory.class);

    private final GitlabMergeRequestDecorator underTest = new GitlabMergeRequestDecorator(server, scmInfoRepository, gitlabClientFactory, markdownFormatterFactory);

    @Before
    public void setUp() throws IOException {
        when(gitlabClientFactory.createClient(any(), any())).thenReturn(gitlabClient);
        when(almSettingDto.getUrl()).thenReturn("http://gitlab.dummy");
        when(projectAlmSettingDto.getAlmRepo()).thenReturn(PROJECT_PATH);
        when(analysisDetails.getBranchName()).thenReturn(Long.toString(MERGE_REQUEST_IID));
        when(mergeRequest.getIid()).thenReturn(MERGE_REQUEST_IID);
        when(mergeRequest.getSourceProjectId()).thenReturn(PROJECT_ID);
        when(mergeRequest.getDiffRefs()).thenReturn(diffRefs);
        when(mergeRequest.getWebUrl()).thenReturn(MERGE_REQUEST_WEB_URL);
        when(diffRefs.getBaseSha()).thenReturn(BASE_SHA);
        when(diffRefs.getHeadSha()).thenReturn(HEAD_SHA);
        when(diffRefs.getStartSha()).thenReturn(START_SHA);
        when(gitlabClient.getMergeRequest(PROJECT_PATH, MERGE_REQUEST_IID)).thenReturn(mergeRequest);
        when(gitlabClient.getMergeRequestCommits(PROJECT_ID, MERGE_REQUEST_IID)).thenReturn(Arrays.stream(new String[]{"ABC", "DEF", "GHI", "JKL"})
                .map(Commit::new)
                .collect(Collectors.toList()));
        when(sonarqubeUser.getUsername()).thenReturn(SONARQUBE_USERNAME);
        when(gitlabClient.getCurrentUser()).thenReturn(sonarqubeUser);
        when(analysisDetails.getPostAnalysisIssueVisitor()).thenReturn(postAnalysisIssueVisitor);
        when(analysisDetails.getAnalysisProjectKey()).thenReturn(PROJECT_KEY);
        when(analysisDetails.getAnalysisId()).thenReturn(ANALYSIS_UUID);
        when(postAnalysisIssueVisitor.getIssues()).thenReturn(new ArrayList<>());
    }

    @Test
    public void shouldReturnCorrectDecoratorType() {
        assertThat(underTest.alm()).containsOnly(ALM.GITLAB);
    }

    @Test
    public void shouldThrowErrorWhenPullRequestKeyNotNumeric() {
        when(analysisDetails.getBranchName()).thenReturn("non-MR-IID");

        assertThatThrownBy(() -> underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not parse Merge Request ID");
    }

    @Test
    public void shouldThrowErrorWhenGitlabMergeRequestRetrievalFails() throws IOException {
        when(gitlabClient.getMergeRequest(any(), anyLong())).thenThrow(new IOException("dummy"));

        assertThatThrownBy(() -> underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not retrieve Merge Request details");
    }

    @Test
    public void shouldThrowErrorWhenGitlabUserRetrievalFails() throws IOException {
        when(gitlabClient.getCurrentUser()).thenThrow(new IOException("dummy"));

        assertThatThrownBy(() -> underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not retrieve current user details");
    }

    @Test
    public void shouldThrowErrorWhenGitlabMergeRequestCommitsRetrievalFails() throws IOException {
        when(gitlabClient.getMergeRequestCommits(anyLong(), anyLong())).thenThrow(new IOException("dummy"));

        assertThatThrownBy(() -> underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not retrieve commit details for Merge Request");
    }

    @Test
    public void shouldThrowErrorWhenGitlabMergeRequestDiscussionRetrievalFails() throws IOException {
        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenThrow(new IOException("dummy"));

        assertThatThrownBy(() -> underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not retrieve Merge Request discussions");
    }

    @Test
    public void shouldCloseDiscussionWithSingleResolvableNoteFromSonarqubeUserButNoIssueIdInBody() throws IOException {
        Note note = mock(Note.class);
        when(note.getAuthor()).thenReturn(sonarqubeUser);
        when(note.getBody()).thenReturn("Post with no issue ID");
        when(note.isResolvable()).thenReturn(true);

        Discussion discussion = mock(Discussion.class);
        when(discussion.getId()).thenReturn("discussionId");
        when(discussion.getNotes()).thenReturn(Collections.singletonList(note));

        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(Collections.singletonList(discussion));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        ArgumentCaptor<MergeRequestNote> mergeRequestNoteArgumentCaptor = ArgumentCaptor.forClass(MergeRequestNote.class);
        verify(gitlabClient, never()).resolveMergeRequestDiscussion(anyLong(), anyLong(), any());
        verify(gitlabClient).addMergeRequestDiscussion(anyLong(), anyLong(), mergeRequestNoteArgumentCaptor.capture());

        assertThat(mergeRequestNoteArgumentCaptor.getValue()).isNotInstanceOf(CommitNote.class);    }

    @Test
    public void shouldNotCloseDiscussionWithSingleNonResolvableNoteFromSonarqubeUserButNoIssueIdInBody() throws IOException {
        Note note = mock(Note.class);
        when(note.getAuthor()).thenReturn(sonarqubeUser);
        when(note.getBody()).thenReturn("Post with no issue ID");
        when(note.isResolvable()).thenReturn(false);

        Discussion discussion = mock(Discussion.class);
        when(discussion.getId()).thenReturn("discussionId");
        when(discussion.getNotes()).thenReturn(Collections.singletonList(note));

        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(Collections.singletonList(discussion));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(gitlabClient, never()).resolveMergeRequestDiscussion(anyLong(), anyLong(), any());
    }

    @Test
    public void shouldNotCloseDiscussionWithMultipleResolvableNotesFromSonarqubeUserButNoId() throws IOException {
        Note note = mock(Note.class);
        when(note.getAuthor()).thenReturn(sonarqubeUser);
        when(note.getBody()).thenReturn("Another post with no issue ID\nbut containing a new line");
        when(note.isResolvable()).thenReturn(true);

        Note note2 = mock(Note.class);
        when(note2.getAuthor()).thenReturn(sonarqubeUser);
        when(note2.getBody()).thenReturn("Additional post from user");
        when(note2.isResolvable()).thenReturn(true);


        Discussion discussion = mock(Discussion.class);
        when(discussion.getId()).thenReturn("discussionId2");
        when(discussion.getNotes()).thenReturn(Arrays.asList(note, note2));

        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(Collections.singletonList(discussion));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        ArgumentCaptor<MergeRequestNote> mergeRequestNoteArgumentCaptor = ArgumentCaptor.forClass(MergeRequestNote.class);
        verify(gitlabClient, never()).resolveMergeRequestDiscussion(anyLong(), anyLong(), any());
        verify(gitlabClient).addMergeRequestDiscussion(anyLong(), anyLong(), mergeRequestNoteArgumentCaptor.capture());

        assertThat(mergeRequestNoteArgumentCaptor.getValue()).isNotInstanceOf(CommitNote.class);
    }

    @Test
    public void shouldCloseDiscussionWithResolvableNoteFromSonarqubeUserAndOnlySystemNoteFromOtherUser() throws IOException {
        User otherUser = mock(User.class);
        when(otherUser.getUsername()).thenReturn("other.user@gitlab.dummy");

        Note note = mock(Note.class);
        when(note.getAuthor()).thenReturn(sonarqubeUser);
        when(note.getBody()).thenReturn("[View in SonarQube](url)");
        when(note.isResolvable()).thenReturn(true);

        Note note2 = mock(Note.class);
        when(note2.getAuthor()).thenReturn(otherUser);
        when(note2.getBody()).thenReturn("System post on behalf of user");
        when(note2.isSystem()).thenReturn(true);


        Discussion discussion = mock(Discussion.class);
        when(discussion.getId()).thenReturn("discussionId2");
        when(discussion.getNotes()).thenReturn(Arrays.asList(note, note2));

        when(analysisDetails.parseIssueIdFromUrl("url")).thenReturn(Optional.of(new AnalysisDetails.ProjectIssueIdentifier(PROJECT_KEY, "issueId")));
        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(Collections.singletonList(discussion));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        ArgumentCaptor<String> discussionIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(gitlabClient).resolveMergeRequestDiscussion(eq(PROJECT_ID), eq(MERGE_REQUEST_IID), discussionIdArgumentCaptor.capture());

        assertThat(discussionIdArgumentCaptor.getValue()).isEqualTo(discussion.getId());
    }

    @Test
    public void shouldNotAttemptCloseOfDiscussionWithMultipleResolvableNotesFromSonarqubeUserAndAnotherUserWithNoId() throws IOException {
        User otherUser = mock(User.class);
        when(otherUser.getUsername()).thenReturn("other.user@gitlab.dummy");

        Note note = mock(Note.class);
        when(note.getAuthor()).thenReturn(sonarqubeUser);
        when(note.getBody()).thenReturn("Yet another post with no issue ID");
        when(note.isResolvable()).thenReturn(true);

        Note note2 = mock(Note.class);
        when(note2.getAuthor()).thenReturn(otherUser);
        when(note2.getBody()).thenReturn("Post from another user");
        when(note2.isResolvable()).thenReturn(true);

        Discussion discussion = mock(Discussion.class);
        when(discussion.getId()).thenReturn("discussionId3");
        when(discussion.getNotes()).thenReturn(Arrays.asList(note, note2));

        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(Collections.singletonList(discussion));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);
        verify(gitlabClient, never()).resolveMergeRequestDiscussion(anyLong(), anyLong(), any());

        ArgumentCaptor<MergeRequestNote> mergeRequestNoteArgumentCaptor = ArgumentCaptor.forClass(MergeRequestNote.class);
        verify(gitlabClient, never()).resolveMergeRequestDiscussion(anyLong(), anyLong(), any());
        verify(gitlabClient).addMergeRequestDiscussion(anyLong(), anyLong(), mergeRequestNoteArgumentCaptor.capture());

        assertThat(mergeRequestNoteArgumentCaptor.getValue()).isNotInstanceOf(CommitNote.class);
    }

    @Test
    public void shouldNotCommentOrAttemptCloseOfDiscussionWithMultipleResolvableNotesFromSonarqubeUserAndACloseMessageWithNoId() throws IOException {
        Note note = mock(Note.class);
        when(note.getAuthor()).thenReturn(sonarqubeUser);
        when(note.getBody()).thenReturn("And another post with no issue ID\nNo View in SonarQube link");
        when(note.isResolvable()).thenReturn(true);

        Note note2 = mock(Note.class);
        when(note2.getAuthor()).thenReturn(sonarqubeUser);
        when(note2.getBody()).thenReturn("dummy");
        when(note2.isResolvable()).thenReturn(true);

        Note note3 = mock(Note.class);
        when(note3.getAuthor()).thenReturn(sonarqubeUser);
        when(note3.getBody()).thenReturn("other comment");
        when(note3.isResolvable()).thenReturn(true);

        Discussion discussion = mock(Discussion.class);
        when(discussion.getId()).thenReturn("discussionId4");
        when(discussion.getNotes()).thenReturn(Arrays.asList(note, note2, note3));

        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(Collections.singletonList(discussion));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(gitlabClient, never()).resolveMergeRequestDiscussion(anyLong(), anyLong(), any());
        verify(gitlabClient, never()).addMergeRequestDiscussionNote(anyLong(), anyLong(), any(), any());
    }

    @Test
    public void shouldCommentAboutCloseOfDiscussionWithMultipleResolvableNotesFromSonarqubeUserAndAnotherUserWithIssuedId() throws IOException {
        User otherUser = mock(User.class);
        when(otherUser.getUsername()).thenReturn("other.user@gitlab.dummy");

        Note note = mock(Note.class);
        when(note.getAuthor()).thenReturn(sonarqubeUser);
        when(note.getBody()).thenReturn("Sonarqube reported issue\n[View in SonarQube](https://dummy.url.with.subdomain/path/to/sonarqube?paramters=many&values=complex%20and+encoded)");
        when(note.isResolvable()).thenReturn(true);

        Note note2 = mock(Note.class);
        when(note2.getAuthor()).thenReturn(otherUser);
        when(note2.getBody()).thenReturn("Message from another user");
        when(note2.isResolvable()).thenReturn(true);

        Discussion discussion = mock(Discussion.class);
        when(discussion.getId()).thenReturn("discussionId5");
        when(discussion.getNotes()).thenReturn(Arrays.asList(note, note2));

        when(analysisDetails.parseIssueIdFromUrl("https://dummy.url.with.subdomain/path/to/sonarqube?paramters=many&values=complex%20and+encoded")).thenReturn(Optional.of(new AnalysisDetails.ProjectIssueIdentifier(PROJECT_KEY, "new-issue")));
        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(Collections.singletonList(discussion));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);
        verify(gitlabClient, never()).resolveMergeRequestDiscussion(anyLong(), anyLong(), any());

        ArgumentCaptor<String> discussionIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> noteContentArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(gitlabClient).addMergeRequestDiscussionNote(eq(PROJECT_ID), eq(MERGE_REQUEST_IID), discussionIdArgumentCaptor.capture(), noteContentArgumentCaptor.capture());

        assertThat(discussionIdArgumentCaptor.getValue()).isEqualTo(discussion.getId());
        assertThat(noteContentArgumentCaptor.getValue()).isEqualTo(OLD_SONARQUBE_ISSUE_COMMENT);
    }

    @Test
    public void shouldThrowErrorIfUnableToCleanUpDiscussionOnGitlab() throws IOException {
        User otherUser = mock(User.class);
        when(otherUser.getUsername()).thenReturn("other.user@gitlab.dummy");

        Note note = mock(Note.class);
        when(note.getAuthor()).thenReturn(sonarqubeUser);
        when(note.getBody()).thenReturn("Sonarqube reported issue\n[View in SonarQube](https://dummy.url.with.subdomain/path/to/sonarqube?paramters=many&values=complex%20and+encoded)");
        when(note.isResolvable()).thenReturn(true);

        Note note2 = mock(Note.class);
        when(note2.getAuthor()).thenReturn(otherUser);
        when(note2.getBody()).thenReturn("Message from another user");
        when(note2.isResolvable()).thenReturn(true);

        Discussion discussion = mock(Discussion.class);
        when(discussion.getId()).thenReturn("discussionId5");
        when(discussion.getNotes()).thenReturn(Arrays.asList(note, note2));

        when(analysisDetails.parseIssueIdFromUrl("https://dummy.url.with.subdomain/path/to/sonarqube?paramters=many&values=complex%20and+encoded")).thenReturn(Optional.of(new AnalysisDetails.ProjectIssueIdentifier(PROJECT_KEY, "issueId")));
        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(Collections.singletonList(discussion));
        doThrow(new IOException("dummy")).when(gitlabClient).addMergeRequestDiscussionNote(anyLong(), anyLong(), any(), any());

        assertThatThrownBy(() -> underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not add note to Merge Request discussion");
        verify(gitlabClient, never()).resolveMergeRequestDiscussion(anyLong(), anyLong(), any());

        ArgumentCaptor<String> discussionIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> noteContentArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(gitlabClient).addMergeRequestDiscussionNote(eq(PROJECT_ID), eq(MERGE_REQUEST_IID), discussionIdArgumentCaptor.capture(), noteContentArgumentCaptor.capture());

        assertThat(discussionIdArgumentCaptor.getValue()).isEqualTo(discussion.getId());
        assertThat(noteContentArgumentCaptor.getValue()).isEqualTo(OLD_SONARQUBE_ISSUE_COMMENT);
    }

    @Test
    public void shouldNotCommentOrAttemptCloseOfDiscussionWithMultipleResolvableNotesFromSonarqubeUserAndACloseMessageWithIssueId() throws IOException {
        Note note = mock(Note.class);
        when(note.getAuthor()).thenReturn(sonarqubeUser);
        when(note.getBody()).thenReturn("And another post with an issue ID\n[View in SonarQube](url)");
        when(note.isResolvable()).thenReturn(true);

        Note note2 = mock(Note.class);
        when(note2.getAuthor()).thenReturn(sonarqubeUser);
        when(note2.getBody()).thenReturn(OLD_SONARQUBE_ISSUE_COMMENT);
        when(note2.isResolvable()).thenReturn(true);

        Note note3 = mock(Note.class);
        when(note3.getAuthor()).thenReturn(sonarqubeUser);
        when(note3.getBody()).thenReturn("Some additional comment");
        when(note3.isResolvable()).thenReturn(true);

        Discussion discussion = mock(Discussion.class);
        when(discussion.getId()).thenReturn("discussionId6");
        when(discussion.getNotes()).thenReturn(Arrays.asList(note, note2, note3));

        when(analysisDetails.parseIssueIdFromUrl("url")).thenReturn(Optional.of(new AnalysisDetails.ProjectIssueIdentifier(PROJECT_KEY, "issueId")));
        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(Collections.singletonList(discussion));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(gitlabClient, never()).resolveMergeRequestDiscussion(anyLong(), anyLong(), any());
        verify(gitlabClient, never()).addMergeRequestDiscussionNote(anyLong(), anyLong(), any(), any());
    }

    @Test
    public void shouldNotCommentOrAttemptCloseOfDiscussionWithMultipleResolvableNotesFromSonarqubeWithOtherProjectId() throws IOException {
        Note note = mock(Note.class);
        when(note.getAuthor()).thenReturn(sonarqubeUser);
        when(note.getBody()).thenReturn("And another post with an issue ID\n[View in SonarQube](url)");
        when(note.isResolvable()).thenReturn(true);

        Note note2 = mock(Note.class);
        when(note2.getAuthor()).thenReturn(sonarqubeUser);
        when(note2.getBody()).thenReturn(OLD_SONARQUBE_ISSUE_COMMENT);
        when(note2.isResolvable()).thenReturn(true);

        Note note3 = mock(Note.class);
        when(note3.getAuthor()).thenReturn(sonarqubeUser);
        when(note3.getBody()).thenReturn("Some additional comment");
        when(note3.isResolvable()).thenReturn(true);

        Discussion discussion = mock(Discussion.class);
        when(discussion.getId()).thenReturn("discussionId6");
        when(discussion.getNotes()).thenReturn(Arrays.asList(note, note2, note3));

        when(analysisDetails.parseIssueIdFromUrl("url")).thenReturn(Optional.of(new AnalysisDetails.ProjectIssueIdentifier("otherProjectKey", "issueId")));
        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(Collections.singletonList(discussion));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(gitlabClient, never()).resolveMergeRequestDiscussion(anyLong(), anyLong(), any());
        verify(gitlabClient, never()).addMergeRequestDiscussionNote(anyLong(), anyLong(), any(), any());
    }

    @Test
    public void shouldThrowErrorIfSubmittingNewIssueToGitlabFails() throws IOException {
        PostAnalysisIssueVisitor.LightIssue lightIssue = mock(PostAnalysisIssueVisitor.LightIssue.class);
        when(lightIssue.key()).thenReturn("issueKey1");
        when(lightIssue.getStatus()).thenReturn(Issue.STATUS_OPEN);
        when(lightIssue.getLine()).thenReturn(999);

        Component component = mock(Component.class);

        PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock(PostAnalysisIssueVisitor.ComponentIssue.class);
        when(componentIssue.getIssue()).thenReturn(lightIssue);
        when(componentIssue.getComponent()).thenReturn(component);

        when(postAnalysisIssueVisitor.getIssues()).thenReturn(Collections.singletonList(componentIssue));
        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(new ArrayList<>());
        when(analysisDetails.createAnalysisIssueSummary(eq(componentIssue), any())).thenReturn("Issue Summary");
        when(analysisDetails.getSCMPathForIssue(componentIssue)).thenReturn(Optional.of("path-to-file"));

        Changeset changeset = mock(Changeset.class);
        when(changeset.getRevision()).thenReturn("DEF");

        ScmInfo scmInfo = mock(ScmInfo.class);
        when(scmInfo.hasChangesetForLine(999)).thenReturn(true);
        when(scmInfo.getChangesetForLine(999)).thenReturn(changeset);
        when(scmInfoRepository.getScmInfo(component)).thenReturn(Optional.of(scmInfo));

        when(gitlabClient.addMergeRequestDiscussion(anyLong(), anyLong(), any())).thenThrow(new IOException("dummy"));

        assertThatThrownBy(() -> underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not submit commit comment to Gitlab");

        verify(gitlabClient, never()).resolveMergeRequestDiscussion(anyLong(), anyLong(), any());
        verify(gitlabClient, never()).addMergeRequestDiscussionNote(anyLong(), anyLong(), any(), any());

        ArgumentCaptor<MergeRequestNote> mergeRequestNoteArgumentCaptor = ArgumentCaptor.forClass(MergeRequestNote.class);
        verify(gitlabClient).addMergeRequestDiscussion(eq(PROJECT_ID), eq(MERGE_REQUEST_IID), mergeRequestNoteArgumentCaptor.capture());

        assertThat(mergeRequestNoteArgumentCaptor.getValue()).isEqualToComparingFieldByField(new CommitNote("Issue Summary", BASE_SHA, START_SHA, HEAD_SHA, "path-to-file", "path-to-file", 999));
    }

    @Test
    public void shouldStartNewDiscussionForNewIssueFromCommitInMergeRequest() throws IOException {
        PostAnalysisIssueVisitor.LightIssue lightIssue = mock(PostAnalysisIssueVisitor.LightIssue.class);
        when(lightIssue.key()).thenReturn("issueKey1");
        when(lightIssue.getStatus()).thenReturn(Issue.STATUS_OPEN);
        when(lightIssue.getLine()).thenReturn(999);

        Component component = mock(Component.class);

        PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock(PostAnalysisIssueVisitor.ComponentIssue.class);
        when(componentIssue.getIssue()).thenReturn(lightIssue);
        when(componentIssue.getComponent()).thenReturn(component);

        when(postAnalysisIssueVisitor.getIssues()).thenReturn(Collections.singletonList(componentIssue));
        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(new ArrayList<>());
        when(analysisDetails.createAnalysisIssueSummary(eq(componentIssue), any())).thenReturn("Issue Summary");
        when(analysisDetails.getSCMPathForIssue(componentIssue)).thenReturn(Optional.of("path-to-file"));

        Changeset changeset = mock(Changeset.class);
        when(changeset.getRevision()).thenReturn("DEF");

        ScmInfo scmInfo = mock(ScmInfo.class);
        when(scmInfo.hasChangesetForLine(999)).thenReturn(true);
        when(scmInfo.getChangesetForLine(999)).thenReturn(changeset);
        when(scmInfoRepository.getScmInfo(component)).thenReturn(Optional.of(scmInfo));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(gitlabClient, never()).resolveMergeRequestDiscussion(anyLong(), anyLong(), any());
        verify(gitlabClient, never()).addMergeRequestDiscussionNote(anyLong(), anyLong(), any(), any());

        ArgumentCaptor<MergeRequestNote> mergeRequestNoteArgumentCaptor = ArgumentCaptor.forClass(MergeRequestNote.class);
        verify(gitlabClient, times(2)).addMergeRequestDiscussion(eq(PROJECT_ID), eq(MERGE_REQUEST_IID), mergeRequestNoteArgumentCaptor.capture());

        assertThat(mergeRequestNoteArgumentCaptor.getAllValues().get(0)).isEqualToComparingFieldByField(new CommitNote("Issue Summary", BASE_SHA, START_SHA, HEAD_SHA, "path-to-file", "path-to-file", 999));
        assertThat(mergeRequestNoteArgumentCaptor.getAllValues().get(1)).isNotInstanceOf(CommitNote.class);
    }

    @Test
    public void shouldNotStartNewDiscussionForIssueWithExistingCommentFromCommitInMergeRequest() throws IOException {
        PostAnalysisIssueVisitor.LightIssue lightIssue = mock(PostAnalysisIssueVisitor.LightIssue.class);
        when(lightIssue.key()).thenReturn("issueKey1");
        when(lightIssue.getStatus()).thenReturn(Issue.STATUS_OPEN);
        when(lightIssue.getLine()).thenReturn(999);

        Component component = mock(Component.class);

        PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock(PostAnalysisIssueVisitor.ComponentIssue.class);
        when(componentIssue.getIssue()).thenReturn(lightIssue);
        when(componentIssue.getComponent()).thenReturn(component);

        Note note = mock(Note.class);
        when(note.getBody()).thenReturn("Reported issue\n[View in SonarQube](url)");
        when(note.getAuthor()).thenReturn(sonarqubeUser);
        when(note.isResolvable()).thenReturn(true);

        Discussion discussion = mock(Discussion.class);
        when(discussion.getId()).thenReturn("discussion-id");
        when(discussion.getNotes()).thenReturn(Collections.singletonList(note));

        when(analysisDetails.parseIssueIdFromUrl("url")).thenReturn(Optional.of(new AnalysisDetails.ProjectIssueIdentifier(PROJECT_KEY, "issueKey1")));
        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(Collections.singletonList(discussion));
        when(postAnalysisIssueVisitor.getIssues()).thenReturn(Collections.singletonList(componentIssue));
        when(analysisDetails.createAnalysisIssueSummary(eq(componentIssue), any())).thenReturn("Issue Summary");
        when(analysisDetails.getSCMPathForIssue(componentIssue)).thenReturn(Optional.of("path-to-file"));

        Changeset changeset = mock(Changeset.class);
        when(changeset.getRevision()).thenReturn("DEF");

        ScmInfo scmInfo = mock(ScmInfo.class);
        when(scmInfo.hasChangesetForLine(999)).thenReturn(true);
        when(scmInfo.getChangesetForLine(999)).thenReturn(changeset);
        when(scmInfoRepository.getScmInfo(component)).thenReturn(Optional.of(scmInfo));

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(gitlabClient, never()).resolveMergeRequestDiscussion(anyLong(), anyLong(), any());
        verify(gitlabClient, never()).addMergeRequestDiscussionNote(anyLong(), anyLong(), any(), any());

        ArgumentCaptor<MergeRequestNote> mergeRequestNoteArgumentCaptor = ArgumentCaptor.forClass(MergeRequestNote.class);
        verify(gitlabClient).addMergeRequestDiscussion(eq(PROJECT_ID), eq(MERGE_REQUEST_IID), mergeRequestNoteArgumentCaptor.capture());

        assertThat(mergeRequestNoteArgumentCaptor.getValue()).isNotInstanceOf(CommitNote.class);
    }

    @Test
    public void shouldNotCreateCommentsForIssuesWithNoLineNumbers() throws IOException {
        PostAnalysisIssueVisitor.LightIssue lightIssue = mock(PostAnalysisIssueVisitor.LightIssue.class);
        when(lightIssue.key()).thenReturn("issueKey1");
        when(lightIssue.getStatus()).thenReturn(Issue.STATUS_OPEN);
        when(lightIssue.getLine()).thenReturn(null);

        Component component = mock(Component.class);

        PostAnalysisIssueVisitor.ComponentIssue componentIssue = mock(PostAnalysisIssueVisitor.ComponentIssue.class);
        when(componentIssue.getIssue()).thenReturn(lightIssue);
        when(componentIssue.getComponent()).thenReturn(component);

        when(postAnalysisIssueVisitor.getIssues()).thenReturn(Collections.singletonList(componentIssue));
        when(gitlabClient.getMergeRequestDiscussions(anyLong(), anyLong())).thenReturn(new ArrayList<>());
        when(analysisDetails.createAnalysisIssueSummary(eq(componentIssue), any())).thenReturn("Issue Summary");

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        verify(gitlabClient, never()).resolveMergeRequestDiscussion(anyLong(), anyLong(), any());
        verify(gitlabClient, never()).addMergeRequestDiscussionNote(anyLong(), anyLong(), any(), any());
        verify(scmInfoRepository, never()).getScmInfo(any());

        ArgumentCaptor<MergeRequestNote> mergeRequestNoteArgumentCaptor = ArgumentCaptor.forClass(MergeRequestNote.class);
        verify(gitlabClient).addMergeRequestDiscussion(eq(PROJECT_ID), eq(MERGE_REQUEST_IID), mergeRequestNoteArgumentCaptor.capture());

        assertThat(mergeRequestNoteArgumentCaptor.getValue()).isNotInstanceOf(CommitNote.class);
    }

    @Test
    public void shouldSubmitSuccessfulPipelineStatusAndResolvedSummaryCommentOnSuccessAnalysis() throws IOException {
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.OK);
        when(analysisDetails.createAnalysisSummary(any())).thenReturn("Summary comment");
        when(analysisDetails.getCommitSha()).thenReturn("commitsha");

        when(server.getPublicRootUrl()).thenReturn("https://sonarqube.dummy");

        Discussion discussion = mock(Discussion.class);
        when(discussion.getId()).thenReturn("dicussion id");
        when(gitlabClient.addMergeRequestDiscussion(anyLong(), anyLong(), any())).thenReturn(discussion);

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        ArgumentCaptor<MergeRequestNote> mergeRequestNoteArgumentCaptor = ArgumentCaptor.forClass(MergeRequestNote.class);
        verify(gitlabClient).addMergeRequestDiscussion(eq(PROJECT_ID), eq(MERGE_REQUEST_IID), mergeRequestNoteArgumentCaptor.capture());
        verify(gitlabClient).resolveMergeRequestDiscussion(PROJECT_ID, MERGE_REQUEST_IID, discussion.getId());
        ArgumentCaptor<PipelineStatus> pipelineStatusArgumentCaptor = ArgumentCaptor.forClass(PipelineStatus.class);
        verify(gitlabClient).setMergeRequestPipelineStatus(eq(PROJECT_ID), eq("commitsha"), pipelineStatusArgumentCaptor.capture());

        assertThat(mergeRequestNoteArgumentCaptor.getValue()).isEqualToComparingFieldByField(new MergeRequestNote("Summary comment"));
        assertThat(pipelineStatusArgumentCaptor.getValue())
                .isEqualToComparingFieldByField(new PipelineStatus("SonarQube", "SonarQube Status",
                        PipelineStatus.State.SUCCESS, "https://sonarqube.dummy/dashboard?id=" + PROJECT_KEY + "&pullRequest=" + MERGE_REQUEST_IID, null, null));
    }

    @Test
    public void shouldSubmitFailedPipelineStatusAndUnresolvedSummaryCommentOnFailedAnalysis() throws IOException {
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.ERROR);
        when(analysisDetails.createAnalysisSummary(any())).thenReturn("Different Summary comment");
        when(analysisDetails.getCommitSha()).thenReturn("other sha");
        when(analysisDetails.getCoverage()).thenReturn(Optional.of(BigDecimal.TEN));
        when(analysisDetails.getScannerProperty("com.github.mc1arke.sonarqube.plugin.branch.pullrequest.gitlab.pipelineId")).thenReturn(Optional.of("11"));

        when(server.getPublicRootUrl()).thenReturn("https://sonarqube2.dummy");

        Discussion discussion = mock(Discussion.class);
        when(discussion.getId()).thenReturn("dicussion id 2");
        when(gitlabClient.addMergeRequestDiscussion(anyLong(), anyLong(), any())).thenReturn(discussion);

        underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto);

        ArgumentCaptor<MergeRequestNote> mergeRequestNoteArgumentCaptor = ArgumentCaptor.forClass(MergeRequestNote.class);
        verify(gitlabClient).addMergeRequestDiscussion(eq(PROJECT_ID), eq(MERGE_REQUEST_IID), mergeRequestNoteArgumentCaptor.capture());
        verify(gitlabClient, never()).resolveMergeRequestDiscussion(PROJECT_ID, MERGE_REQUEST_IID, discussion.getId());
        ArgumentCaptor<PipelineStatus> pipelineStatusArgumentCaptor = ArgumentCaptor.forClass(PipelineStatus.class);
        verify(gitlabClient).setMergeRequestPipelineStatus(eq(PROJECT_ID), eq("other sha"), pipelineStatusArgumentCaptor.capture());

        assertThat(mergeRequestNoteArgumentCaptor.getValue()).isEqualToComparingFieldByField(new MergeRequestNote("Different Summary comment"));
        assertThat(pipelineStatusArgumentCaptor.getValue())
                .isEqualToComparingFieldByField(new PipelineStatus("SonarQube", "SonarQube Status",
                        PipelineStatus.State.FAILED, "https://sonarqube2.dummy/dashboard?id=" + PROJECT_KEY + "&pullRequest=" + MERGE_REQUEST_IID, BigDecimal.TEN, 11L));
    }

    @Test
    public void shouldThrowErrorWhenSubmitPipelineStatusToGitlabFails() throws IOException {
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.ERROR);
        when(analysisDetails.createAnalysisSummary(any())).thenReturn("Different Summary comment");
        when(analysisDetails.getCommitSha()).thenReturn("other sha");
        when(analysisDetails.getCoverage()).thenReturn(Optional.of(BigDecimal.TEN));
        when(analysisDetails.getScannerProperty("com.github.mc1arke.sonarqube.plugin.branch.pullrequest.gitlab.pipelineId")).thenReturn(Optional.of("11"));

        when(server.getPublicRootUrl()).thenReturn("https://sonarqube2.dummy");

        Discussion discussion = mock(Discussion.class);
        when(discussion.getId()).thenReturn("dicussion id 2");
        when(gitlabClient.addMergeRequestDiscussion(anyLong(), anyLong(), any())).thenReturn(discussion);
        doThrow(new IOException("dummy")).when(gitlabClient).setMergeRequestPipelineStatus(anyLong(), any(), any());

        assertThatThrownBy(() -> underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not update pipeline status in Gitlab");

        ArgumentCaptor<MergeRequestNote> mergeRequestNoteArgumentCaptor = ArgumentCaptor.forClass(MergeRequestNote.class);
        verify(gitlabClient).addMergeRequestDiscussion(eq(PROJECT_ID), eq(MERGE_REQUEST_IID), mergeRequestNoteArgumentCaptor.capture());
        verify(gitlabClient, never()).resolveMergeRequestDiscussion(PROJECT_ID, MERGE_REQUEST_IID, discussion.getId());
        ArgumentCaptor<PipelineStatus> pipelineStatusArgumentCaptor = ArgumentCaptor.forClass(PipelineStatus.class);
        verify(gitlabClient).setMergeRequestPipelineStatus(eq(PROJECT_ID), eq("other sha"), pipelineStatusArgumentCaptor.capture());

        assertThat(mergeRequestNoteArgumentCaptor.getValue()).isEqualToComparingFieldByField(new MergeRequestNote("Different Summary comment"));
        assertThat(pipelineStatusArgumentCaptor.getValue())
                .isEqualToComparingFieldByField(new PipelineStatus("SonarQube", "SonarQube Status",
                        PipelineStatus.State.FAILED, "https://sonarqube2.dummy/dashboard?id=" + PROJECT_KEY + "&pullRequest=" + MERGE_REQUEST_IID, BigDecimal.TEN, 11L));
    }

    @Test
    public void shouldThrowErrorWhenSubmitAnalysisToGitlabFails() throws IOException {
        when(analysisDetails.getQualityGateStatus()).thenReturn(QualityGate.Status.ERROR);
        when(analysisDetails.createAnalysisSummary(any())).thenReturn("Different Summary comment");
        when(analysisDetails.getCommitSha()).thenReturn("other sha");
        when(analysisDetails.getCoverage()).thenReturn(Optional.of(BigDecimal.TEN));
        when(analysisDetails.getScannerProperty("com.github.mc1arke.sonarqube.plugin.branch.pullrequest.gitlab.pipelineId")).thenReturn(Optional.of("11"));

        when(server.getPublicRootUrl()).thenReturn("https://sonarqube2.dummy");

        Discussion discussion = mock(Discussion.class);
        when(discussion.getId()).thenReturn("dicussion id 2");
        when(gitlabClient.addMergeRequestDiscussion(anyLong(), anyLong(), any())).thenReturn(discussion);
        doThrow(new IOException("dummy")).when(gitlabClient).addMergeRequestDiscussion(anyLong(), anyLong(), any());

        assertThatThrownBy(() -> underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Could not submit summary comment to Gitlab");

        ArgumentCaptor<MergeRequestNote> mergeRequestNoteArgumentCaptor = ArgumentCaptor.forClass(MergeRequestNote.class);
        verify(gitlabClient).addMergeRequestDiscussion(eq(PROJECT_ID), eq(MERGE_REQUEST_IID), mergeRequestNoteArgumentCaptor.capture());
        verify(gitlabClient, never()).resolveMergeRequestDiscussion(PROJECT_ID, MERGE_REQUEST_IID, discussion.getId());
        verify(gitlabClient, never()).setMergeRequestPipelineStatus(anyLong(), any(), any());

        assertThat(mergeRequestNoteArgumentCaptor.getValue()).isEqualToComparingFieldByField(new MergeRequestNote("Different Summary comment"));
    }

    @Test
    public void shouldReturnWebUrlFromMergeRequestIfScannerPropertyNotSet() {
        assertThat(underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .isEqualToComparingFieldByField(DecorationResult.builder().withPullRequestUrl(MERGE_REQUEST_WEB_URL).build());
    }

    @Test
    public void shouldReturnWebUrlFromScannerPropertyIfSet() {
        when(analysisDetails.getScannerProperty("sonar.pullrequest.gitlab.projectUrl")).thenReturn(Optional.of(MERGE_REQUEST_WEB_URL + "/additional"));
        assertThat(underTest.decorateQualityGateStatus(analysisDetails, almSettingDto, projectAlmSettingDto))
                .isEqualToComparingFieldByField(DecorationResult.builder().withPullRequestUrl(MERGE_REQUEST_WEB_URL + "/additional/merge_requests/" + MERGE_REQUEST_IID).build());
    }
}
