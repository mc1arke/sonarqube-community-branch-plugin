package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class IdentityRef implements Serializable {
    private String displayName;
    private String url;
    @JsonProperty("_links")
    private ReferenceLinks links;
    private String id;
    private String uniqueName;
    private String directoryAlias;
    private String profileUrl;
    private String imageUrl;
    @JsonProperty("isContainer")
    private Boolean container;
    @JsonProperty("isAadIdentity")
    private Boolean aadIdentity;
    @JsonProperty("isInactive")
    private Boolean inactive;
    @JsonProperty("isDeletedInOrigin")
    private Boolean deletedInOrigin;
    private String displayNameForXmlSerialization;
    private String urlForXmlSerialization;

    public IdentityRef()
    {};

    public String getDisplayName()
    {
        return this.displayName;
    }
    public String getUrl()
    {
        return this.url;
    }
    public ReferenceLinks getLinks()
    {
        return this.links;
    }
    public String getId()
    {
        return this.id;
    }
    /**
     * Deprecated - use Domain+PrincipalName instead
     */
    public String getUniqueName()
    {
        return this.uniqueName;
    }
    /**
     * Deprecated - Can be retrieved by querying the Graph user referenced in the
     * "self" entry of the IdentityRef "_links" dictionary
     */
    public String getDirectoryAlias()
    {
        return this.directoryAlias;
    }
    /**
     * Deprecated - not in use in most preexisting implementations of ToIdentityRef
     */
    public String getProfileUrl()
    {
        return this.profileUrl;
    }
    /**
     * Deprecated - Available in the "avatar" entry of the IdentityRef "_links" dictionary
     */
    public String getImageUrl()
    {
        return this.imageUrl;
    }
    /**
     * Deprecated - Can be inferred from the subject type of the descriptor (Descriptor.IsGroupType)
     */
    public Boolean isContainer()
    {
        return this.container;
    }

    /**
     * Deprecated - Can be inferred from the subject type of the descriptor (Descriptor.IsAadUserType/Descriptor.IsAadGroupType)
     */
    public Boolean isAadIdentity()
    {
        return this.aadIdentity;
    }
    /**
     * Deprecated - Can be retrieved by querying the Graph membership state referenced
     * in the "membershipState" entry of the GraphUser "_links" dictionary
     */
    public Boolean getInactive()
    {
        return this.inactive;
    }
    public Boolean getIsDeletedInOrigin()
    {
        return this.deletedInOrigin;
    }
    /**
     * This property is for xml compat only.
     */
    public String getdisplayNameForXmlSerialization()
    {
        return this.displayNameForXmlSerialization;
    }
    /**
     * This property is for xml compat only.
     */
    public String getUrlForXmlSerialization()
    {
        return this.urlForXmlSerialization;
    }
}