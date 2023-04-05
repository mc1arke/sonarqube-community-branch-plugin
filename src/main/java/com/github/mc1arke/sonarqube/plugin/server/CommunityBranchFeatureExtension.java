/*
 * Copyright (C) 2019-2023 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.server;

import org.sonar.server.branch.BranchFeatureExtension;

/**
 * Enables branch management in SonarQube.
 *
 * @author Michael Clarke
 */
public class CommunityBranchFeatureExtension implements BranchFeatureExtension {

    @Override
    public String getName() {
        return "branch-support";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

}
