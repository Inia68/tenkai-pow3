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
package scripts.ai.areas.TalkingIsland;

import ai.AbstractNpcAI;
import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.util.Util;

/**
 * Boy and Girl AI.
 * @author St3eT
 */
public class MovingPet extends AbstractNpcAI
{
    // NPCs
    private static final int[] PETS = {40011, 40012, 40013, 40014};

    private MovingPet()
    {
        addSpawnId(PETS);
        addMoveFinishedId(PETS);
    }

    @Override
    public String onAdvEvent(String event, Npc npc, PlayerInstance player)
    {
        if (event.equals("NPC_SHOUT"))
        {
            npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.WOW_2);
            startQuestTimer("NPC_SHOUT", 10000 + (getRandom(5) * 1000), npc, null);
        }
        return super.onAdvEvent(event, npc, player);
    }

    @Override
    public String onSpawn(Npc npc)
    {
        if (npc.getId() == 40012 || npc.getId() == 4013 || npc.getId() == 4014) {
            npc.setWalking();
        } else {
            npc.setRunning();
        }
        final Location randomLoc = Util.getRandomPosition(npc.getSpawn().getLocation(), 200, 600);
        addMoveToDesire(npc, GeoEngine.getInstance().canMoveToTargetLoc(npc.getLocation().getX(), npc.getLocation().getY(), npc.getLocation().getZ(), randomLoc.getX(), randomLoc.getY(), randomLoc.getZ(), npc.getInstanceWorld()), 23);
        return super.onSpawn(npc);
    }

    @Override
    public void onMoveFinished(Npc npc)
    {
        final Location randomLoc = Util.getRandomPosition(npc.getSpawn().getLocation(), 200, 600);
        addMoveToDesire(npc, GeoEngine.getInstance().canMoveToTargetLoc(npc.getLocation().getX(), npc.getLocation().getY(), npc.getLocation().getZ(), randomLoc.getX(), randomLoc.getY(), randomLoc.getZ(), npc.getInstanceWorld()), 23);
        super.onMoveFinished(npc);
    }

    public static void main(String[] args)
    {
        new MovingPet();
    }
}