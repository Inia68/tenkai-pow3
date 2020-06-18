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
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.PacketWriter;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.OutgoingPackets;

public class InterfaceExpBonus implements IClientOutgoingPacket
{
	private final int _objectId;
	private final Location _loc;

	public InterfaceExpBonus(WorldObject obj)
	{
		_objectId = obj.getObjectId();
		_loc = obj.getLocation();
	}
	
	@Override
	public boolean write(PacketWriter packet)
	{
		OutgoingPackets.EX_CRAFT_RANDOM_MAKE.writeId(packet);

		packet.writeC(0x02);
		packet.writeC(0x02);
		PlayerInstance player = World.getInstance().getPlayer(_objectId);
		if (player == null) {
			return false;
		}
		int vitalityBonus = (int) player.getStat().getVitalityExpBonus() * 100;
		packet.writeH(vitalityBonus);
		return true;
	}
}
