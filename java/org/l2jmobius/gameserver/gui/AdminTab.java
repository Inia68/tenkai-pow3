package org.l2jmobius.gameserver.gui;

import org.l2jmobius.gameserver.gui.playertable.PlayerTablePane;
import org.l2jmobius.gameserver.model.announce.Announcement;
import org.l2jmobius.gameserver.util.Broadcast;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AdminTab extends JPanel {
    private static final long serialVersionUID = 1L;
    private GridBagConstraints cons = new GridBagConstraints();
    private GridBagLayout layout = new GridBagLayout();
    private JPanel listPanel = new PlayerTablePane();
    private JPanel infoPanel = new JPanel();

    public AdminTab() {

        JTextArea talkadmin = new JTextArea();
        JButton bouton = new JButton("Send");
        bouton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Broadcast.toAllOnlinePlayers(talkadmin.getText());
            }
        });
        setLayout(layout);
        cons.fill = GridBagConstraints.HORIZONTAL;

        infoPanel.setLayout(layout);

        cons.insets = new Insets(5, 5, 5, 5);
        cons.gridwidth = 3;
        cons.gridheight = 20;
        cons.weightx = 1;
        cons.weighty = 1;
        cons.gridx = 0;
        cons.gridy = 2;

        infoPanel.add(bouton, cons);

        infoPanel.setPreferredSize(new Dimension(235, infoPanel.getHeight()));

        cons.fill = GridBagConstraints.BOTH;
        cons.weightx = 1;
        cons.weighty = 1;

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listPanel, infoPanel);
        splitPane.setResizeWeight(0.3);
        splitPane.setDividerLocation(535);
        add(splitPane, cons);
        listPanel.add(talkadmin, cons);
    }
}