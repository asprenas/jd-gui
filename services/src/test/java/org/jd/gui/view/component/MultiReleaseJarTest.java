package org.jd.gui.view.component;

import org.jd.gui.api.model.Container;
import org.jd.gui.util.decompiler.DecompilerLoaderFactory;
import org.jd.gui.test.InMemoryContainer;
import org.jd.gui.test.MultiReleaseJarFixture;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiReleaseJarTest {
    @Test
    void usesRuntimeMultiReleaseClassAndPinsDirectVersionEntry() {
        InMemoryContainer container = MultiReleaseJarFixture.create(true);

        ClassFilePage rootPage = new ClassFilePage(MultiReleaseJarFixture.api(), container.getEntry(MultiReleaseJarFixture.CLASS_PATH));
        assertTrue(rootPage.getText().contains("version-17"), rootPage.getText());

        Container.Entry version9Entry = container.getEntry("META-INF/versions/9/" + MultiReleaseJarFixture.CLASS_PATH);
        ClassFilePage version9Page = new ClassFilePage(MultiReleaseJarFixture.api(), version9Entry);
        assertTrue(version9Page.getText().contains("version-9"), version9Page.getText());

        Container.Entry version99Entry = container.getEntry("META-INF/versions/99/" + MultiReleaseJarFixture.CLASS_PATH);
        ClassFilePage version99Page = new ClassFilePage(MultiReleaseJarFixture.api(), version99Entry);
        assertTrue(version99Page.getText().contains("version-too-new"), version99Page.getText());
    }

    @Test
    void rejectsExtremeVersionDirectoryWithoutUnboundedLookup() {
        InMemoryContainer container = MultiReleaseJarFixture.create(true);
        String path = "META-INF/versions/2147483647/" + MultiReleaseJarFixture.CLASS_PATH;
        container.add(path, new byte[] {0});

        IllegalArgumentException error = org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> DecompilerLoaderFactory.create(container.getEntry(path), 17));
        assertTrue(error.getMessage().contains("2147483647"));
    }

    @Test
    void ignoresVersionedClassesWhenManifestIsNotMultiRelease() {
        InMemoryContainer container = MultiReleaseJarFixture.create(false);

        ClassFilePage page = new ClassFilePage(MultiReleaseJarFixture.api(), container.getEntry(MultiReleaseJarFixture.CLASS_PATH));

        assertTrue(page.getText().contains("root-version"), page.getText());
    }

    @Test
    void ignoresVersionedClassesWhenManifestIsMissing() {
        InMemoryContainer container = MultiReleaseJarFixture.createWithoutManifest();

        ClassFilePage page = new ClassFilePage(MultiReleaseJarFixture.api(), container.getEntry(MultiReleaseJarFixture.CLASS_PATH));

        assertTrue(page.getText().contains("root-version"), page.getText());
    }

    @Test
    void loadsReferencedVersionedDependencyUsingTheSameTarget() throws Exception {
        InMemoryContainer container = MultiReleaseJarFixture.create(true);
        DecompilerLoaderFactory.DecompilerInput input = DecompilerLoaderFactory.create(
            container.getEntry(MultiReleaseJarFixture.CLASS_PATH), 17);
        byte[] expected;
        try (var stream = container.getEntry("META-INF/versions/17/" + MultiReleaseJarFixture.DEPENDENCY_PATH).getInputStream()) {
            expected = stream.readAllBytes();
        }

        assertArrayEquals(expected, input.loader().load("sample/Dependency"));
    }
}
