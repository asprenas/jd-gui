package org.jd.gui.view.component;

import org.jd.gui.test.InMemoryContainer;
import org.jd.gui.test.MultiReleaseJarFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.ToolProvider;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URLClassLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JDCore121RegressionTest {
    @Test
    void decompilesGenericCallWithoutRawArgumentCastAndRecompiles(@TempDir Path tempDir) throws Exception {
        String source = "package p; public class GenericCase {"
            + "interface Supplier<T> { T get(); } interface Processor<T> { T result(); }"
            + "static Supplier<java.io.FileInputStream> newSupplier(java.io.File f) { return null; }"
            + "static <T> T readBytes(Supplier<? extends java.io.InputStream> s, Processor<T> p) { return p.result(); }"
            + "public static <T> T read(java.io.File f, Processor<T> p) { return readBytes(newSupplier(f), p); }"
            + "}";
        String result = decompile("p/GenericCase", source, tempDir.resolve("input"));
        assertTrue(result.contains("return readBytes(newSupplier("), result);
        assertTrue(!result.contains("readBytes((Supplier)"), result);
        assertCompiles("p/GenericCase", result, tempDir.resolve("output"));
    }

    @Test
    void decompilesLabeledBlockControlFlowAndRecompiles(@TempDir Path tempDir) throws Exception {
        String source = "package p; public class LabeledCase {"
            + "public int parse(CharSequence text, int position, boolean negative, boolean expect) {"
            + "int offset = 0; parse: { if (expect) { if (text.charAt(position) != ':') break parse; position++; }"
            + "offset = text.length(); } return negative ? -offset : offset; } }";
        Path inputWorkspace = tempDir.resolve("input");
        String result = decompile("p/LabeledCase", source, inputWorkspace);
        assertTrue(result.contains("paramCharSequence.charAt(paramInt)"), result);
        assertTrue(result.contains("return paramBoolean1 ? -i : i;"), result);
        Path recompiledClasses = assertCompiles("p/LabeledCase", result, tempDir.resolve("output"));
        assertParseBehaviorMatches(inputWorkspace.resolve("classes"), recompiledClasses);
    }

    private static String decompile(String internalName, String source, Path root) throws Exception {
        Path sourceFile = root.resolve(internalName + ".java");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, source, StandardCharsets.UTF_8);
        Path classes = root.resolve("classes");
        Files.createDirectories(classes);
        int compilation = ToolProvider.getSystemJavaCompiler().run(null, null, null, "-proc:none", "-d", classes.toString(), sourceFile.toString());
        assertEquals(0, compilation);
        InMemoryContainer container = new InMemoryContainer();
        try (var classFiles = Files.walk(classes)) {
            classFiles.filter(path -> path.toString().endsWith(".class")).forEach(path -> {
                try {
                    String pathInContainer = classes.relativize(path).toString().replace(java.io.File.separatorChar, '/');
                    container.add(pathInContainer, Files.readAllBytes(path));
                } catch (java.io.IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            });
        }
        return new ClassFilePage(MultiReleaseJarFixture.api(), container.getEntry(internalName + ".class")).getText();
    }

    private static Path assertCompiles(String internalName, String source, Path root) throws Exception {
        Path sourceFile = root.resolve(internalName + ".java");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, source, StandardCharsets.UTF_8);
        Path classes = root.resolve("classes");
        Files.createDirectories(classes);
        int compilation = ToolProvider.getSystemJavaCompiler().run(null, null, null, "-proc:none", "-d", classes.toString(), sourceFile.toString());
        assertEquals(0, compilation, source);
        return classes;
    }

    private static void assertParseBehaviorMatches(Path originalClasses, Path recompiledClasses) throws Exception {
        try (var originalLoader = new URLClassLoader(new java.net.URL[] {originalClasses.toUri().toURL()}, ClassLoader.getPlatformClassLoader());
             var recompiledLoader = new URLClassLoader(new java.net.URL[] {recompiledClasses.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {
            Object original = originalLoader.loadClass("p.LabeledCase").getConstructor().newInstance();
            Object recompiled = recompiledLoader.loadClass("p.LabeledCase").getConstructor().newInstance();
            var originalParse = original.getClass().getMethod("parse", CharSequence.class, int.class, boolean.class, boolean.class);
            var recompiledParse = recompiled.getClass().getMethod("parse", CharSequence.class, int.class, boolean.class, boolean.class);
            for (String text : new String[] {":abc", "xabc"}) {
                for (boolean negative : new boolean[] {false, true}) {
                    for (boolean expect : new boolean[] {false, true}) {
                        Object[] arguments = {text, 0, negative, expect};
                        assertEquals(originalParse.invoke(original, arguments), recompiledParse.invoke(recompiled, arguments),
                            "behavior differs for text=" + text + ", negative=" + negative + ", expect=" + expect);
                    }
                }
            }
        }
    }
}
