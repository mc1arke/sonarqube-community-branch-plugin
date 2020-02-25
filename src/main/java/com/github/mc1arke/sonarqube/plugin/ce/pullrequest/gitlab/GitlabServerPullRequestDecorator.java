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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.AnalysisDetails;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PostAnalysisIssueVisitor;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.PullRequestBuildStatusDecorator;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.response.Commit;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.response.DiffRefs;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.response.Discussion;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.response.MergeRequest;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.response.Note;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.response.Position;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.gitlab.response.User;
import com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup.MarkdownFormatterFactory;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.ce.task.projectanalysis.scm.Changeset;
import org.sonar.ce.task.projectanalysis.scm.ScmInfo;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepository;

public class GitlabServerPullRequestDecorator implements PullRequestBuildStatusDecorator {

    private static final Logger LOGGER = Loggers.get(GitlabServerPullRequestDecorator.class);
    private static final List<String> OPEN_ISSUE_STATUSES =
            Issue.STATUSES.stream().filter(s -> !Issue.STATUS_CLOSED.equals(s) && !Issue.STATUS_RESOLVED.equals(s))
                    .collect(Collectors.toList());
    public static final String PULLREQUEST_GITLAB_URL = "com.github.mc1arke.sonarqube.plugin.branch.pullrequest.gitlab.url";
    public static final String PULLREQUEST_GITLAB_TOKEN = "com.github.mc1arke.sonarqube.plugin.branch.pullrequest.gitlab.token";
    public static final String PULLREQUEST_GITLAB_REPOSITORY_SLUG = "com.github.mc1arke.sonarqube.plugin.branch.pullrequest.gitlab.repositorySlug";
	public static final String PULLREQUEST_COMPACT_COMMENTS_ENABLED = "com.github.mc1arke.sonarqube.plugin.branch.pullrequest.gitlab.compactComments";
	public static final String PULLREQUEST_CAN_FAIL_PIPELINE_ENABLED = "com.github.mc1arke.sonarqube.plugin.branch.pullrequest.gitlab.canFailPipeline";


    private final ConfigurationRepository configurationRepository;
    private final Server server;
    private final ScmInfoRepository scmInfoRepository;

    public GitlabServerPullRequestDecorator(Server server, ConfigurationRepository configurationRepository, ScmInfoRepository scmInfoRepository) {
        super();
        this.configurationRepository = configurationRepository;
        this.server = server;
        this.scmInfoRepository = scmInfoRepository;
    }

