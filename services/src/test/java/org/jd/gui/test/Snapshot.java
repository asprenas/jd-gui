/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.gui.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Compares a text dump with an expected file in 'src/test/resources/parser/expected'.
 * Run tests with '-PupdateSnapshots' to (re)write the expected files.
 */
public final class Snapshot {
    private static final String RESOURCE_DIR = "parser/expected/";
    private static final Path SOURCE_DIR = Paths.get("src/test/resources", RESOURCE_DIR);

    private Snapshot() {}

    public static void assertMatches(String name, String actual) {
        String fileName = name + ".txt";

        if (Boolean.getBoolean("snapshot.update")) {
            try {
                Files.createDirectories(SOURCE_DIR);
                Files.write(SOURCE_DIR.resolve(fileName), actual.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return;
        }

        try (InputStream is = Snapshot.class.getClassLoader().getResourceAsStream(RESOURCE_DIR + fileName)) {
            assertNotNull(is, "Missing snapshot " + RESOURCE_DIR + fileName + " (run with -PupdateSnapshots)");
            String expected = new String(is.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
            assertEquals(expected, actual, "Snapshot mismatch: " + fileName);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
