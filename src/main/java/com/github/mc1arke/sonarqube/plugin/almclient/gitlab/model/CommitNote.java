/*
 * Copyright (C) 2021 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.almclient.gitlab.model;

public class CommitNote extends MergeRequestNote {

    private final String baseSha;
    private final String startSha;
    private final String headSha;
    private final String oldPath;
    private final String newPath;
    private final int newLine;

    public CommitNote(String content, String baseSha, String startSha, String headSha, String oldPath, String newPath, int newLine) {
        super(content);
        this.baseSha = baseSha;
        this.startSha = startSha;
        this.headSha = headSha;
        this.oldPath = oldPath;
        this.newPath = newPath;
        this.newLine = newLine;
    }

    public String getBaseSha() {
        return baseSha;
    }

    public String getStartSha() {
        return startSha;
    }

    public String getHeadSha() {
        return headSha;
    }

    public String getOldPath() {
        return oldPath;
    }

    public String getNewPath() {
        return newPath;
    }

    public int getNewLine() {
        return newLine;
    }
}
