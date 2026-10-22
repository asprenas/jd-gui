/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.gui.util.parser.antlr;

import org.antlr.v4.runtime.CharStreams;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Deeply nested expressions overflowed the default stack (20 000 '+' operands, 3 000 was the limit).
 */
public class ANTLRJavaParserTest {
    @Test
    public void testLongConcatenation() {
        int count = 20000;
        StringBuilder sb = new StringBuilder("class Big { String s = \"a0\"");
        for (int i = 1; i < count; i++) sb.append(" + \"a").append(i).append('"');
        sb.append("; }");

        int[] literals = {0};
        boolean[] completed = {false};

        ANTLRJavaParser.parse(CharStreams.fromString(sb.toString()), new JavaParserBaseListener() {
            @Override public void enterLiteral(JavaParser.LiteralContext ctx) { literals[0]++; }
            @Override public void exitCompilationunit(JavaParser.CompilationunitContext ctx) { completed[0] = true; }
        });

        assertTrue(completed[0]);
        assertEquals(count, literals[0]);
    }

    @Test
    public void testListenerExceptionIsPropagated() {
        IllegalStateException expected = new IllegalStateException("listener");

        try {
            ANTLRJavaParser.parse(CharStreams.fromString("class A {}"), new JavaParserBaseListener() {
                @Override public void enterClassDeclaration(JavaParser.ClassDeclarationContext ctx) { throw expected; }
            });
        } catch (IllegalStateException e) {
            assertEquals(expected, e);
            return;
        }

        throw new AssertionError("exception not propagated");
    }
}
