/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.gui.util.decompiler;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.loader.LoaderException;
import org.jd.gui.api.model.Container;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public class ContainerLoader implements Loader {
    protected byte[] buffer = new byte[1024 * 4];
    protected Container.Entry entry;

    public ContainerLoader() { this.entry = null; }
    public ContainerLoader(Container.Entry entry) {
        this.entry = entry;
    }

    public void setEntry(Container.Entry e) { this.entry = e; }

    protected Container.Entry getEntry(String internalPath) {
        if (entry == null || internalPath == null || internalPath.isEmpty()) {
            return null;
        }

        String path = internalPath + ".class";

        if (entry.getPath().equals(path)) {
            return entry;
        }

        Container.Entry parent = entry.getParent();
        if (parent != null) {
            for (Container.Entry sibling : parent.getChildren()) {
                if (sibling.getPath().equals(path)) {
                    return sibling;
                }
            }
        }

        return findEntryFromRoot(path);
    }

    protected Container.Entry findEntryFromRoot(String path) {
        if (path.startsWith("/")) {
            return null;
        }

        Container container = entry.getContainer();
        Container.Entry root = container == null ? null : container.getRoot();
        if (root == null) {
            return null;
        }

        Container.Entry current = root;
        StringBuilder currentPath = new StringBuilder();
        for (String segment : path.split("/")) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                return null;
            }
            if (currentPath.length() > 0) {
                currentPath.append('/');
            }
            currentPath.append(segment);

            Collection<Container.Entry> children = current.getChildren();
            if (children == null) {
                return null;
            }

            String expectedPath = currentPath.toString();
            Container.Entry next = null;
            for (Container.Entry child : children) {
                if (expectedPath.equals(child.getPath())) {
                    next = child;
                    break;
                }
            }
            if (next == null) {
                return null;
            }
            current = next;
        }
        return current.isDirectory() ? null : current;
    }

    // --- Loader --- //
    @Override
    public boolean canLoad(String internalName) {
        return getEntry(internalName) != null;
    }

    @Override
    public byte[] load(String internalName) throws LoaderException {
        Container.Entry entry = getEntry(internalName);

        if (entry == null) {
            return null;
        } else {
            try (InputStream input=entry.getInputStream(); ByteArrayOutputStream output=new ByteArrayOutputStream()) {
                int len = input.read(buffer);

                while (len > 0) {
                    output.write(buffer, 0, len);
                    len = input.read(buffer);
                }

                return output.toByteArray();
            } catch (IOException e) {
                throw new LoaderException(e);
            }
        }
    }
}
