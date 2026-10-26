package org.jd.gui.test;

import org.jd.gui.api.API;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class MultiReleaseJarFixture {
    public static final String CLASS_PATH = "sample/Versioned.class";
    public static final String DEPENDENCY_PATH = "sample/Dependency.class";

    private MultiReleaseJarFixture() { }

    public static InMemoryContainer create(boolean multiRelease) {
        return create(true, multiRelease);
    }

    public static InMemoryContainer createWithoutManifest() {
        return create(false, false);
    }

    private static InMemoryContainer create(boolean includeManifest, boolean multiRelease) {
        InMemoryContainer container = new InMemoryContainer();
        if (includeManifest) {
            String manifest = "Manifest-Version: 1.0\r\n"
                + (multiRelease ? "Multi-Release: true\r\n" : "")
                + "\r\n";
            container.add("META-INF/MANIFEST.MF", manifest.getBytes(StandardCharsets.UTF_8));
        }
        container.add(CLASS_PATH, versionedClass("root-version"));
        container.add("META-INF/versions/9/" + CLASS_PATH, versionedClass("version-9"));
        container.add("META-INF/versions/17/" + CLASS_PATH, versionedClass("version-17"));
        container.add("META-INF/versions/99/" + CLASS_PATH, versionedClass("version-too-new"));
        container.add(DEPENDENCY_PATH, classWithName("sample/Dependency", "root-dependency"));
        container.add("META-INF/versions/9/" + DEPENDENCY_PATH, classWithName("sample/Dependency", "version-9-dependency"));
        container.add("META-INF/versions/17/" + DEPENDENCY_PATH, classWithName("sample/Dependency", "version-17-dependency"));
        container.add("META-INF/versions/99/" + DEPENDENCY_PATH, classWithName("sample/Dependency", "version-too-new-dependency"));
        return container;
    }

    public static API api(Map<String, String> preferences) {
        return (API)Proxy.newProxyInstance(API.class.getClassLoader(), new Class<?>[] { API.class }, (proxy, method, args) -> {
            if ("getPreferences".equals(method.getName())) return preferences;
            Class<?> returnType = method.getReturnType();
            if (returnType == boolean.class) return false;
            if (returnType == int.class) return 0;
            if (returnType == long.class) return 0L;
            if (returnType == float.class) return 0f;
            if (returnType == double.class) return 0d;
            if (returnType == char.class) return '\0';
            if (returnType == short.class) return (short)0;
            if (returnType == byte.class) return (byte)0;
            return null;
        });
    }

    public static API api() {
        Map<String, String> preferences = new HashMap<>();
        preferences.put("JdGuiPreferences.errorBackgroundColor", "#ffffff");
        return api(preferences);
    }

    private static byte[] versionedClass(String marker) {
        return classWithName("sample/Versioned", marker);
    }

    private static byte[] classWithName(String internalName, String marker) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, internalName, null, "java/lang/Object", null);

        MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(1, 1);
        constructor.visitEnd();

        MethodVisitor version = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "version", "()Ljava/lang/String;", null, null);
        version.visitCode();
        version.visitLdcInsn(marker);
        version.visitInsn(Opcodes.ARETURN);
        version.visitMaxs(1, 0);
        version.visitEnd();

        if ("sample/Versioned".equals(internalName)) {
            MethodVisitor dependencyVersion = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "dependencyVersion", "()Ljava/lang/String;", null, null);
            dependencyVersion.visitCode();
            dependencyVersion.visitMethodInsn(Opcodes.INVOKESTATIC, "sample/Dependency", "version", "()Ljava/lang/String;", false);
            dependencyVersion.visitInsn(Opcodes.ARETURN);
            dependencyVersion.visitMaxs(1, 0);
            dependencyVersion.visitEnd();
        }

        writer.visitEnd();
        return writer.toByteArray();
    }
}
