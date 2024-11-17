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
package com.github.mc1arke.sonarqube.plugin.ce.pullrequest.markup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

class NodeTest {

    @Test
    void shouldThrowExceptionOnInvalidChild() {
        BasicValidChildNodeImpl childNode = new BasicValidChildNodeImpl();
        assertThatThrownBy(() -> new BasicInvalidChildNodeImpl(childNode))
                .isExactlyInstanceOf(IllegalArgumentException.class).hasMessage(
                BasicValidChildNodeImpl.class.getName() + " is not a valid child of " +
                BasicInvalidChildNodeImpl.class.getName());
    }

    @Test
    void shouldReturnCorrectChildren() {
        Node node1 = mock();
        Node node2 = mock();
        Node node3 = mock();

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