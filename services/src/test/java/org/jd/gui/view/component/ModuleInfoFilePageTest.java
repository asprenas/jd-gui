package org.jd.gui.view.component;

import org.jd.gui.test.InMemoryContainer;
import org.jd.gui.test.MultiReleaseJarFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;

import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModuleInfoFilePageTest {
    @Test
    void preservesModuleAnnotationAndStaticRequirementWithoutSyntheticJavaBase(@TempDir Path tempDir) throws Exception {
        Path sourceDir = tempDir.resolve("source");
        Path classesDir = tempDir.resolve("classes");
        Files.createDirectories(sourceDir);
        Path source = sourceDir.resolve("module-info.java");
        Files.writeString(source, "@Deprecated\nmodule sample.module { requires static java.logging; }\n", StandardCharsets.UTF_8);
        compile(sourceDir, classesDir);
        byte[] original = Files.readAllBytes(classesDir.resolve("module-info.class"));

        InMemoryContainer container = new InMemoryContainer();
        container.add("module-info.class", original);
        ModuleInfoFilePage page = new ModuleInfoFilePage(MultiReleaseJarFixture.api(), container.getEntry("module-info.class"));
        String decompiled = page.getText();

        assertTrue(decompiled.contains("@Deprecated"), decompiled);
        assertTrue(decompiled.contains("requires static java.logging;"), decompiled);
        assertFalse(decompiled.contains("requires java.base;"), decompiled);

        Path outputDir = tempDir.resolve("roundtrip");
        Files.createDirectories(outputDir);
        Files.writeString(outputDir.resolve("module-info.java"), decompiled, StandardCharsets.UTF_8);
        compile(outputDir, outputDir.resolve("classes"));
        byte[] recompiled = Files.readAllBytes(outputDir.resolve("classes/module-info.class"));
        assertEquals(moduleSignature(original), moduleSignature(recompiled));
    }

    private static void compile(Path sourceDir, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        int result = ToolProvider.getSystemJavaCompiler().run(null, null, null,
            "-proc:none", "-d", outputDir.toString(), sourceDir.resolve("module-info.java").toString());
        assertEquals(0, result, "javac failed for " + sourceDir);
    }

    private static List<String> moduleSignature(byte[] classBytes) {
        List<String> signature = new ArrayList<>();
        new ClassReader(classBytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public ModuleVisitor visitModule(String name, int access, String version) {
                signature.add("module:" + name + ":" + access + ":" + version);
                return new ModuleVisitor(Opcodes.ASM9) {
                    @Override public void visitRequire(String module, int access, String version) {
                        signature.add("requires:" + module + ":" + access + ":" + version);
                    }
                };
            }
            @Override public org.objectweb.asm.AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                signature.add("annotation:" + descriptor + ":" + visible);
                return null;
            }
        }, 0);
        signature.sort(String::compareTo);
        return signature;
    }
}
