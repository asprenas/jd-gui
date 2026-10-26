/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.gui.service.preferencespanel;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.jd.gui.spi.PreferencesPanel;
import org.jd.gui.util.FontPreferences;
import org.jd.gui.util.exception.ExceptionUtil;
import org.jd.gui.view.component.AbstractTextPage;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ViewerPreferencesProvider extends JPanel implements PreferencesPanel, ChangeListener {
    protected static final int MIN_TEXT_SIZE = 2;
    protected static final int MAX_TEXT_SIZE = 40;
    protected static final int MIN_UI_SIZE = 8;
    protected static final int MAX_UI_SIZE = 32;

    protected final Map<String, JComboBox<String>> familySelectors = new LinkedHashMap<>();
    protected final Map<String, JSpinner> sizeSelectors = new LinkedHashMap<>();
    protected PreferencesPanel.PreferencesPanelChangeListener listener;
    protected Color errorBackgroundColor = Color.RED;

    public ViewerPreferencesProvider() {
        super(new GridBagLayout());
        addGroup(0, "Java source", FontPreferences.JAVA_FONT_FAMILY, FontPreferences.JAVA_FONT_SIZE, MIN_TEXT_SIZE, MAX_TEXT_SIZE);
        addGroup(1, "Other text", FontPreferences.OTHER_FONT_FAMILY, FontPreferences.OTHER_FONT_SIZE, MIN_TEXT_SIZE, MAX_TEXT_SIZE);
        addGroup(2, "UI", FontPreferences.UI_FONT_FAMILY, FontPreferences.UI_FONT_SIZE, MIN_UI_SIZE, MAX_UI_SIZE);
    }

    protected void addGroup(int row, String title, String familyKey, String sizeKey, int minSize, int maxSize) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.insets = new Insets(4, 4, 4, 8);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0;
        JLabel familyLabel = new JLabel(title + " font:");
        add(familyLabel, c);
        JComboBox<String> family = new JComboBox<>(FontPreferences.getAvailableFontFamilyNames().toArray(new String[0]));
        family.setEditable(false);
        family.setPrototypeDisplayValue("International Font Family");
        family.setPreferredSize(new Dimension(220, family.getPreferredSize().height));
        familyLabel.setLabelFor(family);
        family.addActionListener(e -> onChange());
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        add(family, c);
        familySelectors.put(familyKey, family);

        c.gridx = 2;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        add(new JLabel("Size:"), c);
        JSpinner size = new JSpinner(new SpinnerNumberModel(minSize, minSize, maxSize, 1));
        size.addChangeListener(this);
        c.gridx = 3;
        add(size, c);
        sizeSelectors.put(sizeKey, size);
    }

    @Override public String getPreferencesGroupTitle() { return "Viewer"; }
    @Override public String getPreferencesPanelTitle() { return "Appearance"; }
    @Override public JComponent getPanel() { return this; }
    @Override public void init(Color errorBackgroundColor) { this.errorBackgroundColor = errorBackgroundColor; }
    @Override public boolean isActivated() { return true; }

    @Override
    public void loadPreferences(Map<String, String> preferences) {
        Font textDefault = getThemeFont();
        Font uiDefault = UIManager.getFont("Label.font");
        if (uiDefault == null) uiDefault = new Font("Dialog", Font.PLAIN, 12);

        setFamily(preferences, FontPreferences.JAVA_FONT_FAMILY, textDefault.getFamily(), "Monospaced");
        setFamily(preferences, FontPreferences.OTHER_FONT_FAMILY, textDefault.getFamily(), "Monospaced");
        setFamily(preferences, FontPreferences.UI_FONT_FAMILY, uiDefault.getFamily(), "Dialog");

        String legacySize = preferences.get(FontPreferences.LEGACY_FONT_SIZE);
        setSize(preferences, FontPreferences.JAVA_FONT_SIZE, textDefault.getSize(), legacySize, MIN_TEXT_SIZE, MAX_TEXT_SIZE);
        setSize(preferences, FontPreferences.OTHER_FONT_SIZE, textDefault.getSize(), legacySize, MIN_TEXT_SIZE, MAX_TEXT_SIZE);
        setSize(preferences, FontPreferences.UI_FONT_SIZE, uiDefault.getSize(), null, MIN_UI_SIZE, MAX_UI_SIZE);
    }

    protected void setFamily(Map<String, String> preferences, String key, String fallback, String logicalFallback) {
        String value = FontPreferences.resolveFamily(preferences.get(key), FontPreferences.resolveFamily(fallback, logicalFallback));
        familySelectors.get(key).setSelectedItem(value);
    }

    protected void setSize(Map<String, String> preferences, String key, int defaultSize, String legacySize, int min, int max) {
        int value = parseSize(preferences.get(key), -1);
        if (value < 0) value = parseSize(legacySize, defaultSize);
        if (value < min || value > max) value = Math.max(min, Math.min(max, defaultSize));
        sizeSelectors.get(key).setValue(value);
    }

    protected int parseSize(String value, int defaultValue) {
        try { return value == null ? defaultValue : Integer.parseInt(value); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    protected Font getThemeFont() {
        RSyntaxTextArea textArea = new RSyntaxTextArea();
        try {
            Theme theme = Theme.load(getClass().getClassLoader().getResourceAsStream(AbstractTextPage.THEME_RESOURCE));
            if (theme != null) theme.apply(textArea);
        } catch (IOException e) {
            assert ExceptionUtil.printStackTrace(e);
        }
        Font font = textArea.getFont();
        return font == null ? new Font("Monospaced", Font.PLAIN, 12) : font;
    }

    @Override
    public void savePreferences(Map<String, String> preferences) {
        if (!arePreferencesValid()) {
            throw new IllegalStateException("Font preferences contain an unsupported family or size.");
        }
        familySelectors.forEach((key, selector) -> preferences.put(key, String.valueOf(selector.getSelectedItem())));
        sizeSelectors.forEach((key, selector) -> preferences.put(key, String.valueOf(selector.getValue())));
        preferences.remove(FontPreferences.LEGACY_FONT_SIZE);
    }

    @Override
    public boolean arePreferencesValid() {
        boolean validFamilies = familySelectors.values().stream().allMatch(selector ->
            selector.getSelectedItem() instanceof String && FontPreferences.isAvailable((String)selector.getSelectedItem()));
        if (!validFamilies) return false;

        for (Map.Entry<String, JSpinner> entry : sizeSelectors.entrySet()) {
            Object value = entry.getValue().getValue();
            if (!(value instanceof Number)) return false;
            int size = ((Number)value).intValue();
            int min = FontPreferences.UI_FONT_SIZE.equals(entry.getKey()) ? MIN_UI_SIZE : MIN_TEXT_SIZE;
            int max = FontPreferences.UI_FONT_SIZE.equals(entry.getKey()) ? MAX_UI_SIZE : MAX_TEXT_SIZE;
            if (size < min || size > max) return false;
        }
        return true;
    }
    @Override public void addPreferencesChangeListener(PreferencesPanel.PreferencesPanelChangeListener listener) { this.listener = listener; }
    @Override public void stateChanged(ChangeEvent e) { onChange(); }

    protected void onChange() {
        for (JComboBox<String> selector : familySelectors.values()) {
            selector.setBackground(arePreferencesValid() ? UIManager.getColor("ComboBox.background") : errorBackgroundColor);
        }
        if (listener != null) listener.preferencesPanelChanged(this);
    }
}
