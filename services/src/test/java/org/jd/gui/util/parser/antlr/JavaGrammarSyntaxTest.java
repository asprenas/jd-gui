/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.gui.util.parser.antlr;

import org.jd.gui.test.InMemoryContainer;
import org.jd.gui.test.ParserTestSupport;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Every fixture (Java 7 to Java 24) must be parsed without lexer or parser error.
 */
public class JavaGrammarSyntaxTest {
    @ParameterizedTest(name = "{1}")
    @MethodSource("org.jd.gui.test.ParserTestSupport#allFixtures")
    public void testNoSyntaxError(String root, String path) {
        InMemoryContainer container = InMemoryContainer.fromResources(root);
        List<String> errors = ParserTestSupport.syntaxErrors(container.getEntry(path));

        assertEquals(List.of(), errors, "Syntax errors in " + root + '/' + path);
    }
}
