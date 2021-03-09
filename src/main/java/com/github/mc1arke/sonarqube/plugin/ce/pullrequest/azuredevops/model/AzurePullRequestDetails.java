package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model;

import java.util.Base64;
import java.nio.charset.StandardCharsets;

public class AzurePullRequestDetails {
            
    public static final String API_VERSION_PREFIX = "?api-version=";
    private final String apiVersion;
    private final String authorizationHeader;
    private final String azureRepositoryName;
    private final String azureProjectId;
    private final String azureUrl;
    private final String pullRequestId;    

    public AzurePullRequestDetails(String apiVersion, String azureRepositoryName, String azureProjectId, String azureUrl, 
                                   String personalAccessToken, String pullRequestId) {
        this.apiVersion = apiVersion;
        this.authorizationHeader = generateAuthorizationHeader(personalAccessToken);
        this.azureRepositoryName = azureRepositoryName;
        this.azureProjectId = azureProjectId;
        this.azureUrl = azureUrl;
        this.pullRequestId = pullRequestId;
    }

    private static String generateAuthorizationHeader(String apiToken) {
        String encodeBytes = Base64.getEncoder().encodeToString((":" + apiToken).getBytes(StandardCharsets.UTF_8));
        return "Basic " + encodeBytes;
    }

    public String getApiVersion() { 
        return API_VERSION_PREFIX + this.apiVersion; 
    }
   
    public String getAuthorizationHeader() {
        return this.authorizationHeader;
    }

    public String getAzureRepositoryName() {
        return this.azureRepositoryName;
    }

    public String getAzureProjectId() {
        return this.azureProjectId;
    }

    public String getAzureUrl() {
        return this.azureUrl;
    }

    public String getPullRequestId() {
        return this.pullRequestId;
    }
}