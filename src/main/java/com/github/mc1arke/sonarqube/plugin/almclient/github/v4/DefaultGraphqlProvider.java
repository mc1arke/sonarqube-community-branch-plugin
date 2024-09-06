/*
 * Copyright (C) 2019-2024 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.almclient.github.v4;

import io.aexp.nodes.graphql.GraphQLRequestEntity;
import io.aexp.nodes.graphql.GraphQLTemplate;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;

@ComputeEngineSide
@ServerSide
public final class DefaultGraphqlProvider implements GraphqlProvider {

    @Override
    public GraphQLTemplate createGraphQLTemplate() {
        return new GraphQLTemplate();
    }

    @Override
    public GraphQLRequestEntity.RequestBuilder createRequestBuilder() {
        return GraphQLRequestEntity.Builder();
    }

    @Override
    public <T> InputObject.Builder<T> createInputObject() {
        return new InputObject.Builder<>();
    }
}
