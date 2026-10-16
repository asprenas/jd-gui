/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.gui.test;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.jd.gui.api.model.Container;
import org.jd.gui.util.parser.antlr.ANTLRJavaParser;
import org.jd.gui.util.parser.antlr.AbstractJavaListener;
import org.jd.gui.util.parser.antlr.JavaLexer;
import org.jd.gui.util.parser.antlr.JavaParser;
import org.junit.jupiter.params.provider.Arguments;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ParserTestSupport {
    /** Java 7 fixtures: classpath directory and source files */
    public static final String JAVA7_ROOT = "parser/java7";
    public static final List<String> JAVA7_FILES = List.of(
            "test/fixture/Base.java", "test/fixture/Sample.java", "test/fixture/Shape.java");

    /** Java 8 to 24 fixtures */
    public static final String MODERN_ROOT = "parser/modern";
    public static final List<String> MODERN_FILES = List.of(
            "module-info.java", "test/modern/Decompiled.java", "test/modern/Lambdas.java",
            "test/modern/Records.java", "test/modern/Shapes.java", "test/modern/Switches.java");

    private ParserTestSupport() {}

    /** Arguments (root, path) of every fixture */
    public static Stream<Arguments> allFixtures() {
        return Stream.concat(
                JAVA7_FILES.stream().map(path -> Arguments.of(JAVA7_ROOT, path)),
                MODERN_FILES.stream().map(path -> Arguments.of(MODERN_ROOT, path)));
    }

    /** Arguments (root, path) of the Java 7 fixtures */
    public static Stream<Arguments> java7Fixtures() {
        return JAVA7_FILES.stream().map(path -> Arguments.of(JAVA7_ROOT, path));
    }

    /** @return the lexer and parser errors of a source file, as "line:column message" */
    public static List<String> syntaxErrors(Container.Entry entry) {
        List<String> errors = new ArrayList<>();
        BaseErrorListener listener = new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                errors.add(line + ":" + charPositionInLine + " " + msg);
            }
        };

        try (InputStream is = entry.getInputStream()) {
            JavaLexer lexer = new JavaLexer(CharStreams.fromStream(is));
            lexer.removeErrorListeners();
            lexer.addErrorListener(listener);

            JavaParser parser = new JavaParser(new CommonTokenStream(lexer));
            parser.removeErrorListeners();
            parser.addErrorListener(listener);
            parser.compilationunit();
            return errors;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <L extends AbstractJavaListener> L parse(Container.Entry entry, L listener) {
        try (InputStream is = entry.getInputStream()) {
            ANTLRJavaParser.parse(CharStreams.fromStream(is), listener);
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
