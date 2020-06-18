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
package scripts.handlers.itemhandlers;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.model.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.sessionzones.TimedHuntingZoneList;

public class TimeStone implements IItemHandler {
	@Override
	public boolean useItem(Playable playable, ItemInstance item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.YOUR_PET_CANNOT_CARRY_THIS_ITEM);
			return false;
		}

		PlayerInstance player = playable.getActingPlayer();
		int _zoneId = item.getId() == 80996 ? 1 : 6;
		long addTime = player.getVariables().getLong("HUNTING_ZONE_ADD_TIME_" + _zoneId, 0);
		if (addTime >= 3600000) {
			player.sendMessage("You already added additionnal time.");
			return false;
		}
		final long currentTime = System.currentTimeMillis();
		long endTime = player.getVariables().getLong(PlayerVariables.HUNTING_ZONE_RESET_TIME + _zoneId, 0);
		if ((endTime + Config.TIME_LIMITED_ZONE_RESET_DELAY) < currentTime)
		{
			player.sendMessage("You have to enter a first time extend the time.");
			return false;
		}
		player.getVariables().set("HUNTING_ZONE_ADD_TIME_" + _zoneId, 3600000);
		playable.sendPacket(new TimedHuntingZoneList(player));


		return true;
	}
}
