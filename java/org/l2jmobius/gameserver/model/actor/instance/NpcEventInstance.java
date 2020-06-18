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
import org.l2jmobius.gameserver.data.xml.impl.SkillData;
import org.l2jmobius.gameserver.data.xml.impl.TeleporterData;
import org.l2jmobius.gameserver.enums.InstanceType;
import org.l2jmobius.gameserver.enums.TeleportType;
import org.l2jmobius.gameserver.instancemanager.CastleManager;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.holders.RecipeHolder;
import org.l2jmobius.gameserver.model.holders.TeleporterQuestRecommendationHolder;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.items.Item;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.model.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.model.quest.QuestState;
import org.l2jmobius.gameserver.model.skills.CommonSkill;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.teleporter.TeleportHolder;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.*;
import org.l2jmobius.gameserver.taskmanager.AttackStanceTaskManager;
import org.l2jmobius.gameserver.util.Util;

import java.util.*;
import java.util.logging.Logger;

/**
 * @author Inia
 */
public class NpcEventInstance extends Npc
{

	public NpcEventInstance(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.BufferInstance);
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
			final RecipeHolder rp = RecipeData.getInstance().getRecipe(3002);
			boolean haveRecipe = player.hasRecipeList(rp.getId());
			if (haveRecipe) {
				showChatWindow(player, 1);
			} else {
				player.registerCommonRecipeList(rp, true);
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_ADDED);
				sm.addItemName(rp.getItemId());
				player.sendPacket(sm);
				showChatWindow(player, 2);
			}
			player.sendPacket(new RecipeBookItemList(player, false));
			return;

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
		return "data/html/event/" + pom + ".htm";
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
