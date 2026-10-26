package org.jd.gui.view.component;

import org.jd.gui.util.FontPreferences;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbstractTextPageFontTest {
    @Test
    void javaAndOtherTextPagesUseIndependentFontPreferences() {
        TestTextPage javaPage = new TestTextPage(true);
        TestTextPage otherPage = new TestTextPage(false);
        Map<String, String> preferences = new HashMap<>();
        preferences.put(FontPreferences.JAVA_FONT_FAMILY, "Dialog");
        preferences.put(FontPreferences.JAVA_FONT_SIZE, "20");
        preferences.put(FontPreferences.OTHER_FONT_FAMILY, "Monospaced");
        preferences.put(FontPreferences.OTHER_FONT_SIZE, "15");

        javaPage.preferencesChanged(preferences);
        otherPage.preferencesChanged(preferences);

        assertEquals("Dialog", javaPage.font().getFamily());
        assertEquals(20, javaPage.font().getSize());
        assertEquals(javaPage.font(), javaPage.lineNumberFont());
        assertEquals("Monospaced", otherPage.font().getFamily());
        assertEquals(15, otherPage.font().getSize());
        assertEquals(otherPage.font(), otherPage.lineNumberFont());
    }

    @Test
    void changingJavaFontSizePersistsAndUpdatesOnlyOpenJavaPages() {
        TestTextPage javaPage = new TestTextPage(true);
        TestTextPage anotherJavaPage = new TestTextPage(true);
        TestTextPage otherPage = new TestTextPage(false);
        Map<String, String> preferences = new HashMap<>();
        preferences.put(FontPreferences.JAVA_FONT_FAMILY, "Dialog");
        preferences.put(FontPreferences.JAVA_FONT_SIZE, "16");
        preferences.put(FontPreferences.OTHER_FONT_FAMILY, "Monospaced");
        preferences.put(FontPreferences.OTHER_FONT_SIZE, "13");
        javaPage.preferencesChanged(preferences);
        anotherJavaPage.preferencesChanged(preferences);
        otherPage.preferencesChanged(preferences);

        javaPage.setSize(24);
        javaPage.persistFontSizeAndPropagate();

        assertEquals("24", preferences.get(FontPreferences.JAVA_FONT_SIZE));
        assertEquals(24, anotherJavaPage.font().getSize());
        assertEquals(13, otherPage.font().getSize());
    }

    private static final class TestTextPage extends AbstractTextPage {
        private final boolean javaSource;

        private TestTextPage(boolean javaSource) { this.javaSource = javaSource; }
        private java.awt.Font font() { return textArea.getFont(); }
        private java.awt.Font lineNumberFont() { return scrollPane.getGutter().getLineNumberFont(); }
        private void setSize(int size) { textArea.setFont(textArea.getFont().deriveFont((float)size)); }
        @Override protected String getFontFamilyPreferenceKey() {
            return javaSource ? FontPreferences.JAVA_FONT_FAMILY : FontPreferences.OTHER_FONT_FAMILY;
        }
        @Override protected String getFontSizePreferenceKey() {
            return javaSource ? FontPreferences.JAVA_FONT_SIZE : FontPreferences.OTHER_FONT_SIZE;
        }
    }
}
