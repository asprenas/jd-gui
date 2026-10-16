/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.gui.test;

import org.jd.gui.api.model.Container;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * Container backed by a test resource directory, used to feed ANTLR listeners in tests.
 */
public class InMemoryContainer implements Container {
    protected final EntryImpl root = new EntryImpl(this, null, "", true, null);

    /**
     * @param resourceDirectory classpath directory used as container root, e.g. "parser/java7"
     */
    public static InMemoryContainer fromResources(String resourceDirectory) {
        URL url = InMemoryContainer.class.getClassLoader().getResource(resourceDirectory);
        if (url == null) {
            throw new IllegalArgumentException("Resource directory not found: " + resourceDirectory);
        }

        try {
            Path base = Paths.get(url.toURI());
            InMemoryContainer container = new InMemoryContainer();

            try (Stream<Path> paths = Files.walk(base)) {
                paths.filter(Files::isRegularFile).sorted().forEach(path -> {
                    String relative = base.relativize(path).toString().replace(java.io.File.separatorChar, '/');
                    try {
                        container.add(relative, Files.readAllBytes(path));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }
            return container;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    public void add(String path, byte[] content) {
        EntryImpl parent = root;
        String[] segments = path.split("/");
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < segments.length - 1; i++) {
            if (current.length() > 0) current.append('/');
            current.append(segments[i]);
            parent = parent.getOrCreateDirectory(current.toString());
        }

        parent.children.put(path, new EntryImpl(this, parent, path, false, content));
    }

    public Entry getEntry(String path) {
        EntryImpl entry = root;
        String[] segments = path.split("/");
        StringBuilder current = new StringBuilder();

        for (String segment : segments) {
            if (current.length() > 0) current.append('/');
            current.append(segment);
            entry = entry.children.get(current.toString());
            if (entry == null) {
                throw new IllegalArgumentException("Entry not found: " + path);
            }
        }
        return entry;
    }

    @Override public String getType() { return "test"; }
    @Override public Entry getRoot() { return root; }

    protected static class EntryImpl implements Entry {
        protected final InMemoryContainer container;
        protected final EntryImpl parent;
        protected final String path;
        protected final boolean directory;
        protected final byte[] content;
        protected final Map<String, EntryImpl> children = new TreeMap<>();

        EntryImpl(InMemoryContainer container, EntryImpl parent, String path, boolean directory, byte[] content) {
            this.container = container;
            this.parent = parent;
            this.path = path;
            this.directory = directory;
            this.content = content;
        }

        EntryImpl getOrCreateDirectory(String path) {
            return children.computeIfAbsent(path, p -> new EntryImpl(container, this, p, true, null));
        }

        @Override public Container getContainer() { return container; }
        @Override public Entry getParent() { return parent; }
        @Override public URI getUri() { return URI.create("test:/" + path); }
        @Override public String getPath() { return path; }
        @Override public boolean isDirectory() { return directory; }
        @Override public long length() { return directory ? 0 : content.length; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(directory ? new byte[0] : content); }
        @Override public Collection<Entry> getChildren() { return Collections.unmodifiableCollection(new ArrayList<>(children.values())); }
    }
}
