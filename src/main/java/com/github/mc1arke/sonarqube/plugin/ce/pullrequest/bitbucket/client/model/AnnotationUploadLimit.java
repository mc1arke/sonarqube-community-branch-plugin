package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.client.model;

/**
 * Determines the different upload limits for both bitbucket cloud and bitbucket server.
 */
public class AnnotationUploadLimit {
    private final int annotationBatchSize;
    private final int totalAllowedAnnotations;

    public AnnotationUploadLimit(int annotationBatchSize, int totalAllowedAnnotations) {
        this.annotationBatchSize = annotationBatchSize;
        this.totalAllowedAnnotations = totalAllowedAnnotations;
    }

    public int getAnnotationBatchSize() {
        return annotationBatchSize;
    }

    public int getTotalAllowedAnnotations() {
        return totalAllowedAnnotations;
    }
}
