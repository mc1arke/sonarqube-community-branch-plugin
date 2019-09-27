package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.bitbucket.response.diff;

import java.io.Serializable;
import java.util.List;

public class Hunk implements Serializable
{
    private String context;

    private int sourceLine;

    private int sourceSpan;

    private int destinationLine;

    private int destinationSpan;

    private List<Segment> segments;

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public int getSourceLine() {
        return sourceLine;
    }

    public void setSourceLine(int sourceLine) {
        this.sourceLine = sourceLine;
    }

    public int getSourceSpan() {
        return sourceSpan;
    }

    public void setSourceSpan(int sourceSpan) {
        this.sourceSpan = sourceSpan;
    }

    public int getDestinationLine() {
        return destinationLine;
    }

    public void setDestinationLine(int destinationLine) {
        this.destinationLine = destinationLine;
    }

    public int getDestinationSpan() {
        return destinationSpan;
    }

    public void setDestinationSpan(int destinationSpan) {
        this.destinationSpan = destinationSpan;
    }

    public List<Segment> getSegments() {
        return segments;
    }

    public void setSegments(List<Segment> segments) {
        this.segments = segments;
    }
}