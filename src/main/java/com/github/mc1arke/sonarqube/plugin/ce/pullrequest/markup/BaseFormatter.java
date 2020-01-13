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

abstract class BaseFormatter<N extends Node> implements Formatter<N> {

    String childContents(Node node, FormatterFactory formatterFactory) {
        StringBuilder output = new StringBuilder();
        node.getChildren().forEach(n -> output.append(formatterFor(formatterFactory, n).format(n, formatterFactory)));
        return output.toString();
    }

    private static <N extends Node> Formatter<N> formatterFor(FormatterFactory formatterFactory, N node) {
        if (node instanceof Document) {
            return (Formatter<N>) formatterFactory.documentFormatter();
        } else if (node instanceof Heading) {
            return (Formatter<N>) formatterFactory.headingFormatter();
        } else if (node instanceof Image) {
            return (Formatter<N>) formatterFactory.imageFormatter();
        } else if (node instanceof List) {
            return (Formatter<N>) formatterFactory.listFormatter();
        } else if (node instanceof ListItem) {
            return (Formatter<N>) formatterFactory.listItemFormatter();
        } else if (node instanceof Paragraph) {
            return (Formatter<N>) formatterFactory.paragraphFormatter();
        } else if (node instanceof Text) {
            return (Formatter<N>) formatterFactory.textFormatter();
        } else if (node instanceof Link) {
            return (Formatter<N>) formatterFactory.linkFormatter();
        } else {
            throw new IllegalArgumentException("Unknown node type: " + node.getClass().getName());
        }
    }
}
