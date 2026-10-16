/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.gui.service.type;

import org.jd.gui.api.model.Container;
import org.jd.gui.api.model.Type;
import org.jd.gui.test.InMemoryContainer;
import org.jd.gui.test.ParserTestSupport;
import org.jd.gui.test.Snapshot;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

/**
 * Regression test of the type model built by {@link JavaFileTypeFactoryProvider.Listener}.
 */
public class JavaFileTypeFactoryListenerTest {
    static List<String> java7Files() { return ParserTestSupport.JAVA7_FILES; }

    @ParameterizedTest
    @MethodSource("java7Files")
    public void testJava7(String path) {
        InMemoryContainer container = InMemoryContainer.fromResources(ParserTestSupport.JAVA7_ROOT);
        Container.Entry entry = container.getEntry(path);
        JavaFileTypeFactoryProvider.Listener listener = ParserTestSupport.parse(entry, new JavaFileTypeFactoryProvider.Listener(entry));

        StringBuilder sb = new StringBuilder();
        sb.append("mainType: ").append(listener.getMainType() == null ? null : listener.getMainType().getName()).append('\n');
        for (Type type : listener.getRootTypes()) {
            dump(sb, type, "");
        }

        Snapshot.assertMatches(ParserTestSupport.snapshotName(ParserTestSupport.JAVA7_ROOT, path) + ".types", sb.toString());
    }

    static void dump(StringBuilder sb, Type type, String indent) {
        sb.append(indent).append("type ").append(type.getName())
          .append(" flags=").append(type.getFlags())
          .append(" super=").append(type.getSuperName())
          .append(" outer=").append(type.getOuterName())
          .append(" display=").append(type.getDisplayPackageName()).append('|')
          .append(type.getDisplayTypeName()).append('|').append(type.getDisplayInnerTypeName()).append('\n');
        for (Type.Field field : type.getFields()) {
            sb.append(indent).append("  field ").append(field.getName()).append(' ').append(field.getDescriptor())
              .append(" flags=").append(field.getFlags()).append(" display=").append(field.getDisplayName()).append('\n');
        }
        for (Type.Method method : type.getMethods()) {
            sb.append(indent).append("  method ").append(method.getName()).append(method.getDescriptor())
              .append(" flags=").append(method.getFlags()).append(" display=").append(method.getDisplayName()).append('\n');
        }
        for (Type inner : type.getInnerTypes()) {
            dump(sb, inner, indent + "  ");
        }
    }
}
