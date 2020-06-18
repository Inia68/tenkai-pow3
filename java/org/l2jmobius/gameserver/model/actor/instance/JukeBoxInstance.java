/*
 * This file is part of the L2J Mobius project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.model.actor.instance;

import org.l2jmobius.gameserver.data.xml.impl.RecipeData;
import org.l2jmobius.gameserver.enums.InstanceType;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.holders.RecipeHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.*;
import org.l2jmobius.gameserver.util.BuilderUtil;

/**
 * @author Inia
 */
public class JukeBoxInstance extends Npc {

    public JukeBoxInstance(NpcTemplate template) {
        super(template);
        setRandomAnimation(false);
        setInstanceType(InstanceType.JukeBoxInstance);
    }


    private void playSound(PlayerInstance activeChar, String sound) {
        if (activeChar.getInventory().getInventoryItemCount(5343, 0) > 0) {
			activeChar.destroyItemByItemId("jukebox", 5343, 1, null, true);
            final PlaySound snd = new PlaySound(1, sound, 0, 0, 0, 0, 0);
            activeChar.sendPacket(snd);
            activeChar.broadcastPacket(snd);
            BuilderUtil.sendSysMessage(activeChar, "Playing " + sound + ".");
            broadcastPacket(new SocialAction(this.getObjectId(), 1));
        } else {
            activeChar.sendMessage("You need a music ticket !");
        }

    }

    @Override
    public void onBypassFeedback(PlayerInstance player, String command) {
        if (player == null) {
            return;
        }

        if (command.startsWith("play")) {
            String[] cmds = command.split(" ");
            playSound(player, cmds[1]);
            return;

        }
        if (command.startsWith("Chat")) {
            if (command.substring(5) == null) {
                showChatWindow(player);
                return;
            }
            showChatWindow(player, Integer.valueOf(command.substring(5)));
        } else {
            super.onBypassFeedback(player, command);
        }
    }


    @Override
    public String getHtmlPath(int npcId, int value, PlayerInstance player) {
        String pom;
        if (value == 0) {
            pom = Integer.toString(npcId);
        } else {
            pom = npcId + "-" + value;
        }
        return "data/html/jukebox/" + pom + ".htm";
    }

    @Override
    public void showChatWindow(PlayerInstance player, int val) {
        NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
        html.setFile(player, getHtmlPath(getId(), val, player));
        html.replace("%objectId%", String.valueOf(getObjectId()));
        player.sendPacket(html);
    }

}
