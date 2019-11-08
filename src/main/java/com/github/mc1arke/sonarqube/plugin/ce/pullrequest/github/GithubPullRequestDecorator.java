/*
 * Copyright (C) 2019 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github;

import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PullRequestBuildStatusDecorator;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.v3.AppInstallation;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.v3.AppToken;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.v3.InstallationRepositories;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.v3.Repository;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.v4.CheckConclusionState;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.v4.CreateCheckRun;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.github.v4.RequestableCheckStatusState;
import com.google.common.reflect.TypeToken;
import com.hazelcast.util.ConcurrentReferenceHashMap;
import io.aexp.nodes.graphql.Argument;
import io.aexp.nodes.graphql.Arguments;
import io.aexp.nodes.graphql.GraphQLRequestEntity;
import io.aexp.nodes.graphql.GraphQLResponseEntity;
import io.aexp.nodes.graphql.GraphQLTemplate;
import io.aexp.nodes.graphql.InputObject;
import io.aexp.nodes.graphql.internal.Error;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultJwtBuilder;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Sequence;
import org.sonar.api.ce.posttask.Analysis;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.platform.Server;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.scanner.protocol.GsonHelper;
import org.sonar.server.measure.Rating;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class GithubPullRequestDecorator implements PullRequestBuildStatusDecorator {

    private static final Logger LOGGER = Loggers.get(GithubPullRequestDecorator.class);
    private static final List<String> OPEN_ISSUE_STATUSES =
            Issue.STATUSES.stream().filter(s -> !Issue.STATUS_CLOSED.equals(s) && !Issue.STATUS_RESOLVED.equals(s))
                    .collect(Collectors.toList());

    private final ConfigurationRepository configurationRepository;
    private final Server server;
    private final MetricRepository metricRepository;
    private final MeasureRepository measureRepository;
    private final TreeRootHolder treeRootHolder;
    private final PostAnalysisIssueVisitor postAnalysisIssueVisitor;

    public GithubPullRequestDecorator(Server server, ConfigurationRepository configurationRepository,
                                      MeasureRepository measureRepository, MetricRepository metricRepository,
                                      TreeRootHolder treeRootHolder,
                                      PostAnalysisIssueVisitor postAnalysisIssueVisitor) {
        super();
        this.configurationRepository = configurationRepository;
        this.server = server;
        this.measureRepository = measureRepository;
        this.metricRepository = metricRepository;
        this.treeRootHolder = treeRootHolder;
        this.postAnalysisIssueVisitor = postAnalysisIssueVisitor;
    }

    @Override
    public void decorateQualityGateStatus(PostProjectAnalysisTask.ProjectAnalysis projectAnalysis) {

        Optional<Analysis> optionalAnalysis = projectAnalysis.getAnalysis();
        if (!optionalAnalysis.isPresent()) {
            LOGGER.warn(
                    "No analysis results were created for this project analysis. This is likely to be due to an earlier failure");
            return;
        }

        Analysis analysis = optionalAnalysis.get();

        Optional<String> revision = analysis.getRevision();
        if (!revision.isPresent()) {
            LOGGER.warn("No commit details were submitted with this analysis. Check the project is committed to Git");
            return;
        }

        if (null == projectAnalysis.getQualityGate()) {
            LOGGER.warn("No quality gate was found on the analysis, so no results are available");
            return;
        }

        String commitId = revision.get();

        try {
            Configuration configuration = configurationRepository.getConfiguration();
            String apiUrl = getMandatoryProperty("sonar.pullrequest.github.endpoint", configuration);
            String apiPrivateKey = getMandatoryProperty("sonar.alm.github.app.privateKey.secured", configuration);
            String projectPath = getMandatoryProperty("sonar.pullrequest.github.repository", configuration);
            String appId = getMandatoryProperty("sonar.alm.github.app.id", configuration);
            String appName = getMandatoryProperty("sonar.alm.github.app.name", configuration);

            RepositoryAuthenticationToken repositoryAuthenticationToken =
                    getInstallationToken(apiUrl, appId, apiPrivateKey, projectPath);
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + repositoryAuthenticationToken.getAuthenticationToken());
            headers.put("Accept", "application/vnd.github.antiope-preview+json");

            String status =
                    (QualityGate.Status.OK == projectAnalysis.getQualityGate().getStatus() ? "Passed" : "Failed");

            List<QualityGate.Condition> failedConditions = projectAnalysis.getQualityGate().getConditions().stream()
                    .filter(c -> c.getStatus() != QualityGate.EvaluationStatus.OK).collect(Collectors.toList());

            Optional<QualityGate.Condition> newCoverageCondition = projectAnalysis.getQualityGate().getConditions().stream()
                    .filter(c -> CoreMetrics.NEW_COVERAGE_KEY.equals(c.getMetricKey())).findFirst();
            String estimatedCoverage = measureRepository
                    .getRawMeasure(treeRootHolder.getRoot(), metricRepository.getByKey(CoreMetrics.COVERAGE_KEY))
                    .map(Measure::getData).orElse("0");

            Optional<QualityGate.Condition> newDuplicationCondition = projectAnalysis.getQualityGate().getConditions().stream()
                    .filter(c -> CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY.equals(c.getMetricKey())).findFirst();
            String estimatedDuplications = measureRepository.getRawMeasure(treeRootHolder.getRoot(), metricRepository
                    .getByKey(CoreMetrics.DUPLICATED_LINES_KEY)).map(Measure::getData).orElse("0");


            Map<RuleType, Long> issueCounts = Arrays.stream(RuleType.values()).collect(Collectors.toMap(k -> k,
                                                                                                        k -> postAnalysisIssueVisitor
                                                                                                                .getIssues()
                                                                                                                .stream()
                                                                                                                .filter(i -> OPEN_ISSUE_STATUSES
                                                                                                                        .contains(
                                                                                                                                i.status()))
                                                                                                                .filter(i -> k ==
                                                                                                                             i.type())
                                                                                                                .count()));
            String newCoverage;
            if (newCoverageCondition.isPresent() && (newCoverageCondition.get().getStatus() != QualityGate.EvaluationStatus.NO_VALUE)) {
                newCoverage = newCoverageCondition.get().getValue() + "% Coverage";
            } else {
                newCoverage = "![No Coverage info](https://raw.githubusercontent.com/SonarSource/sonarcloud-github-static-resources/gh-pages/v2/checks/CoverageChart/NoCoverageInfo.svg?sanitize=true) No coverage information";
            }

            String newDuplication;
            if (newDuplicationCondition.isPresent() && (newDuplicationCondition.get().getStatus() != QualityGate.EvaluationStatus.NO_VALUE)) {
                newDuplication = newDuplicationCondition.get().getValue() + "% Duplicated Code";
            } else {
                newDuplication =  "![No Dublication info](https://raw.githubusercontent.com/SonarSource/sonarcloud-github-static-resources/gh-pages/v2/checks/CoverageChart/NoCoverageInfo.svg?sanitize=true) No Duplication information";
            }

            String summaryBuilder = status + "\n" + failedConditions.stream().filter(c -> c.getStatus() != QualityGate.EvaluationStatus.NO_VALUE)
                    .map(c -> "- " + format(c))
                    .collect(Collectors.joining("\n")) + "\n# Analysis Details\n" + "## " +
                                    issueCounts.entrySet().stream().mapToLong(Map.Entry::getValue).sum() + " Issues\n" +
                                    " - " + pluralOf(issueCounts.get(RuleType.BUG), "Bug", "Bugs") + "\n" + " - " +
                                    pluralOf(issueCounts.get(RuleType.VULNERABILITY) +
                                             issueCounts.get(RuleType.SECURITY_HOTSPOT), "Vulnerability",
                                             "Vulnerabilities") + "\n" + " - " +
                                    pluralOf(issueCounts.get(RuleType.CODE_SMELL), "Code Smell", "Code Smells") + "\n" +
                                    "## Coverage and Duplications\n" + " - " + newCoverage +
                                    "% Coverage (" + estimatedCoverage + "% Estimated after merge)\n" + " - " +
                                    newDuplication + "% Duplicated Code (" + estimatedDuplications +
                                    "% Estimated after merge)\n";

            InputObject<String> checkRunOutputContent =
                    new InputObject.Builder<String>().put("title", "Quality Gate " + status.toLowerCase(Locale.ENGLISH))
                            .put("summary", summaryBuilder).build();

            InputObject<Object> repositoryInputObject =
                    new InputObject.Builder<>().put("repositoryId", repositoryAuthenticationToken.getRepositoryId())
                            .put("name", appName + " Results").put("headSha", commitId)
                            .put("status", RequestableCheckStatusState.COMPLETED).put("conclusion",
                                                                                      QualityGate.Status.OK ==
                                                                                      projectAnalysis.getQualityGate()
                                                                                              .getStatus() ?
                                                                                      CheckConclusionState.SUCCESS :
                                                                                      CheckConclusionState.FAILURE)
                            .put("detailsUrl",
                                 String.format("%s/dashboard?id=%s&pullRequest=%s", server.getPublicRootUrl(),
                                               URLEncoder.encode(projectAnalysis.getProject().getKey(),
                                                                 StandardCharsets.UTF_8.name()), URLEncoder
                                                       .encode(projectAnalysis.getBranch().get().getName().get(),
                                                               StandardCharsets.UTF_8.name()))).put("startedAt",
                                                                                                    new SimpleDateFormat(
                                                                                                            "yyyy-MM-dd'T'HH:mm:ssXXX")
                                                                                                            .format(analysis.getDate()))
                            .put("completedAt", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()))
                            .put("externalId", analysis.getAnalysisUuid()).put("output", checkRunOutputContent).build();


            GraphQLRequestEntity graphQLRequestEntity =
                    GraphQLRequestEntity.Builder().url(apiUrl + "/graphql").headers(headers)
                            .request(CreateCheckRun.class)
                            .arguments(new Arguments("createCheckRun", new Argument<>("input", repositoryInputObject)))
                            .requestMethod(GraphQLTemplate.GraphQLMethod.MUTATE).build();

            LOGGER.debug("Using request: " + graphQLRequestEntity.getRequest());

            GraphQLTemplate graphQLTemplate = new GraphQLTemplate();

            GraphQLResponseEntity<CreateCheckRun> response =
                    graphQLTemplate.mutate(graphQLRequestEntity, CreateCheckRun.class);

            LOGGER.debug("Received response: " + response.toString());

            if (null != response.getErrors() && response.getErrors().length > 0) {
                for (Error error : response.getErrors()) {
                    LOGGER.warn(error.toString());
                }
                throw new IllegalStateException(
                        "An error was returned in the response from the Github API. See the previous log messages for details");
            }
        } catch (IOException | GeneralSecurityException ex) {
            throw new IllegalStateException("Could not decorate Pull Request on Github", ex);
        }

    }

    private static String pluralOf(long value, String singleLabel, String multiLabel) {
        return value + " " + (1 == value ? singleLabel : multiLabel);
    }

    private static RepositoryAuthenticationToken getInstallationToken(String apiUrl, String appId, String apiPrivateKey,
                                                                      String projectPath)
            throws IOException, GeneralSecurityException {

        Calendar expiry = Calendar.getInstance();
        expiry.add(Calendar.MINUTE, 10);
        String jwtToken =
                new DefaultJwtBuilder().setIssuedAt(new Date()).setExpiration(expiry.getTime()).claim("iss", appId)
                        .signWith(createPrivateKey(apiPrivateKey), SignatureAlgorithm.RS256).compact();

        URLConnection appConnection = new URL(apiUrl + "/app/installations").openConnection();
        appConnection.setRequestProperty("Accept", "application/vnd.github.machine-man-preview+json");
        appConnection.setRequestProperty("Authorization", "Bearer " + jwtToken);


        List<AppInstallation> appInstallations;
        try (Reader reader = new InputStreamReader(appConnection.getInputStream())) {
            appInstallations = GsonHelper.create().fromJson(reader, new TypeToken<List<AppInstallation>>() {
            }.getType());
        }


        for (AppInstallation installation : appInstallations) {
            URLConnection accessTokenConnection = new URL(installation.getAccessTokensUrl()).openConnection();
            ((HttpURLConnection) accessTokenConnection).setRequestMethod("POST");
            accessTokenConnection.setRequestProperty("Accept", "application/vnd.github.machine-man-preview+json");
            accessTokenConnection.setRequestProperty("Authorization", "Bearer " + jwtToken);


            try (Reader reader = new InputStreamReader(accessTokenConnection.getInputStream())) {
                AppToken appToken = GsonHelper.create().fromJson(reader, AppToken.class);

                URLConnection installationRepositoriesConnection =
                        new URL(installation.getRepositoriesUrl()).openConnection();
                ((HttpURLConnection) installationRepositoriesConnection).setRequestMethod("GET");
                installationRepositoriesConnection
                        .setRequestProperty("Accept", "application/vnd.github.machine-man-preview+json");
                installationRepositoriesConnection.setRequestProperty("Authorization", "Bearer " + appToken.getToken());
                String repositoryNodeId = null;
                try (Reader installationRepositoriesReader = new InputStreamReader(
                        installationRepositoriesConnection.getInputStream())) {
                    InstallationRepositories installationRepositories = GsonHelper.create()
                            .fromJson(installationRepositoriesReader, InstallationRepositories.class);
                    for (Repository repository : installationRepositories.getRepositories()) {
                        if (projectPath.equals(repository.getFullName())) {
                            repositoryNodeId = repository.getNodeId();
                            break;
                        }
                    }
                    if (null == repositoryNodeId) {
                        continue;
                    }

                }

                return new RepositoryAuthenticationToken(repositoryNodeId, appToken.getToken());

            }
        }

        throw new IllegalStateException(
                "No token could be found with access to the requested repository with the given application ID and key");
    }


    private static PrivateKey createPrivateKey(String apiPrivateKey) throws GeneralSecurityException {
        String privateKeyPem = apiPrivateKey.replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "").replaceAll("\\n", "").replaceAll("\\r", "")
                .replaceAll("\\s+", "").trim();

        ASN1Sequence asn1Sequence = ASN1Sequence.getInstance(Base64.getDecoder().decode(privateKeyPem));

        BigInteger modulus = ((ASN1Integer) asn1Sequence.getObjectAt(1)).getValue();
        BigInteger publicExp = ((ASN1Integer) asn1Sequence.getObjectAt(2)).getValue();
        BigInteger privateExp = ((ASN1Integer) asn1Sequence.getObjectAt(3)).getValue();
        BigInteger prime1 = ((ASN1Integer) asn1Sequence.getObjectAt(4)).getValue();
        BigInteger prime2 = ((ASN1Integer) asn1Sequence.getObjectAt(5)).getValue();
        BigInteger exp1 = ((ASN1Integer) asn1Sequence.getObjectAt(6)).getValue();
        BigInteger exp2 = ((ASN1Integer) asn1Sequence.getObjectAt(7)).getValue();
        BigInteger crtCoefficient = ((ASN1Integer) asn1Sequence.getObjectAt(8)).getValue();


        RSAPrivateCrtKeySpec keySpec =
                new RSAPrivateCrtKeySpec(modulus, publicExp, privateExp, prime1, prime2, exp1, exp2, crtCoefficient);

        return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }


    private static String getMandatoryProperty(String propertyName, Configuration configuration) {
        return configuration.get(propertyName).orElseThrow(() -> new IllegalStateException(
                String.format("%s must be specified in the project configuration", propertyName)));
    }

    private static String format(QualityGate.Condition condition) {
        org.sonar.api.measures.Metric<?> metric = CoreMetrics.getMetric(condition.getMetricKey());
        if (metric.getType() == org.sonar.api.measures.Metric.ValueType.RATING) {
            return String
                    .format("%s %s (%s %s)", Rating.valueOf(Integer.parseInt(condition.getValue())), metric.getName(),
                            condition.getOperator() == QualityGate.Operator.GREATER_THAN ? "is worse than" :
                            "is better than", Rating.valueOf(Integer.parseInt(condition.getErrorThreshold())));
        } else {
            return String.format("%s %s (%s %s)", condition.getValue(), metric.getName(),
                                 condition.getOperator() == QualityGate.Operator.GREATER_THAN ? "is greater than" :
                                 "is less than", condition.getErrorThreshold());
        }
    }

    @Override
    public String name() {
        return "Github";
    }

    private static class RepositoryAuthenticationToken {

        private final String repositoryId;
        private final String authenticationToken;

        private RepositoryAuthenticationToken(String repositoryId, String authenticationToken) {
            super();
            this.repositoryId = repositoryId;
            this.authenticationToken = authenticationToken;
        }

        String getRepositoryId() {
            return repositoryId;
        }

        String getAuthenticationToken() {
            return authenticationToken;
        }
    }
}
