/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.gui.service.indexer;

import org.jd.gui.api.model.Container;
import org.jd.gui.test.InMemoryContainer;
import org.jd.gui.test.ParserTestSupport;
import org.jd.gui.test.Snapshot;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Regression test of the type/member index built by {@link JavaFileIndexerProvider.Listener}.
 */
public class JavaFileIndexerListenerTest {
    static List<String> java7Files() { return ParserTestSupport.JAVA7_FILES; }

    @ParameterizedTest
    @MethodSource("java7Files")
    public void testJava7(String path) {
        InMemoryContainer container = InMemoryContainer.fromResources(ParserTestSupport.JAVA7_ROOT);
        Container.Entry entry = container.getEntry(path);
        JavaFileIndexerProvider.Listener listener = ParserTestSupport.parse(entry, new JavaFileIndexerProvider.Listener(entry));

        Snapshot.assertMatches(ParserTestSupport.snapshotName(ParserTestSupport.JAVA7_ROOT, path) + ".index", dump(listener));
    }

    static String dump(JavaFileIndexerProvider.Listener listener) {
        StringBuilder sb = new StringBuilder();
        section(sb, "typeDeclarations", listener.getTypeDeclarationSet());
        section(sb, "constructorDeclarations", listener.getConstructorDeclarationSet());
        section(sb, "methodDeclarations", listener.getMethodDeclarationSet());
        section(sb, "fieldDeclarations", listener.getFieldDeclarationSet());
        section(sb, "typeReferences", listener.getTypeReferenceSet());
        section(sb, "constructorReferences", listener.getConstructorReferenceSet());
        section(sb, "methodReferences", listener.getMethodReferenceSet());
        section(sb, "fieldReferences", listener.getFieldReferenceSet());
        section(sb, "strings", listener.getStringSet());
        sb.append("superTypeNames:\n");
        for (Map.Entry<String, HashSet<String>> e : new TreeMap<>(listener.getSuperTypeNamesMap()).entrySet()) {
            sb.append("  ").append(e.getKey()).append(" -> ").append(e.getValue().stream().sorted().toList()).append('\n');
        }
        return sb.toString();
    }

    static void section(StringBuilder sb, String name, java.util.Collection<String> values) {
        sb.append(name).append(":\n").append(ParserTestSupport.sortedLines(values));
    }
}
