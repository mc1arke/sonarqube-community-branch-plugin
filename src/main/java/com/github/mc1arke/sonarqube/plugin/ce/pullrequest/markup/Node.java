/*
 * Copyright (C) 2019 Michael Clarke
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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public abstract class Node {

    private final Collection<Node> children = new ArrayList<>();

    Node(Node... children) {
        super();
        Arrays.asList(children).forEach(this::addChild);
    }

    private void addChild(Node child) {
        if (!isValidChild(child)) {
            throw new IllegalArgumentException(
                    String.format("%s is not a valid child of %s", child.getClass().getName(), getClass().getName()));
        }
        children.add(child);
    }

    Collection<Node> getChildren() {
        return Collections.unmodifiableCollection(children);
    }

    abstract boolean isValidChild(Node child);

}
