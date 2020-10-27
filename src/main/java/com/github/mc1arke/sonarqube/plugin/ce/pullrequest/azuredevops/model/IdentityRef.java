package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class IdentityRef implements Serializable {
    
    private final String displayName;
    private final String url;
    @JsonProperty("_links")
    private final ReferenceLinks links;
    private final String id;
    private final String uniqueName;
    private final String directoryAlias;
    private final String profileUrl;
    private final String imageUrl;
    @JsonProperty("isContainer")
    private final boolean container;
    @JsonProperty("isAadIdentity")
    private final boolean aadIdentity;
    @JsonProperty("isInactive")
    private final boolean inactive;
    @JsonProperty("isDeletedInOrigin")
    private final boolean deletedInOrigin;
    private final String displayNameForXmlSerialization;
    private final String urlForXmlSerialization;
  
    @JsonCreator
    public IdentityRef(@JsonProperty("displayName") String displayName, 
                   @JsonProperty("url") String url,
                   @JsonProperty("_links") ReferenceLinks links, 
                   @JsonProperty("id") String id, 
                   @JsonProperty("uniqueName") String uniqueName, 
                   @JsonProperty("directoryAlias") String directoryAlias,
                   @JsonProperty("profileUrl") String profileUrl, 
                   @JsonProperty("imageUrl") String imageUrl, 
                   @JsonProperty("isContainer") boolean container, 
                   @JsonProperty("isAadIdentity") boolean aadIdentity, 
                   @JsonProperty("isInactive") boolean inactive, 
                   @JsonProperty("isDeletedInOrigin") boolean deletedInOrigin,
                   @JsonProperty("displayNameForXmlSerialization") String displayNameForXmlSerialization,
                   @JsonProperty("urlForXmlSerialization") String urlForXmlSerialization) {

        this.displayName = displayName;
        this.url = url;
        this.links = links;
        this.id = id;
        this.uniqueName = uniqueName;
        this.directoryAlias = directoryAlias;
        this.profileUrl = profileUrl;
        this.imageUrl = imageUrl;
        this.container = container;
        this.aadIdentity = aadIdentity;
        this.inactive = inactive;
        this.deletedInOrigin = deletedInOrigin;
        this.displayNameForXmlSerialization = displayNameForXmlSerialization;
        this.urlForXmlSerialization = urlForXmlSerialization;
    }
}