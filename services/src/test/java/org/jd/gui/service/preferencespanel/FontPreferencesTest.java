package org.jd.gui.service.preferencespanel;

import org.jd.gui.util.FontPreferences;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FontPreferencesTest {
    @Test
    void logicalFontsAreAlwaysAvailableAndMissingFontsUseFallback() {
        assertTrue(FontPreferences.isAvailable("Monospaced"));
        assertTrue(FontPreferences.isAvailable("Dialog"));
        assertEquals("Monospaced", FontPreferences.resolveFamily("missing-font-family", "Monospaced"));
        assertEquals("Dialog", FontPreferences.resolveFamily("missing-font-family", "also-missing"));
        assertFalse(FontPreferences.getAvailableFontFamilyNames().isEmpty());
    }

    @Test
    void listedFamiliesAreConfirmedByFontConstruction() {
        for (String family : FontPreferences.getAvailableFontFamilyNames()) {
            assertTrue(FontPreferences.isAvailable(family), family);
            assertEquals(family, FontPreferences.resolveFamily(family, "Dialog"));
        }
    }

    @Test
    void uiDefaultsAndExistingWindowTreeReceiveConfiguredFont() throws Exception {
        Map<Object, Object> originalFonts = new HashMap<>();
        for (Object key : UIManager.getDefaults().keySet().toArray()) {
            Object value = UIManager.get(key);
            if (value instanceof Font) originalFonts.put(key, value);
        }

        JFrame window = new JFrame();
        JButton button = new JButton("Button");
        window.add(button);
        try {
            FontPreferences.applyUiFont("Dialog", 16);
            assertEquals("Dialog", UIManager.getFont("Label.font").getFamily());
            assertEquals(16, UIManager.getFont("Label.font").getSize());

            FontPreferences.updateWindowFonts(window, "Dialog", 16);
            assertEquals("Dialog", button.getFont().getFamily());
            assertEquals(16, button.getFont().getSize());
        } finally {
            originalFonts.forEach(UIManager::put);
            window.dispose();
        }
    }
}
