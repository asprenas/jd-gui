/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.gui.util.parser.antlr;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.jd.gui.util.exception.ExceptionUtil;

public class ANTLRJavaParser {
    /**
     * Parsing and walking are recursive: long expressions (thousands of '+' operands) overflow the default stack.
     * The stack of the parsing thread is reserved, not committed: only used pages consume memory.
     */
    protected static final long STACK_SIZE = 512L * 1024 * 1024;

    public static void parse(CharStream input, JavaParserListener listener) {
        Throwable[] failure = new Throwable[1];
        Thread thread = new Thread(null, () -> {
            try {
                parseInCurrentThread(input, listener);
            } catch (Throwable t) {
                failure[0] = t;
            }
        }, "ANTLRJavaParser", STACK_SIZE);

        thread.start();

        boolean interrupted = false;

        while (true) {
            try {
                thread.join();
                break;
            } catch (InterruptedException e) {
                // The listener is filled by the parsing thread: wait for it, then restore the interrupt status
                interrupted = true;
            }
        }

        if (interrupted) {
            Thread.currentThread().interrupt();
        }

        if (failure[0] instanceof RuntimeException) {
            throw (RuntimeException)failure[0];
        } else if (failure[0] instanceof Error) {
            throw (Error)failure[0];
        }
    }

    protected static void parseInCurrentThread(CharStream input, JavaParserListener listener) {
        try {
            JavaLexer lexer = new JavaLexer(input);

            lexer.removeErrorListeners();

            CommonTokenStream tokens = new CommonTokenStream(lexer);
            JavaParser parser = new JavaParser(tokens);

            parser.removeErrorListeners();

            ParseTree tree = parser.compilationunit();

            ParseTreeWalker.DEFAULT.walk(listener, tree);
        } catch (StackOverflowError e) {
            // Too complex source file, even with a large stack
            assert ExceptionUtil.printStackTrace(e);
        }
    }
}
