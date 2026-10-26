package org.jd.gui.service.sourcesaver;

import org.jd.gui.test.InMemoryContainer;
import org.jd.gui.test.MultiReleaseJarFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassFileSourceSaverProviderTest {
    @TempDir
    Path tempDirectory;

    @Test
    void exportsRuntimeSelectedMultiReleaseClass() throws Exception {
        InMemoryContainer container = MultiReleaseJarFixture.create(true);
        Map<String, String> preferences = new HashMap<>();
        preferences.put("ClassFileSaverPreferences.writeMetadata", "false");
        preferences.put("ClassFileSaverPreferences.writeLineNumbers", "false");
        Path output = tempDirectory.resolve("Versioned.java");

        new ClassFileSourceSaverProvider().saveContent(
            MultiReleaseJarFixture.api(preferences),
            () -> false,
            path -> { },
            tempDirectory,
            output,
            container.getEntry(MultiReleaseJarFixture.CLASS_PATH));

        assertTrue(Files.readString(output).contains("version-17"), Files.readString(output));
    }
}
