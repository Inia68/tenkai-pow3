package org.l2jmobius.gameserver.gui.playertable;


import org.l2jmobius.commons.concurrent.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.concurrent.ThreadPoolExecutor;

public class PlayerTablePane extends JPanel {
    private static Logger log = LoggerFactory.getLogger(PlayerTablePane.class.getName());

    private static final long serialVersionUID = 1L;

    public class ButtonListeners implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent evt) {
            //String cmd = evt.getActionCommand();
        }
    }

    private GridBagLayout layout = new GridBagLayout();

    //Npc Table
    private PlayerTableModel playerTableModel;
    private JTable playerTable;

    private int currentSelectedPlayer = -1;

    public PlayerTablePane() {
        setLayout(layout);

        GridBagConstraints cons = new GridBagConstraints();
        cons.insets = new Insets(5, 5, 5, 5);

        JPanel smallPane = new JPanel();
        smallPane.setLayout(layout);

		/*ButtonListeners buttonListeners = new ButtonListeners();
		JButton analyze = new JButton("Check Targeting");
		analyze.addActionListener(buttonListeners);
		analyze.setActionCommand("CheckTargeting");
		smallPane.add(analyze, cons);
		cons.weightx = 0.5;
		cons.weighty = 0.1;
		cons.gridx = 0;
		cons.gridy = 0;
		cons.gridheight = 1;
		cons.anchor = GridBagConstraints.WEST;
		cons.fill = GridBagConstraints.HORIZONTAL;
		add(smallPane, cons);*/

        playerTableModel = new PlayerTableModel();
        playerTable = new JTable(playerTableModel);
        playerTable.addMouseListener(new PlayerTableMouseListener(this));
        playerTable.setDefaultRenderer(Object.class, new PlayerTableRenderer(playerTableModel));
        playerTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        playerTable.getSelectionModel().addListSelectionListener(new PlayerSelectionListener());
        playerTable.getColumnModel().getColumn(0).setMaxWidth(100);
        JScrollPane scrollPane = new JScrollPane(playerTable);
        scrollPane.setMinimumSize(new Dimension(250, 500));
        cons.weightx = 0.5;
        cons.weighty = 0.95;
        cons.gridx = 0;
        cons.gridy = 0;
        cons.gridheight = 1;
        cons.fill = GridBagConstraints.BOTH;
        add(scrollPane, cons);
        ThreadPool.scheduleAtFixedRate(this::updateTable, 10000, 1000);
    }

    public void setSelectedPlayer(int startIndex, int endIndex) {
        getPlayerTable().setAutoscrolls(true);
        getPlayerTable().getSelectionModel().setSelectionInterval(startIndex, endIndex);
        getPlayerTable().scrollRectToVisible(getPlayerTable().getCellRect(startIndex, 0, true));
    }

    public void updateTable() {
        SwingUtilities.invokeLater(() -> {
            if (playerTableModel.updateData()) {
                getPlayerTable().updateUI();
            }
        });
    }

    public JTable getPlayerTable() {
        return playerTable;
    }

    public PlayerTableModel getPlayerTableModel() {
        return playerTableModel;
    }

    public void updateCurrentPlayer() {
        updateCurrentPlayer(false);
    }

    public void updateCurrentPlayer(boolean forced) {
        if (!forced && currentSelectedPlayer == playerTable.getSelectedRow()) {
        } else {
            currentSelectedPlayer = playerTable.getSelectedRow();
        }

        //Player player = World.getInstance().getPlayer((Integer)playerTableModel.getValueAt(playerTable.getSelectedRow(), 0));
    }

    public void setTableSelectByMouseEvent(MouseEvent e) {
        int rowNumber = playerTable.rowAtPoint(e.getPoint());
        playerTable.getSelectionModel().setSelectionInterval(rowNumber, rowNumber);
    }

    public class PlayerSelectionListener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            PlayerTablePane view = PlayerTablePane.this;
            // If cell selection is enabled, both row and column change events are fired
            if (e.getSource() == view.getPlayerTable().getSelectionModel()) {
                view.updateCurrentPlayer();
            }
        }
    }

    @Override
    public void finalize() throws Throwable {
        super.finalize();
        log.info("Finalized: " + getClass().getSimpleName());
    }
}