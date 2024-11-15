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

abstract class BaseFormatterFactory implements FormatterFactory {

    protected String childContents(Node node) {
        StringBuilder output = new StringBuilder();
        node.getChildren().forEach(n -> output.append(format(n)));
        return output.toString();
    }

    protected String format(Node node) {
        if (node instanceof Document) {
            return documentFormatter().format((Document) node);
        } else if (node instanceof Heading) {
            return headingFormatter().format((Heading) node);
        } else if (node instanceof Image) {
            return imageFormatter().format((Image) node);
        } else if (node instanceof List) {
            return listFormatter().format((List) node);
        } else if (node instanceof ListItem) {
            return listItemFormatter().format((ListItem) node);
        } else if (node instanceof Paragraph) {
            return paragraphFormatter().format((Paragraph) node);
        } else if (node instanceof Text) {
            return textFormatter().format((Text) node);
        } else if (node instanceof Link) {
            return linkFormatter().format((Link) node);
        } else if (node instanceof Bold) {
            return boldFormatter().format((Bold) node);
        } else {
            throw new IllegalArgumentException("Unknown node type: " + node.getClass().getName());
        }
    }
}
