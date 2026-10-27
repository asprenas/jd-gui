/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.gui.util.swing;

import javax.swing.*;
import javax.swing.plaf.basic.BasicMenuItemUI;
import javax.swing.plaf.basic.BasicMenuUI;
import java.awt.*;

/** Applies the compact menu spacing used by JD-GUI menus and popups. */
public final class CompactMenuLayout {
    private CompactMenuLayout() { }

    public static void apply(JMenu menu) {
        menu.setUI(new CompactMenuUI());
        menu.setMargin(new Insets(1, 8, 1, 8));
        menu.setIconTextGap(3);
        for (Component component : menu.getMenuComponents()) {
            if (component instanceof JMenu) {
                apply((JMenu)component);
            } else if (component instanceof JMenuItem) {
                apply((JMenuItem)component);
            }
        }
    }

    public static void applyRecentFiles(JMenu menu) {
        menu.setUI(new CompactMenuUI());
        menu.setMargin(new Insets(1, 0, 1, 4));
        menu.setIconTextGap(0);
        for (Component component : menu.getMenuComponents()) {
            if (component instanceof JMenuItem) {
                JMenuItem item = (JMenuItem)component;
                item.setUI(new CompactMenuItemUI());
                item.setMargin(new Insets(1, 0, 1, 4));
                item.setIconTextGap(0);
            }
        }
    }

    public static void apply(JPopupMenu popup) {
        for (Component component : popup.getComponents()) {
            if (component instanceof JMenu) {
                apply((JMenu)component);
            } else if (component instanceof JMenuItem) {
                apply((JMenuItem)component);
            }
        }
    }

    public static void apply(JMenuItem menuItem) {
        menuItem.setUI(new CompactMenuItemUI());
        menuItem.setMargin(new Insets(1, 8, 1, 8));
        menuItem.setIconTextGap(3);
    }

    private static final class CompactMenuItemUI extends BasicMenuItemUI {
        @Override
        protected void installDefaults() {
            super.installDefaults();
            checkIcon = null;
        }
    }

    private static final class CompactMenuUI extends BasicMenuUI {
        @Override
        protected void installDefaults() {
            super.installDefaults();
            checkIcon = null;
        }
    }
}