    @Override
    public void decorateQualityGateStatus(AnalysisDetails analysis) {
        LOGGER.info("starting to analyze with {}", analysis);

        try {
	        final String pullRequestId = analysis.getBranchName();
            User user = getUser();
            List<Discussion> discussions = getOwnDiscussions(user, pullRequestId);
            final boolean deleteCommentsEnabled = getMandatoryBooleanProperty(PULL_REQUEST_DELETE_COMMENTS_ENABLED);
            
            Map<String, Note> lineComments = new HashMap<>();
            for (Discussion discussion : discussions) {
                for (Note note : discussion.getNotes()) {
                     Position position = note.getPosition();
                    if ("DiffNote".equals(note.getType()) && position!=null && position.getNewLine()!=null && position.getNewLine().trim().length() > 0) {
                    	lineComments.put(position.getNewLine(), note);	
                    } else if (deleteCommentsEnabled) {
                    	deleteCommitDiscussionNote(pullRequestId, discussion.getId(), note.getId());
                    }
	            }
            }
            doPipeline(analysis);
            doSummary(analysis);
            doFileComments(analysis, lineComments);
            if (deleteCommentsEnabled) {
	            for( Note note: lineComments.values()) {
	            	deleteCommitDiscussionNote(pullRequestId, note.getDiscussionId(), note.getId());            	
	            }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not decorate Pull Request on Gitlab Server", ex);
        }

    }


	protected void doFileComments(AnalysisDetails analysis, Map<String, Note> lineNotes) throws IOException {
		// Issues
        final boolean fileCommentEnabled = getMandatoryBooleanProperty(PULL_REQUEST_FILE_COMMENT_ENABLED);
        final boolean compactCommentsEnabled = getMandatoryBooleanProperty(PULLREQUEST_COMPACT_COMMENTS_ENABLED);
		if (fileCommentEnabled) {
			Map<String, String> headers = getHeaders();
	        final String pullRequestId = analysis.getBranchName();
            final String mergeRequestDiscussionURL = getMergeRequestDiskussionsURL(pullRequestId);
            MergeRequest mergeRequest = getMergeRequest(pullRequestId);
            DiffRefs diffRefs = mergeRequest.getDiffRefs();
            List<String> commits = getCommits(pullRequestId);
		    List<PostAnalysisIssueVisitor.ComponentIssue> openIssues = analysis.getPostAnalysisIssueVisitor().getIssues().stream().filter(i -> OPEN_ISSUE_STATUSES.contains(i.getIssue().getStatus())).collect(Collectors.toList());
		    for (PostAnalysisIssueVisitor.ComponentIssue issue : openIssues) {
		        String path = analysis.getSCMPathForIssue(issue).orElse(null);
		        if (path != null && issue.getIssue().getLine() != null) {
		            //only if we have a path and line number
		            String fileComment = analysis.createAnalysisIssueSummary(issue, new MarkdownFormatterFactory(), compactCommentsEnabled);

					if (getScmInfoForComponent(issue.getComponent())
		                    .filter(i -> i.hasChangesetForLine(issue.getIssue().getLine()))
		                    .map(i -> i.getChangesetForLine(issue.getIssue().getLine()))
		                    .map(Changeset::getRevision)
		                    .filter(commits::contains)
		                    .isPresent()) {
		                //only if the change is on a commit, that belongs to this MR
						String lineNr = String.valueOf(issue.getIssue().getLine());
						Note existingNote = lineNotes.get(lineNr);
						if (existingNote!=null) {
		                    updateCommitComment(mergeRequestDiscussionURL,  existingNote.getDiscussionId(), existingNote.getId(), headers, fileComment);							
		                    lineNotes.remove(lineNr);
						} else {
							List<NameValuePair> fileContentParams = Arrays.asList(
			                        new BasicNameValuePair("body", fileComment),
			                        new BasicNameValuePair("position[base_sha]", diffRefs.getBaseSha()),
			                        new BasicNameValuePair("position[start_sha]", diffRefs.getStartSha()),
			                        new BasicNameValuePair("position[head_sha]", diffRefs.getHeadSha()),
			                        new BasicNameValuePair("position[old_path]", path),
			                        new BasicNameValuePair("position[new_path]", path),
			                        new BasicNameValuePair("position[new_line]", lineNr),
			                        new BasicNameValuePair("position[position_type]", "text"));
	
			                try {
			                    postCommitComment(mergeRequestDiscussionURL, headers, fileContentParams);
			                } catch (IOException ex) {
			                	LOGGER.error("Can't post issue comment on line '{}' to '{}'.", issue.getIssue().getLine(), mergeRequestDiscussionURL);
			                }
						}

		            } else {
		                LOGGER.info(String.format("Skipping %s:%d since the commit does not belong to the MR", path, issue.getIssue().getLine()));
		            }
		        }
			}
		}
	}

	protected void doSummary(AnalysisDetails analysis) throws UnsupportedEncodingException {
        final boolean summaryCommentEnabled = getMandatoryBooleanProperty(PULL_REQUEST_COMMENT_SUMMARY_ENABLED);
		if (summaryCommentEnabled) {
	        final String pullRequestId = analysis.getBranchName();
			String mergeRequestDiscussionURL = getMergeRequestDiskussionsURL(pullRequestId);
  		    Map<String, String> headers = getHeaders();
		    String summaryComment = analysis.createAnalysisSummary(new MarkdownFormatterFactory());
		    List<NameValuePair> summaryContentParams = Collections.singletonList(new BasicNameValuePair("body", summaryComment));
		    try {
		        postCommitComment(mergeRequestDiscussionURL, headers, summaryContentParams);
		    } catch (IOException ex) {
		    	LOGGER.error("Can't post summary comment to '{}'.", mergeRequestDiscussionURL);
		    }
		}
	}

	private void doPipeline(AnalysisDetails analysis) throws UnsupportedEncodingException {
        final boolean canFailPipeline = getMandatoryBooleanProperty(PULLREQUEST_CAN_FAIL_PIPELINE_ENABLED);
		Map<String, String> headers = getHeaders();
        String revision = analysis.getCommitSha();
        final String statusUrl = getStatusURL(revision);
		LOGGER.info(String.format("Status url is: %s ", statusUrl));
		String coverageValue = analysis.findQualityGateCondition(CoreMetrics.NEW_COVERAGE_KEY)
		        .filter(condition -> condition.getStatus() != QualityGate.EvaluationStatus.NO_VALUE)
		        .map(QualityGate.Condition::getValue)
		        .orElse("0");

		try {
		    postStatus(statusUrl, headers, analysis, coverageValue, canFailPipeline);
		} catch (IOException ex) {
			LOGGER.error("Can't post status to '{}'.", statusUrl);
		}
	}

	private List<Discussion> getOwnDiscussions(User user, String pullRequestId)
			throws IOException {
		Map<String, String> headers = getHeaders();
        final String mergeRequestDiscussionURL = getMergeRequestDiskussionsURL(pullRequestId);
		List<Discussion> discussions = getPagedList(mergeRequestDiscussionURL, headers, new TypeReference<List<Discussion>>() {
			});
		List<Discussion> result = new ArrayList<>();
		for(Discussion discussion: discussions) {
			if (discussion.getNotes()!=null && discussion.getNotes().size()>0) {
				Note firstNote = discussion.getNotes().get(0);
				if (!firstNote.isSystem() 
						&& firstNote.getAuthor() != null && firstNote.getAuthor().getUsername().equals(user.getUsername())
						&& ("DiffNote".equals(firstNote.getType()) || "DiscussionNote".equals(firstNote.getType()))) {
					firstNote.setDiscussionId(discussion.getId());
					result.add(discussion);
				}
			}
		}
        LOGGER.info(String.format("Discussions in MR: %s ", result
                .stream()
                .map(Discussion::getId)
                .collect(Collectors.joining(", "))));
		return result;
	}

	private MergeRequest getMergeRequest(final String pullRequestId) throws IOException {
		Map<String, String> headers = getHeaders();
        final String mergeRequestURL = getMergeRequestURL(pullRequestId);
		return getSingle(mergeRequestURL, headers, MergeRequest.class);
	}

	
	private List<String> getCommits(final String pullRequestId) throws IOException {
		Map<String, String> headers = getHeaders();
        final String prCommitsURL = getMergeRequestCommitsURL(pullRequestId);
		return getPagedList(prCommitsURL, headers,  new TypeReference<List<Commit>>() {
		}).stream().map(Commit::getId).collect(Collectors.toList());
	}
	
	private User getUser() throws IOException {
        final String hostURL = getMandatoryProperty(PULLREQUEST_GITLAB_URL);
        final String userURL = getUserURL(hostURL);
        LOGGER.info(String.format("User url is: %s ", userURL));
		Map<String, String> headers = getHeaders();
		User result = getSingle(userURL, headers, User.class);
        LOGGER.info(String.format("Using user: %s ", result.getUsername()));
		return result;
	}

    private <X> X getSingle(String userURL, Map<String, String> headers, Class<X> type) throws IOException {
        HttpGet httpGet = new HttpGet(userURL);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpGet.addHeader(entry.getKey(), entry.getValue());
        }
        HttpResponse httpResponse = HttpClients.createDefault().execute(httpGet);
        if (null != httpResponse && httpResponse.getStatusLine().getStatusCode() != 200) {
            LOGGER.error(httpResponse.toString());
            LOGGER.error(EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8));
            throw new IllegalStateException("An error was returned in the response from the Gitlab API. See the previous log messages for details");
        } else if (null != httpResponse) {
            LOGGER.debug(httpResponse.toString());
            HttpEntity entity = httpResponse.getEntity();
            X user = new ObjectMapper()
                    .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
                    .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .readValue(IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8), type);

