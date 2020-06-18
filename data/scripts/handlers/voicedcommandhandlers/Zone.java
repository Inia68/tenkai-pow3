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
package scripts.handlers.voicedcommandhandlers;

import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.instancemanager.UpgradableFarmZoneManager;
import org.l2jmobius.gameserver.instancemanager.ZoneManager;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.model.zone.type.FarmZone;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

import java.text.DecimalFormat;

public class Zone implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"zone"
	};
	
	@Override
	public boolean useVoicedCommand(String command, PlayerInstance player, String target)
	{
		if (command.startsWith("zone"))
		{
			if (player.isInsideZone(ZoneId.FARM)) {
				ZoneType zone = ZoneManager.getInstance().getZone(player.getLocation(), FarmZone.class);
				if (zone != null) {
					UpgradableFarmZoneManager.getInstance().getPanel(player, "farm_zone;" + String.valueOf(zone.getId()));
				}
			}
		}
		else
		{
			return false;
		}
		return true;
	}


	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}