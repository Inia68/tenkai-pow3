package org.l2jmobius.gameserver.gui;

import org.l2jmobius.gameserver.gui.playertable.PlayerTablePane;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.util.Broadcast;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CommandTab extends JPanel {
    private static final long serialVersionUID = 1L;


    public CommandTab() {

        JLabel labelSendMessage = new JLabel("Send Message: ");
        JTextField textAuthor = new JTextField("Admin", 15);
        JTextField textMessage = new JTextField("", 15);
        JButton sendButton = new JButton("Send");

        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Broadcast.toAllOnlinePlayers(textAuthor.getText() + ": " + textMessage.getText());
                textMessage.setText("");
            }
        });

        GridBagLayout layout = new GridBagLayout();
        this.setLayout(layout);
        GridBagConstraints gbc = new GridBagConstraints();

        // Put constraints on different buttons
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        this.add(labelSendMessage, gbc);
        gbc.gridx = 1;
        this.add(textAuthor, gbc);
        gbc.gridx = 2;
        this.add(textMessage, gbc);
        gbc.gridx = 3;
        this.add(sendButton, gbc);


        JLabel labelGiveItems = new JLabel("give items");
        JTextField textItemId = new JTextField("item id", 15);
        JTextField textAmount = new JTextField("item amount", 15);
        JTextField textPlayer = new JTextField("player", 15);
        JButton sendItemButton = new JButton("Send");
        sendItemButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int itemId = Integer.parseInt(textItemId.getText());
                int itemAmount = Integer.parseInt(textAmount.getText());
                String playerName = textPlayer.getText();
                PlayerInstance player = World.getInstance().getPlayer(playerName);
                if (player != null) {
                    player.addItem("admin", itemId, itemAmount, player, true);
                }
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 2;
        this.add(labelGiveItems, gbc);
        gbc.gridx = 1;
        this.add(textItemId, gbc);
        gbc.gridx = 2;
        this.add(textAmount, gbc);
        gbc.gridx = 3;
        this.add(textPlayer, gbc);
        gbc.gridx = 4;
        this.add(sendItemButton, gbc);



    }
}