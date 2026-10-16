/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.gui.view.component;

import org.jd.gui.api.model.Container;
import org.jd.gui.test.InMemoryContainer;
import org.jd.gui.test.ParserTestSupport;
import org.jd.gui.test.Snapshot;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.TreeMap;

/**
 * Regression test of the declarations, hyperlinks and strings collected by {@link JavaFilePage}.
 */
public class JavaFilePageParserTest {
    @ParameterizedTest(name = "{0}/{1}")
    @MethodSource("org.jd.gui.test.ParserTestSupport#allFixtures")
    public void testFixture(String root, String path) {
        InMemoryContainer container = InMemoryContainer.fromResources(root);
        Container.Entry entry = container.getEntry(path);
        JavaFilePage page = new JavaFilePage(null, entry);

        Snapshot.assertMatches(ParserTestSupport.snapshotName(root, path) + ".page", dump(page));
    }

    static String dump(JavaFilePage page) {
        StringBuilder sb = new StringBuilder();

        sb.append("declarations:\n");
        for (Map.Entry<String, TypePage.DeclarationData> e : new TreeMap<>(page.declarations).entrySet()) {
            TypePage.DeclarationData d = e.getValue();
            sb.append("  ").append(e.getKey()).append(" @").append(d.startPosition).append('-').append(d.endPosition)
              .append(" type=").append(d.typeName).append(" name=").append(d.name).append(" desc=").append(d.descriptor);
            if (d instanceof JavaFilePage.TypeDeclarationData) {
                sb.append(" super=").append(((JavaFilePage.TypeDeclarationData)d).superTypeName);
            }
            sb.append('\n');
        }

        sb.append("typeDeclarations:\n");
        for (Map.Entry<Integer, TypePage.DeclarationData> e : page.typeDeclarations.entrySet()) {
            sb.append("  @").append(e.getKey()).append(' ').append(e.getValue().typeName).append('\n');
        }

        sb.append("hyperlinks:\n");
        for (HyperlinkPage.HyperlinkData h : page.hyperlinks.values()) {
            sb.append("  @").append(h.startPosition).append('-').append(h.endPosition);
            if (h instanceof TypePage.HyperlinkReferenceData) {
                TypePage.ReferenceData r = ((TypePage.HyperlinkReferenceData)h).reference;
                sb.append(" type=").append(r.typeName).append(" name=").append(r.name)
                  .append(" desc=").append(r.descriptor).append(" owner=").append(r.owner);
            }
            sb.append('\n');
        }

        sb.append("strings:\n");
        for (TypePage.StringData s : page.strings) {
            sb.append("  @").append(s.startPosition).append('-').append(s.endPosition)
              .append(' ').append(s.text).append(" owner=").append(s.owner).append('\n');
        }
        return sb.toString();
    }
}
