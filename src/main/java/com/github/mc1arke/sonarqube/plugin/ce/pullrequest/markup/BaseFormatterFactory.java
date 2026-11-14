/*
 * Copyright (C) 2019-2026 Michael Clarke
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
        return switch (node) {
            case Document document -> documentFormatter().format(document);
            case Heading heading -> headingFormatter().format(heading);
            case Image image -> imageFormatter().format(image);
            case List list -> listFormatter().format(list);
            case ListItem listItem -> listItemFormatter().format(listItem);
            case Paragraph paragraph -> paragraphFormatter().format(paragraph);
            case Text text -> textFormatter().format(text);
            case Link link -> linkFormatter().format(link);
            case Bold bold -> boldFormatter().format(bold);
            default -> throw new IllegalArgumentException("Unknown node type: " + node.getClass().getName());
        };
    }
}
