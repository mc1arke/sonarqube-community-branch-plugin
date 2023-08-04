package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab;

import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.Discussion;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.Note;
import com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model.User;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class DiscussionMock {
    private static final String PROJECT_KEY = "projectKey";
    private static final String SONARQUBE_USERNAME = "sonarqube@gitlab.dummy";
    private static final String OLD_SONARQUBE_ISSUE_COMMENT = "This issue no longer exists in SonarQube, " +
            "but due to other comments being present in this discussion, " +
            "the discussion is not being being closed automatically. " +
            "Please manually resolve this discussion once the other comments have been reviewed.";
    private static final User sonarqubeUser = new User(SONARQUBE_USERNAME);

    private static Discussion createIssueComment() {
        Note note = createUnresolvedSonarQubeNote(
                "Reported issue\n[View in SonarQube](http://domain.url/sonar/issue?issues=issueKey1&id=" +
                        PROJECT_KEY + ")");
        return new Discussion("issue-comment-id", Collections.singletonList(note));
    }

    private static Discussion createResolvedIssueComment() {
        Note note = createResolvedSonarQubeNote(
                "Reported issue\n[View in SonarQube](http://domain.url/sonar/issue?issues=issueKey1&id=" +
                        PROJECT_KEY + ")");
        return new Discussion("resolved-issue-comment-id", Collections.singletonList(note));
    }

    private static Discussion createResolvedByCommentIssueComment() {
        Note note = createUnresolvedSonarQubeNote(
                "Reported issue\n[View in SonarQube](http://domain.url/sonar/issue?issues=issueKey1&id=" +
                        PROJECT_KEY + ")");
        Note note2 = createUnresolvedSonarQubeNote(OLD_SONARQUBE_ISSUE_COMMENT);
        Note note3 = createUnresolvedSonarQubeNote("Some additional comment");
        return new Discussion("issue-with-resolved-comment-id", Arrays.asList(note, note2, note3));
    }

    private static Discussion createUnresolvedSummaryNote() {
        Note note = createUnresolvedSonarQubeNote(
                "Analysis Details\n[View in SonarQube](http://domain.url/sonar/dashboard?id=" + PROJECT_KEY + ")");
        return new Discussion("summary-note-id", Collections.singletonList(note));
    }

    private static Discussion createResolvedSummaryNote() {
        Note note = createResolvedSonarQubeNote(
                "Analysis Details\n[View in SonarQube](http://domain.url/sonar/dashboard?id=" + PROJECT_KEY + ")");
        return new Discussion("summary-note-id", Collections.singletonList(note));
    }

    private static Note createUnresolvedSonarQubeNote(String body) {
        return new Note(new Random().nextLong(), false, sonarqubeUser, body, false, true);
    }

    private static Note createResolvedSonarQubeNote(String body) {
        return new Note(new Random().nextLong(), false, sonarqubeUser, body, true, true);
    }

    public static Map<DiscussionType, Discussion> getDiscussionsMap(DiscussionType... discussions) {
        return Arrays.stream(discussions)
                .collect(Collectors.toMap(k -> k, DiscussionType::create, (e1, e2) -> e1, LinkedHashMap::new));
    }

    enum DiscussionType {
        RESOLVED_SUMMARY_NOTE(DiscussionMock::createResolvedSummaryNote),
        UNRESOLVED_SUMMARY_NOTE(DiscussionMock::createUnresolvedSummaryNote),
        ISSUE_COMMENT(DiscussionMock::createIssueComment),
        RESOLVED_ISSUE_COMMENT(DiscussionMock::createResolvedIssueComment),
        RESOLVED_BY_COMMENT_ISSUE_COMMENT(DiscussionMock::createResolvedByCommentIssueComment);

        private final Supplier<Discussion> creationMethod;

        DiscussionType(Supplier<Discussion> creationMethod) {
            this.creationMethod = creationMethod;
        }

        public Discussion create() {
            return creationMethod.get();
        }
    }

}
