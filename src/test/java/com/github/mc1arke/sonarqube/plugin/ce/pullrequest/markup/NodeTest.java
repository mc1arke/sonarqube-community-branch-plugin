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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class NodeTest {

    @Test
    public void testExceptionThrownOnInvalidChild() {
        assertThatThrownBy(() -> new BasicInvalidChildNodeImpl(new BasicValidChildNodeImpl()))
                .isExactlyInstanceOf(IllegalArgumentException.class).hasMessage(
                BasicValidChildNodeImpl.class.getName() + " is not a valid child of " +
                BasicInvalidChildNodeImpl.class.getName());
    }

    @Test
    public void testCorrectChildrenReturned() {
        Node node1 = mock(Node.class);
        Node node2 = mock(Node.class);
        Node node3 = mock(Node.class);

        assertThat(new BasicValidChildNodeImpl(node1, node2, node3).getChildren()).containsExactly(node1, node2, node3);
    }

    private static class BasicValidChildNodeImpl extends Node {

        BasicValidChildNodeImpl(Node... children) {
            super(children);
        }

        @Override
        boolean isValidChild(Node child) {
            return true;
        }
    }

    private static class BasicInvalidChildNodeImpl extends Node {

        BasicInvalidChildNodeImpl(Node... children) {
            super(children);
        }

        @Override
        boolean isValidChild(Node child) {
            return false;
        }
    }

}