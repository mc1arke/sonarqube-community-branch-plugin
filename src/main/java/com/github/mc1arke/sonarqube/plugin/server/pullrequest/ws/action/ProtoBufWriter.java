package com.github.mc1arke.sonarqube.plugin.server.pullrequest.ws.action;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;

import com.google.protobuf.Message;

@FunctionalInterface
interface ProtoBufWriter {

    void write(Message message, Request request, Response response);

}
