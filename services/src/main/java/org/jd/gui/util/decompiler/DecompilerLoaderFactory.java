package org.jd.gui.util.decompiler;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.loader.MultiReleaseLoader;
import org.jd.gui.api.model.Container;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Creates the loader and logical type name used by viewer and source export paths. */
public final class DecompilerLoaderFactory {
    private static final int MAX_VERSION_DIRECTORY = 255;
    private static final Pattern VERSIONED_CLASS_PATH = Pattern.compile("^META-INF/versions/(\\d+)/(.+\\.class)$");

    private DecompilerLoaderFactory() { }

    public static DecompilerInput create(Container.Entry entry, int runtimeFeatureVersion) {
        ContainerLoader delegate = new ContainerLoader(entry);
        String internalName = toInternalName(entry.getPath());

        if (!isMultiRelease(delegate)) {
            return new DecompilerInput(delegate, internalName);
        }

        int targetVersion = Math.min(runtimeFeatureVersion, MAX_VERSION_DIRECTORY);
        Matcher matcher = VERSIONED_CLASS_PATH.matcher(entry.getPath());
        if (matcher.matches()) {
            try {
                int selectedVersion = Integer.parseInt(matcher.group(1));
                if (selectedVersion >= 9 && selectedVersion <= MAX_VERSION_DIRECTORY) {
                    targetVersion = selectedVersion;
                    internalName = toInternalName(matcher.group(2));
                } else if (selectedVersion > MAX_VERSION_DIRECTORY) {
                    // MultiReleaseLoader searches downward from the target version, so reject
                    // implausibly large directory numbers instead of triggering an unbounded scan.
                    throw new IllegalArgumentException("Unsupported multi-release directory version: " + selectedVersion);
                }
            } catch (NumberFormatException ignored) {
                // Invalid version directory names are not Java multi-release versions.
            }
        }

        return new DecompilerInput(new MultiReleaseLoader(delegate, targetVersion), internalName);
    }

    private static String toInternalName(String classPath) {
        return classPath.endsWith(".class") ? classPath.substring(0, classPath.length() - ".class".length()) : classPath;
    }

    private static boolean isMultiRelease(ContainerLoader loader) {
        Container.Entry manifestEntry = loader.findEntryFromRoot("META-INF/MANIFEST.MF");
        if (manifestEntry == null) {
            return false;
        }

        try (InputStream input = manifestEntry.getInputStream()) {
            if (input == null) {
                return false;
            }
            String value = new Manifest(input).getMainAttributes().getValue("Multi-Release");
            return value != null && "true".equalsIgnoreCase(value.trim());
        } catch (IOException | RuntimeException ignored) {
            return false;
        }
    }

    public record DecompilerInput(Loader loader, String internalName) { }
}
