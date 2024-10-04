package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.pullrequest.action;

import org.apache.commons.codec.digest.DigestUtils;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.issue.IssueDao;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.rule.RuleDao;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.pullrequest.action.ListAction.checkPermission;

public class GitLabReportAction extends ProjectWsAction {

    private static final String PULL_REQUEST_PARAMETER = "pullRequest";

    private static final Map<String, String> ruleKeyToCheckName = new HashMap<>();

    private final UserSession userSession;

    @Autowired
    public GitLabReportAction(DbClient dbClient, ComponentFinder componentFinder, UserSession userSession) {
        super("gitlab_report", dbClient, componentFinder);
        this.userSession = userSession;
    }

    @Override
    protected void configureAction(WebService.NewAction action) {
        action.createParam(PULL_REQUEST_PARAMETER).setRequired(true);
    }

    @Override
    protected void handleProjectRequest(ProjectDto project, Request request, Response response, DbSession dbSession) {
        checkPermission(project, userSession);

        String pullRequestId = request.mandatoryParam(PULL_REQUEST_PARAMETER);

        BranchDto pullRequest = getDbClient()
                .branchDao()
                .selectByPullRequestKey(dbSession, project.getUuid(), pullRequestId)
                .filter(branch -> branch.getBranchType() == BranchType.PULL_REQUEST)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Pull request '%s' is not found for project '%s'", pullRequestId,
                                project.getKey())));

        IssueDao issueDao = getDbClient().issueDao();
        RuleDao ruleDao = getDbClient().ruleDao();
        // selectOpenByComponentUuids seems to not work as expected
        Set<String> issueKeys = issueDao.selectIssueKeysByComponentUuid(dbSession, pullRequest.getUuid());

        JsonWriter writer = response.newJsonWriter();
        writer.beginArray();
        issueDao
                .selectByKeys(dbSession, issueKeys)
                .stream()
                .filter(i -> i.getIssueStatus() == IssueStatus.OPEN)
                .forEach(i -> buildGitLabIssue(dbSession, i, ruleDao, writer));
        writer.endArray();
        writer.close();
    }

    private void buildGitLabIssue(DbSession dbSession, IssueDto issue, RuleDao ruleDao, JsonWriter writer) {
        writer.beginObject();
        // https://docs.gitlab.com/ee/ci/testing/code_quality.html#implement-a-custom-tool
        writer.prop("check_name", getCheckName(dbSession, ruleDao, issue.getRuleKey()));
        writer.prop("fingerprint", DigestUtils.md5Hex(issue.getKey()));
        writer.prop("description", issue.getMessage());
        writer.prop("severity", issue.getSeverity().toLowerCase());
        // Location
        buildLocation(issue, writer);
        writer.endObject();
    }

    private void buildLocation(IssueDto issue, JsonWriter jsonWriter) {
        jsonWriter.name("location").beginObject();
        jsonWriter.prop("path", issue.getFilePath());
        jsonWriter.name("lines").beginObject();
        jsonWriter.prop("begin", issue.getLine());
        jsonWriter.prop("end", issue.getLine());
        jsonWriter.endObject();
        jsonWriter.endObject();
    }

    private String getCheckName(DbSession dbSession, RuleDao ruleDao, RuleKey ruleKey) {
        return ruleKeyToCheckName.computeIfAbsent(ruleKey.rule(), k -> ruleDao
                .selectByKey(dbSession, ruleKey)
                .map(r -> r.getName().toLowerCase().replaceAll("[^a-zA-Z ]", "").replaceAll("\\s+", "-"))
                .orElse(ruleKey.toString()));
    }

}
