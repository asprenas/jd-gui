/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.gui.util;

import javax.swing.UIManager;
import javax.swing.SwingUtilities;
import javax.swing.plaf.FontUIResource;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Shared preference keys and portable font-family helpers. */
public final class FontPreferences {
    public static final String JAVA_FONT_FAMILY = "ViewerPreferences.javaFontFamily";
    public static final String JAVA_FONT_SIZE = "ViewerPreferences.javaFontSize";
    public static final String OTHER_FONT_FAMILY = "ViewerPreferences.otherFontFamily";
    public static final String OTHER_FONT_SIZE = "ViewerPreferences.otherFontSize";
    public static final String UI_FONT_FAMILY = "ViewerPreferences.uiFontFamily";
    public static final String UI_FONT_SIZE = "ViewerPreferences.uiFontSize";
    public static final String LEGACY_FONT_SIZE = "ViewerPreferences.fontSize";

    private static final String[] LOGICAL_FONTS = {"Dialog", "DialogInput", "Monospaced", "SansSerif", "Serif"};
    private static final List<String> AVAILABLE_FONTS = loadAvailableFontFamilyNames();

    private FontPreferences() { }

    public static List<String> getAvailableFontFamilyNames() {
        return AVAILABLE_FONTS;
    }

    private static List<String> loadAvailableFontFamilyNames() {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        try {
            names.addAll(Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        } catch (RuntimeException ignored) {
            // Keep the portable Java logical families available in restricted/headless environments.
        }
        names.addAll(Arrays.asList(LOGICAL_FONTS));
        return Collections.unmodifiableList(new ArrayList<>(names));
    }

    public static boolean isAvailable(String family) {
        return findUsableFamily(family) != null;
    }

    public static String resolveFamily(String requestedFamily, String fallbackFamily) {
        String resolved = findUsableFamily(requestedFamily);
        if (resolved != null) return resolved;
        resolved = findUsableFamily(fallbackFamily);
        if (resolved != null) return resolved;
        return "Dialog";
    }

    public static String getUiDefaultFamily() {
        Font font = UIManager.getFont("Label.font");
        return resolveFamily(font == null ? null : font.getFamily(), "Dialog");
    }

    public static int getUiDefaultSize() {
        Font font = UIManager.getFont("Label.font");
        return font == null ? 12 : Math.max(8, Math.min(32, font.getSize()));
    }

    private static String findUsableFamily(String family) {
        if (family == null || family.trim().isEmpty()) return null;

        for (String available : getAvailableFontFamilyNames()) {
            if (available.equalsIgnoreCase(family.trim())) {
                // Font silently substitutes Dialog for unknown names, so check the result too.
                Font resolved = new Font(available, Font.PLAIN, 12);
                return available.equalsIgnoreCase(resolved.getFamily()) ? available : null;
            }
        }
        return null;
    }

    public static Font createFont(String family, int style, float size, String fallbackFamily) {
        String resolved = resolveFamily(family, fallbackFamily);
        return new Font(resolved, style, 1).deriveFont(size);
    }

    public static void applyUiFont(String family, int size) {
        String resolved = resolveFamily(family, getUiDefaultFamily());
        for (Object key : UIManager.getDefaults().keySet().toArray()) {
            Object value = UIManager.get(key);
            if (value instanceof Font) {
                Font font = (Font)value;
                UIManager.put(key, new FontUIResource(resolved, font.getStyle(), size));
            }
        }
    }

    public static void updateWindowFonts(Window window, String family, int size) {
        if (window == null) return;
        SwingUtilities.updateComponentTreeUI(window);
        applyFontRecursively(window, resolveFamily(family, getUiDefaultFamily()), size);
        window.invalidate();
        window.validate();
        window.repaint();
    }

    private static void applyFontRecursively(Component component, String family, int size) {
        Font font = component.getFont();
        if (font != null) component.setFont(new Font(family, font.getStyle(), size));
        if (component instanceof Container) {
            for (Component child : ((Container)component).getComponents()) {
                applyFontRecursively(child, family, size);
            }
        }
    }

}
