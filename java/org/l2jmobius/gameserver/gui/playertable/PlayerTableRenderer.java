package org.l2jmobius.gameserver.gui.playertable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class PlayerTableRenderer extends DefaultTableCellRenderer implements TableCellRenderer {
    private static final long serialVersionUID = 1L;
    @SuppressWarnings("unused")
    private PlayerTableModel table;

    public PlayerTableRenderer(PlayerTableModel table) {
        this.table = table;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
        Component c;
        if (value instanceof Component) {
            c = (Component) value;
            if (isSelected) {
                c.setForeground(table.getSelectionForeground());
                c.setBackground(table.getSelectionBackground());
            }
        } else {
            c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
        }

        if (!isSelected) {
            c.setBackground(table.getBackground());
        }

        return c;
    }

    public interface TooltipTable {
        String getToolTip(int row, int col);

        boolean getIsMarked(int row);
    }
}