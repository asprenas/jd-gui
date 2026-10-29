package org.jd.gui.service.treenode;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TextFileTreeNodeFactoryProviderTest {
    @Test
    void registersSpringComponentsAsTextForRootAndNestedPaths() {
        assertTrue(Arrays.asList(new TextFileTreeNodeFactoryProvider().getSelectors())
            .contains("*:file:*/spring.components"));
    }
}
