package org.l2jmobius.gameserver.gui.playertable;



import org.l2jmobius.gameserver.gui.ServerGui;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;

import javax.swing.table.AbstractTableModel;

class PlayerTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;

    private static final String[] columnNames = {"Id", "Name", "Level"};

    private PlayerInstance[] players = new PlayerInstance[]{};

    public PlayerTableModel() {
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public int getRowCount() {
        return players.length;
    }

    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }

    @Override
    public Object getValueAt(int row, int col) {
        switch (col) {
            case 0:
                return players[row].getObjectId();
            case 1:
                return players[row].getName();
            case 2:
                return players[row].getLevel();
        }
        return "";
    }

    public synchronized boolean updateData() {
        PlayerInstance[] players = new PlayerInstance[World.getInstance().getPlayers().size()];
        World.getInstance().getPlayers().toArray(players);
        int playerCount = 0;
        int shopCount = 0;
        for (PlayerInstance player : players) {
            if (player != null && player.isOnline()) {
                if (player.getClient() == null || player.getClient().isDetached()) {
                    shopCount++;
                } else {
                    playerCount++;
                }
            }
        }

        ServerGui.getMainFrame().setTitle(
                "L2 Server [ L2 TENKAI ] | Players online: " + playerCount + " | Offline shops: " + shopCount + " | Total: " +
                        (playerCount + shopCount));
        if (players.length == players.length && !(players.length > 0 && players[0] == players[0])) {
            return false;
        }

        this.players = players;
        return true;
    }
}