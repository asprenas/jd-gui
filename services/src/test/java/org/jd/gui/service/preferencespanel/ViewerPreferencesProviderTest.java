package org.jd.gui.service.preferencespanel;

import org.jd.gui.util.FontPreferences;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;

import static org.junit.jupiter.api.Assertions.*;

class ViewerPreferencesProviderTest {
    @Test
    void migratesLegacySizeToBothTextAreasAndSavesNewKeys() {
        ViewerPreferencesProvider provider = new ViewerPreferencesProvider();
        Map<String, String> preferences = new HashMap<>();
        preferences.put(FontPreferences.LEGACY_FONT_SIZE, "18");

        provider.loadPreferences(preferences);

        assertEquals(18, provider.sizeSelectors.get(FontPreferences.JAVA_FONT_SIZE).getValue());
        assertEquals(18, provider.sizeSelectors.get(FontPreferences.OTHER_FONT_SIZE).getValue());
        assertTrue(provider.arePreferencesValid());

        provider.savePreferences(preferences);

        assertEquals("18", preferences.get(FontPreferences.JAVA_FONT_SIZE));
        assertEquals("18", preferences.get(FontPreferences.OTHER_FONT_SIZE));
        assertFalse(preferences.containsKey(FontPreferences.LEGACY_FONT_SIZE));
        assertTrue(preferences.containsKey(FontPreferences.JAVA_FONT_FAMILY));
        assertTrue(preferences.containsKey(FontPreferences.OTHER_FONT_FAMILY));
        assertTrue(preferences.containsKey(FontPreferences.UI_FONT_FAMILY));
        assertTrue(preferences.containsKey(FontPreferences.UI_FONT_SIZE));
    }

    @Test
    void newSizeKeysTakePrecedenceAndInvalidFamilyFallsBack() {
        ViewerPreferencesProvider provider = new ViewerPreferencesProvider();
        Map<String, String> preferences = new HashMap<>();
        preferences.put(FontPreferences.LEGACY_FONT_SIZE, "18");
        preferences.put(FontPreferences.JAVA_FONT_SIZE, "14");
        preferences.put(FontPreferences.JAVA_FONT_FAMILY, "font-that-is-not-installed");

        provider.loadPreferences(preferences);

        assertEquals(14, provider.sizeSelectors.get(FontPreferences.JAVA_FONT_SIZE).getValue());
        assertEquals(18, provider.sizeSelectors.get(FontPreferences.OTHER_FONT_SIZE).getValue());
        assertTrue(FontPreferences.isAvailable((String)provider.familySelectors.get(FontPreferences.JAVA_FONT_FAMILY).getSelectedItem()));
    }

    @Test
    void missingAndInvalidSizesUseRuntimeDefaultsAndSelectorsEnforceRanges() {
        ViewerPreferencesProvider provider = new ViewerPreferencesProvider();
        Map<String, String> preferences = new HashMap<>();
        preferences.put(FontPreferences.JAVA_FONT_SIZE, "1");
        preferences.put(FontPreferences.OTHER_FONT_SIZE, "41");
        preferences.put(FontPreferences.UI_FONT_SIZE, "not-a-number");

        provider.loadPreferences(preferences);

        int textDefault = Math.max(2, Math.min(40, provider.getThemeFont().getSize()));
        int uiDefault = FontPreferences.getUiDefaultSize();
        assertEquals(textDefault, provider.sizeSelectors.get(FontPreferences.JAVA_FONT_SIZE).getValue());
        assertEquals(textDefault, provider.sizeSelectors.get(FontPreferences.OTHER_FONT_SIZE).getValue());
        assertEquals(uiDefault, provider.sizeSelectors.get(FontPreferences.UI_FONT_SIZE).getValue());

        Map<String, String> unchangedPreferences = new HashMap<>(preferences);
        provider.sizeSelectors.get(FontPreferences.JAVA_FONT_SIZE).setValue(1);
        assertFalse(provider.arePreferencesValid());
        assertThrows(IllegalStateException.class, () -> provider.savePreferences(preferences));
        assertEquals(unchangedPreferences, preferences);

        provider.sizeSelectors.get(FontPreferences.JAVA_FONT_SIZE).setValue(textDefault);
        provider.sizeSelectors.get(FontPreferences.UI_FONT_SIZE).setValue(40);
        assertFalse(provider.arePreferencesValid());
        assertThrows(IllegalStateException.class, () -> provider.savePreferences(preferences));
    }

    @Test
    void longFontNamesDoNotExpandTheSettingsPanel() {
        ViewerPreferencesProvider provider = new ViewerPreferencesProvider();
        provider.loadPreferences(new HashMap<>());
        JComboBox<String> selector = provider.familySelectors.get(FontPreferences.JAVA_FONT_FAMILY);
        int selectorWidth = selector.getPreferredSize().width;
        int panelWidth = provider.getPreferredSize().width;
        String longFamilyName = "A deliberately long installed font family name for layout verification";
        ((DefaultComboBoxModel<String>)selector.getModel()).addElement(longFamilyName);
        selector.setSelectedItem(longFamilyName);

        assertEquals(selectorWidth, selector.getPreferredSize().width);
        assertEquals(panelWidth, provider.getPreferredSize().width);
    }
}
