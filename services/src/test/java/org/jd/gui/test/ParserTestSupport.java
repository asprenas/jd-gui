/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.gui.test;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.jd.gui.api.model.Container;
import org.jd.gui.util.parser.antlr.ANTLRJavaParser;
import org.jd.gui.util.parser.antlr.AbstractJavaListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public final class ParserTestSupport {
    /** Java 7 fixtures: classpath directory and source files */
    public static final String JAVA7_ROOT = "parser/java7";
    public static final List<String> JAVA7_FILES = List.of(
            "test/fixture/Base.java", "test/fixture/Sample.java", "test/fixture/Shape.java");

    private ParserTestSupport() {}

    public static <L extends AbstractJavaListener> L parse(Container.Entry entry, L listener) {
        try (InputStream is = entry.getInputStream()) {
            ANTLRJavaParser.parse(new ANTLRInputStream(is), listener);
            return listener;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Snapshot name for a fixture file, e.g. "java7.Sample" */
    public static String snapshotName(String root, String path) {
        String file = path.substring(path.lastIndexOf('/') + 1, path.length() - ".java".length());
        return root.substring(root.lastIndexOf('/') + 1) + '.' + file;
    }

    public static String sortedLines(Collection<String> values) {
        return values.stream().sorted().map(v -> "  " + v + "\n").collect(Collectors.joining());
    }
}
