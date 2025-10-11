/*
 * Copyright (C) 2019-2025 Michael Clarke
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

abstract class BaseFormatterFactory implements FormatterFactory {

    protected String childContents(Node node) {
        StringBuilder output = new StringBuilder();
        node.getChildren().forEach(n -> output.append(format(n)));
        return output.toString();
    }

    protected String format(Node node) {
        if (node instanceof Document document) {
            return documentFormatter().format(document);
        } else if (node instanceof Heading heading) {
            return headingFormatter().format(heading);
        } else if (node instanceof Image image) {
            return imageFormatter().format(image);
        } else if (node instanceof List list) {
            return listFormatter().format(list);
        } else if (node instanceof ListItem listItem) {
            return listItemFormatter().format(listItem);
        } else if (node instanceof Paragraph paragraph) {
            return paragraphFormatter().format(paragraph);
        } else if (node instanceof Text text) {
            return textFormatter().format(text);
        } else if (node instanceof Link link) {
            return linkFormatter().format(link);
        } else if (node instanceof Bold bold) {
            return boldFormatter().format(bold);
        } else {
            throw new IllegalArgumentException("Unknown node type: " + node.getClass().getName());
        }
    }
}