            LOGGER.info(type + " received");

            return user;
        } else {
            throw new IOException("No response reveived");
        }
    }

    private <X> List<X> getPagedList(String commitDiscussionURL, Map<String, String> headers, TypeReference<List<X>> typeRef) throws IOException {
        HttpGet httpGet = new HttpGet(commitDiscussionURL);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpGet.addHeader(entry.getKey(), entry.getValue());
        }

        List<X> discussions = new ArrayList<>();

        HttpResponse httpResponse = HttpClients.createDefault().execute(httpGet);
        if (null != httpResponse && httpResponse.getStatusLine().getStatusCode() != 200) {
            LOGGER.error(httpResponse.toString());
            LOGGER.error(EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8));
            throw new IllegalStateException("An error was returned in the response from the Gitlab API. See the previous log messages for details");
        } else if (null != httpResponse) {
            LOGGER.debug(httpResponse.toString());
            HttpEntity entity = httpResponse.getEntity();
            List<X> pagedDiscussions = new ObjectMapper()
                    .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
                    .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .readValue(IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8), typeRef);
            discussions.addAll(pagedDiscussions);
            LOGGER.info("MR discussions received");
            Optional<String> nextURL = getNextUrl(httpResponse);
            if (nextURL.isPresent()) {
                LOGGER.info("Getting next page");
                discussions.addAll(getPagedList(nextURL.get(), headers, typeRef));
            }
        }
        return discussions;
    }

    private void deleteCommitDiscussionNote(String pullRequestId, String discussionId, long noteId)  {
        //https://docs.gitlab.com/ee/api/discussions.html#delete-a-commit-thread-note
    	String commitDiscussionNoteURL = null;
    	try {
	        commitDiscussionNoteURL = getNoteUrl(pullRequestId, discussionId, noteId);
	    	Map<String, String> headers = getHeaders();
	        HttpDelete httpDelete = new HttpDelete(commitDiscussionNoteURL);
	        for (Map.Entry<String, String> entry : headers.entrySet()) {
	            httpDelete.addHeader(entry.getKey(), entry.getValue());
	        }
	        LOGGER.info("Deleting {} with headers {}", commitDiscussionNoteURL, headers);
            HttpResponse httpResponse = HttpClients.createDefault().execute(httpDelete);
            validateGitlabResponse(httpResponse, 204, "Commit discussions note deleted");
        } catch (IOException ex) {
        	LOGGER.error("Can't delete note '{}'", commitDiscussionNoteURL);
        }
    }

    private void postCommitComment(String commitCommentUrl, Map<String, String> headers, List<NameValuePair> params) throws IOException {
        //https://docs.gitlab.com/ee/api/commits.html#post-comment-to-commit
        HttpPost httpPost = new HttpPost(commitCommentUrl);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpPost.addHeader(entry.getKey(), entry.getValue());
        }
        httpPost.setEntity(new UrlEncodedFormEntity(params));
        LOGGER.info("Posting {} with headers {} to {}", params, headers, commitCommentUrl);

        HttpResponse httpResponse = HttpClients.createDefault().execute(httpPost);
        validateGitlabResponse(httpResponse, 201, "Comment posted");
    }
    
    
	private void updateCommitComment(String commitCommentUrl, String discussionId, long noteId, Map<String, String> headers, String body) throws IOException {
        //https://docs.gitlab.com/ee/api/notes.html#modify-existing-merge-request-note
        String commitCommentModificationUrl = commitCommentUrl + "/" + discussionId + "/notes/" + noteId;

		HttpPut httpPut = new HttpPut(commitCommentModificationUrl);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpPut.addHeader(entry.getKey(), entry.getValue());
        }
        httpPut.setEntity(new UrlEncodedFormEntity(Collections.singletonList(new BasicNameValuePair("body", body))));
        LOGGER.info("Posting {} with headers {} to {}", body, headers, commitCommentModificationUrl);

        HttpResponse httpResponse = HttpClients.createDefault().execute(httpPut);
        validateGitlabResponse(httpResponse, 200, null);
		
	}

    private void postStatus(String statusPostUrl, Map<String, String> headers, AnalysisDetails analysis, String coverage, boolean canFailPipeline) throws IOException{
        //See https://docs.gitlab.com/ee/api/commits.html#post-the-build-status-to-a-commit
        statusPostUrl += "?name=SonarQube";
        String status = (!canFailPipeline || analysis.getQualityGateStatus() == QualityGate.Status.OK ? "success" : "failed");
        statusPostUrl += "&state=" + status;
        statusPostUrl += "&target_url=" + URLEncoder.encode(String.format("%s/dashboard?id=%s&pullRequest=%s", getServerPublicRootUrl(),
                URLEncoder.encode(analysis.getAnalysisProjectKey(),
                        StandardCharsets.UTF_8.name()), URLEncoder
                        .encode(analysis.getBranchName(),
                                StandardCharsets.UTF_8.name())), StandardCharsets.UTF_8.name());
        statusPostUrl+="&description="+URLEncoder.encode("SonarQube Status", StandardCharsets.UTF_8.name());
        statusPostUrl+="&coverage="+coverage;
        //TODO: add pipelineId if we have it

        HttpPost httpPost = new HttpPost(statusPostUrl);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            httpPost.addHeader(entry.getKey(), entry.getValue());
        }
        HttpResponse httpResponse = HttpClients.createDefault().execute(httpPost);
        if (null != httpResponse && httpResponse.toString().contains("Cannot transition status")) {
            // Workaround for https://gitlab.com/gitlab-org/gitlab-ce/issues/25807
            LOGGER.debug("Transition status is already {}", status);
        } else {
            validateGitlabResponse(httpResponse, 201, "Comment posted");
        }
    }


    private void validateGitlabResponse(HttpResponse httpResponse, int expectedStatus, String successLogMessage) throws IOException {
        if (null != httpResponse && httpResponse.getStatusLine().getStatusCode() != expectedStatus) {
            LOGGER.error(httpResponse.toString());
            LOGGER.error(EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8));
            throw new IllegalStateException("An error was returned in the response from the Gitlab API. See the previous log messages for details");
        } else if (null != httpResponse) {
            LOGGER.debug(httpResponse.toString());
            LOGGER.info(successLogMessage);
        }
    }

    protected boolean getMandatoryBooleanProperty(String propertyName) {
        return Boolean.parseBoolean(getMandatoryProperty(propertyName));
    }

    protected String getMandatoryProperty(String propertyName) {
        return getMandatoryProperty(propertyName, getConfiguration());
    }

    protected static String getMandatoryProperty(String propertyName, Configuration configuration) {
        return configuration.get(propertyName).orElseThrow(() -> new IllegalStateException(
                String.format("%s must be specified in the project configuration", propertyName)));
    }

    private static Optional<String> getNextUrl(HttpResponse httpResponse) {
        Header linkHeader = httpResponse.getFirstHeader("Link");
        if (linkHeader != null) {
            Matcher matcher = Pattern.compile("<([^>]+)>;[\\s]*rel=\"([a-z]+)\"").matcher(linkHeader.getValue());
            while (matcher.find()) {
                if (matcher.group(2).equals("next")) {
                    //found the next rel return the URL
                    return Optional.of(matcher.group(1));
                }
            }
        }
        return Optional.empty();
    }
    
	protected Map<String, String> getHeaders() {
		Map<String, String> headers = new HashMap<>();
        final String apiToken = getMandatoryProperty(PULLREQUEST_GITLAB_TOKEN);
		headers.put("PRIVATE-TOKEN", apiToken);
		headers.put("Accept", "application/json");
		return headers;
	}
    
	protected Optional<ScmInfo> getScmInfoForComponent(Component component) {
		return scmInfoRepository.getScmInfo(component);
	}
    
	protected Configuration getConfiguration() {
		return configurationRepository.getConfiguration();
	}
    
	protected String getServerPublicRootUrl() {
		return server.getPublicRootUrl();
	}
	
	protected String getRestURL(final String hostURL) {
		return String.format("%s/api/v4", hostURL);
	}

	protected String getMergeRequestDiskussionsURL(final String pullRequestId) throws UnsupportedEncodingException {
		final String mergeRequestURl = getMergeRequestURL(pullRequestId);
		String result = mergeRequestURl + "/discussions";
        LOGGER.info(String.format("MR discussion url is: %s ", result));
		return result;
	}

	protected String getMergeRequestCommitsURL(final String pullRequestId) throws UnsupportedEncodingException {
		final String mergeRequestURL = getMergeRequestURL(pullRequestId);
		String result = mergeRequestURL + "/commits";
        LOGGER.info(String.format("PR commits url is: %s ", result));
		return result;
	}

	protected String getMergeRequestURL(final String pullRequestId) throws UnsupportedEncodingException {
        final String projectURL = getProjectURL();
		return projectURL + String.format("/merge_requests/%s", pullRequestId);
	}
	
	protected String getNoteUrl(String pullRequestId, String discussionId, long noteId) throws UnsupportedEncodingException{
		String mergeRequestDiscussionURL = getMergeRequestDiskussionsURL(pullRequestId);
		return mergeRequestDiscussionURL + String.format("/%s/notes/%s",  discussionId, noteId);
	}

	protected String getStatusURL(String revision) throws UnsupportedEncodingException {
        final String projectURL = getProjectURL();
		return projectURL + String.format("/statuses/%s", revision);
	}

	protected String getProjectURL() throws UnsupportedEncodingException {
	    final String hostURL = getMandatoryProperty(PULLREQUEST_GITLAB_URL);
        final String repositorySlug = getMandatoryProperty(PULLREQUEST_GITLAB_REPOSITORY_SLUG);
        final String restURL = getRestURL(hostURL);
		return restURL + String.format("/projects/%s", URLEncoder.encode(repositorySlug, StandardCharsets.UTF_8.name()));
	}

	protected String getUserURL(final String hostURL) {
        final String restURL = getRestURL(hostURL);
		return restURL + "/user";
	}


    @Override
    public String name() {
        return "GitlabServer";
    }
}
