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
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.RecipeBookItemList;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * @author Inia
 */
public class NpcFarmzoneInstance extends Npc
{

	public NpcFarmzoneInstance(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.NpcInstance);
	}

	@Override
	public void onBypassFeedback(PlayerInstance player, String command)
	{
		if (player == null)
		{
			return;
		}

		if (command.startsWith("recipe"))
		{

		} if (command.startsWith("Chat"))
		{
			showChatWindow(player, Integer.valueOf(command.substring(5)));
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}




	@Override
	public String getHtmlPath(int npcId, int value, PlayerInstance player)
	{
		String pom;
		if (value == 0)
		{
			pom = Integer.toString(npcId);
		}
		else
		{
			pom = npcId + "-" + value;
		}
		return "data/html/farm/" + pom + ".htm";
	}

	@Override
	public void showChatWindow(PlayerInstance player, int val)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(player, getHtmlPath(getId(), val, player));
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
	}

}
