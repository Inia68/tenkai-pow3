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
package scripts.handlers.bypasshandlers;

import org.l2jmobius.Config;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.MerchantInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.instance.TeleporterInstance;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;

public class Teleport implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
			"teleto",
			"pvpzone"
	};
	
	@Override
	public boolean useBypass(String command, PlayerInstance player, Creature target)
	{
		if (!(target instanceof TeleporterInstance))
		{
			return false;
		}


		StringTokenizer st = new StringTokenizer(command, " ");
		st.nextToken();

		if (command.startsWith("teleto")) // Tenkai custom - raw teleport coordinates, only check for TW ward
		{
			if (player.isCombatFlagEquipped())
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_WHILE_IN_POSSESSION_OF_A_WARD);
				return false;
			}

			if (player.getPvpFlag() > 0)
			{
				player.sendMessage("You can't teleport while flagged!");
				return false;
			}

			int[] coords = new int[3];
			try
			{
				for (int i = 0; i < 3; i++)
				{
					coords[i] = Integer.valueOf(st.nextToken());
				}
				player.teleToLocation(coords[0], coords[1], coords[2]);
				player.setInstanceId(0);
			}
			catch (Exception e)
			{
				LOGGER.warning("L2Teleporter - " + target.getName() + "(" + target.getId() +
						") - failed to parse raw teleport coordinates from html");
				e.printStackTrace();
			}

			return true;
		}


		return false;
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}
