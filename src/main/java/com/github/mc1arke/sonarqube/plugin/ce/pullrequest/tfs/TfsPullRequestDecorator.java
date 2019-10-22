package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.tfs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PullRequestBuildStatusDecorator;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.core.issue.DefaultIssue;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class TfsPullRequestDecorator implements PullRequestBuildStatusDecorator {

    private static final Logger LOGGER = Loggers.get(TfsPullRequestDecorator.class);
    private static final List<String> OPEN_ISSUE_STATUSES =
            org.sonar.api.issue.Issue.STATUSES.stream().filter(s -> !org.sonar.api.issue.Issue.STATUS_CLOSED.equals(s) && !org.sonar.api.issue.Issue.STATUS_RESOLVED.equals(s))
                    .collect(Collectors.toList());

    private final ConfigurationRepository configurationRepository;
    private final PostAnalysisIssueVisitor postAnalysisIssueVisitor;

    public TfsPullRequestDecorator(ConfigurationRepository configurationRepository,
                                   PostAnalysisIssueVisitor postAnalysisIssueVisitor) {
        super();
        this.configurationRepository = configurationRepository;
        this.postAnalysisIssueVisitor = postAnalysisIssueVisitor;
    }

    @Override
    public String name() {
        return "TFS";
    }

    @Override
    public void decorateQualityGateStatus(PostProjectAnalysisTask.ProjectAnalysis projectAnalysis) {
        try {
            Configuration configuration = configurationRepository.getConfiguration();

            List<Issue> issues = postAnalysisIssueVisitor.getIssues().stream()
                    .filter(i -> OPEN_ISSUE_STATUSES.contains(i.status()))
                    .map(this::toTfsIssue)
                    .collect(Collectors.toList());

            Message message = Message.newBuilder()
                    .issues(issues)
                    .projectName(projectAnalysis.getProject().getName())
                    .pullRequestId(Integer.parseInt(projectAnalysis.getBranch().get().getName().get()))
                    .repositoryId(getMandatoryProperty("sonar.pullrequest.vsts.repository", configuration))
                    .build();

            ObjectMapper mapper = new ObjectMapper();
            String value = mapper.writeValueAsString(message);
            LOGGER.info("JSON:" + value);

            URL url = new URL(getMandatoryProperty("sonar.pullrequest.vsts.proxy.url", configuration));

            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setConnectTimeout(10000);
            con.setReadTimeout(60000);
            con.setDoOutput(true);

            try (DataOutputStream out = new DataOutputStream(con.getOutputStream())) {
                out.writeBytes(value);
            }

            LOGGER.info("ResponseCode:" + con.getResponseCode());
            LOGGER.info("ResponseMessage:" + con.getResponseMessage());
        } catch (Exception e) {
            LOGGER.error("SonarQube analysis failed to complete the review of this pull request", e);
        }
    }

    private static String getMandatoryProperty(String propertyName, Configuration configuration) {
        return configuration.get(propertyName).orElseThrow(() -> new IllegalStateException(
                String.format("%s must be specified in the project configuration", propertyName)));
    }

    private Issue toTfsIssue(DefaultIssue sourceIssue) {
        return Issue.newBuilder()
                .key(sourceIssue.key())
                .componentKey(sourceIssue.componentKey())
                .severity(sourceIssue.severity())
                .ruleKey(sourceIssue.ruleKey().toString())
                .message(sourceIssue.message())
                .file(this.postAnalysisIssueVisitor.getIssueMap().get(sourceIssue))
                .line(sourceIssue.line())
                .newIssue(sourceIssue.isNew())
                .build();
    }
}
