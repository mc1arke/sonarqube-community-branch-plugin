package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.azuredevops.model;

public class IdentityRef {
    public IdentityRef(){};
    public SubjectDescriptor Descriptor;
    public String DisplayName;
    public String Url;
    public ReferenceLinks Links;
    public String Id;
    /**
     * Deprecated - use Domain+PrincipalName instead
     */
    public String UniqueName;
    /**
     * Deprecated - Can be retrieved by querying the Graph user referenced in the
     * "self" entry of the IdentityRef "_links" dictionary
     */
    public String DirectoryAlias;
    /**
     * Deprecated - not in use in most preexisting implementations of ToIdentityRef
     */
    public String ProfileUrl;
    /**
     * Deprecated - Available in the "avatar" entry of the IdentityRef "_links" dictionary
     */
    public String ImageUrl;
    /**
     * Deprecated - Can be inferred from the subject type of the descriptor (Descriptor.IsGroupType)
     */
    public Boolean IsContainer;

    /**
     * Deprecated - Can be inferred from the subject type of the descriptor (Descriptor.IsAadUserType/Descriptor.IsAadGroupType)
     */
    public Boolean IsAadIdentity;
    /**
     * Deprecated - Can be retrieved by querying the Graph membership state referenced
     * in the "membershipState" entry of the GraphUser "_links" dictionary
     */
    public Boolean Inactive;
    public Boolean IsDeletedInOrigin;

    /**
     * This property is for xml compat only.
     */
    public String DisplayNameForXmlSerialization;

    /**
     * This property is for xml compat only.
     */
    public String UrlForXmlSerialization;
}