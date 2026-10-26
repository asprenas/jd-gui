package org.jd.gui.util.decompiler;

import org.jd.gui.api.model.Container;
import org.jd.gui.test.InMemoryContainer;
import org.jd.core.v1.api.loader.MultiReleaseLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContainerLoaderTest {
    @Test
    void resolvesArchiveRootAndVersionedEntries() throws Exception {
        byte[] rootClass = {1, 2, 3};
        byte[] versionedClass = {17, 18, 19};
        byte[] versionedDependency = {27, 28, 29};
        InMemoryContainer container = new InMemoryContainer();
        container.add("com/example/Root.class", rootClass);
        container.add("META-INF/versions/17/com/example/Versioned.class", versionedClass);
        container.add("com/example/Dependency.class", new byte[] {4, 5, 6});
        container.add("META-INF/versions/17/com/example/Dependency.class", versionedDependency);

        Container.Entry selectedEntry = container.getEntry("com/example/Root.class");
        ContainerLoader loader = new ContainerLoader(selectedEntry);

        assertTrue(loader.canLoad("com/example/Root"));
        assertArrayEquals(rootClass, loader.load("com/example/Root"));
        assertTrue(loader.canLoad("META-INF/versions/17/com/example/Versioned"));
        assertArrayEquals(versionedClass, loader.load("META-INF/versions/17/com/example/Versioned"));
        MultiReleaseLoader multiReleaseLoader = new MultiReleaseLoader(loader, 17);
        assertArrayEquals(versionedClass, multiReleaseLoader.load("com/example/Versioned"));
        assertArrayEquals(versionedDependency, multiReleaseLoader.load("com/example/Dependency"));
        assertFalse(loader.canLoad("com/example/Missing"));
        assertNull(loader.load("com/example/Missing"));
    }
}
