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
package org.l2jmobius.gameserver.model.zone.type;

import org.l2jmobius.Config;
import org.l2jmobius.commons.concurrent.ThreadPool;
import org.l2jmobius.gameserver.instancemanager.FarmZoneManager;
import org.l2jmobius.gameserver.instancemanager.UpgradableFarmZoneManager;
import org.l2jmobius.gameserver.model.PlayerCondOverride;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.fishing.Fishing;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.fishing.ExAutoFishAvailable;

import java.lang.ref.WeakReference;

/**
 * A custom farm zone
 * @author Inia
 */
public class FarmZone extends ZoneType
{
	public FarmZone(int id)
	{
		super(id);
	}
	
	@Override
	protected void onEnter(Creature creature)
	{
		if (creature.isPlayer())
		{
			PlayerInstance player = creature.getActingPlayer();
			creature.setInsideZone(ZoneId.FARM, true);
			UpgradableFarmZoneManager.getInstance().getPanel(player, "farm_zone;" + String.valueOf(getId()));
		}
	}
	
	@Override
	protected void onExit(Creature creature)
	{
		if (creature.isPlayer())
		{
			creature.setInsideZone(ZoneId.FARM, false);
		}
	}

	@Override
	public void onDieInside(Creature creature) {
		super.onDieInside(creature);
		if (creature.isMonster()) {
			UpgradableFarmZoneManager._farmZones.get(getId()).onMonsterDie(creature);

		}
	}

	@Override
	public void onDieInside(Creature character, Creature killer) {
		if (character.isPlayer() && killer.isPlayer()) {
			UpgradableFarmZoneManager._farmZones.get(getId()).onDie(character.getActingPlayer(), killer.getActingPlayer());

		}
	}
}
