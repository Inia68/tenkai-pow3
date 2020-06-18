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
import org.l2jmobius.commons.concurrent.ThreadPool;
import org.l2jmobius.gameserver.GameTimeController;
import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.datatables.SpawnTable;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Fantasy Isle Parade
 *
 * @author JOJO, Pandragon
 */
public class Parade extends AbstractNpcAI {
    // @formatter:off
    final int[] ACTORS =
            {
                    32379, 0, 32379,
                    32379, 0, 32379,
                    32379, 0, 32379,
                    0, 0, 0,
                    32380, 0, 32380,
                    32380, 32381, 32380,
                    32380, 0, 32380,
                    32380, 32381, 32380,
                    0, 0, 0,
                    32382, 32382, 32382,
                    32382, 32383, 32382,
                    32383, 32384, 32383,
                    32383, 32384, 32383,
                    0, 0, 0,
                    0, 32385, 0,
                    32385, 0, 32385,
                    0, 32385, 0,
                    0, 0, 0,
                    32412, 0, 32411,
                    0, 0, 0,
                    32421, 0, 32409,
                    32423, 0, 32422,
                    0, 0, 0,
                    32420, 32419, 32417,
                    32418, 0, 32416,
                    0, 0, 0,
                    32414, 0, 32414,
                    0, 32413, 0,
                    32414, 0, 32414,
                    0, 0, 0,
                    32393, 0, 32394,
                    0, 32430, 0,
                    32392, 0, 32391,
                    0, 0, 0,
                    0, 32404, 0,
                    32403, 0, 32401,
                    0, 0, 0,
                    0, 32408, 0,
                    32406, 0, 32407,
                    0, 32405, 0,
                    0, 0, 0,
                    32390, 32389, 32387,
                    32388, 0, 32386,
                    0, 0, 0,
                    0, 32400, 0,
                    32397, 32398, 32396,
                    0, 0, 0,
                    0, 32450, 0,
                    32448, 32449, 32447,
                    0, 0, 0,
                    32380, 0, 32380,
                    32380, 32381, 32380,
                    32380, 0, 32380,
                    32380, 32381, 32380,
                    0, 0, 0,
                    32379, 0, 32379,
                    32379, 0, 32379,
                    32379, 0, 32379,
                    0, 0, 0,
                    0, 32415, 0
            };

    //(Northbound 270 degrees) Route 1
    private final int[] SPAWN = {-113464, 254744, -1511, 31406};
    private final int[][] ROUTE = {{-114040, 254664, -1538, 33056},
            {-114088, 253528, -1527, 61941},
            {-114344, 253368, -1544, 37644},
            {-114376, 250792, -1781, 48699},
            {-115304, 250680, -1765, 32865},
            {-116744, 249992, -1880, 37914},
            {-117784, 249304, -1926, 39960}};


    int npcIndex;
    Collection<Npc> spawns = ConcurrentHashMap.newKeySet();
    ScheduledFuture<?> spawnTask;
    ScheduledFuture<?> thinkTask;
    ScheduledFuture<?> deleteTask;
    ScheduledFuture<?> cleanTask;

    public Parade() {

        // Starts 60 seconds after server startup and repeats every 60 minutes.
         final long diff = timeLeftMilli(8, 0, 0), cycle = 600000;
        ThreadPool.scheduleAtFixedRate(new Start(), 60000, 600000 * 6);

        LOGGER.info("Talking Island: Parade starting at " + new SimpleDateFormat("yyyy/MM/dd HH:mm").format(System.currentTimeMillis() + diff) + " and is scheduled each next " + (cycle / 3600000) + " hours.");
    }

    void load() {
        npcIndex = 0;
    }

    void clean() {
        for (Npc spawn : spawns) {
            if (spawn != null) {
                spawn.deleteMe();
            }
        }
        spawns.clear();
    }

    private long timeLeftMilli(int hh, int mm, int ss) {
        final int now = (GameTimeController.getInstance().getGameTicks() * 60) / 100;
        int dd = ((hh * 3600) + (mm * 60) + ss) - (now % 86400);
        if (dd < 0) {
            dd += 86400;
        }
        return (dd * 1000) / 6;
    }

    private class Start implements Runnable {
        @Override
        public void run() {
            load();
            spawnTask = ThreadPool.scheduleAtFixedRate(new Spawn(), 0, 3000);
            thinkTask = ThreadPool.scheduleAtFixedRate(new Think(), 0, 2000);
            //deleteTask = ThreadPool.scheduleAtFixedRate(new Delete(), 10000, 1000);
            cleanTask = ThreadPool.schedule(new Clean(), 420000);
        }
    }

    private class Spawn implements Runnable {
        @Override
        public void run() {

            if (npcIndex >= ACTORS.length) {
                spawnTask.cancel(false);
                return;
            }
            final int npcId = ACTORS[npcIndex++];
            if (npcId == 0) {
                return;
            }
            final int[] start = SPAWN;
            final Npc actor = addSpawn(npcId, start[0], start[1], start[2], start[3], false, 0);
            final int[] goal = ROUTE[0];
            actor.setRunning();
            actor.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(goal[0], goal[1], goal[2], goal[3]));
            spawns.add(actor);
        }
    }

    private class Think implements Runnable {
        @Override
        public void run() {
            if (spawns.isEmpty()) {
                return;
            }
            for (Npc actor : spawns) {
                if (actor != null) {
                    if (actor.calculateDistanceSq2D(actor.getXdestination(), actor.getYdestination(), 0) < 10) {
                        int[] goal;
                        int index = 0;
                        for (int[] r: ROUTE) {
                            if (r[0] == actor.getXdestination()) {
                                if (index + 1 < ROUTE.length) {
                                    goal = ROUTE[index + 1];
                                    actor.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(goal[0], goal[1], goal[2], goal[3]));
                                } else {
                                    actor.deleteMe();
                                    spawns.remove(actor);
                                }
                                break;
                            }
                            index++;
                        }

                    } else if (!actor.isMoving()) {
                        actor.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(actor.getXdestination(), actor.getYdestination(), actor.getZdestination(), actor.getHeading()));
                    }
                }
            }
            if ((spawns.isEmpty()) && (deleteTask != null)) {
                deleteTask.cancel(false);
            }
        }
    }


    private class Delete implements Runnable {
        @Override
        public void run() {
            if (spawns.isEmpty()) {
                return;
            }
            for (Npc actor : spawns) {
                if (actor != null) {
                    if (actor.calculateDistanceSq2D(actor.getXdestination(), actor.getYdestination(), 0) < (100 * 100)) {
                        actor.deleteMe();
                        spawns.remove(actor);
                    } else if (!actor.isMoving()) {
                        actor.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(actor.getXdestination(), actor.getYdestination(), actor.getZdestination(), actor.getHeading()));
                    }
                }
            }
            if ((spawns.isEmpty()) && (deleteTask != null)) {
                deleteTask.cancel(false);
            }
        }
    }

    private class Clean implements Runnable {
        @Override
        public void run() {
            if (spawnTask != null) {
                spawnTask.cancel(true);
            }
            if (deleteTask != null) {
                deleteTask.cancel(true);
            }
            if (cleanTask != null) {
                cleanTask.cancel(true);
            }
            clean();
        }
    }

    public static void main(String[] args) {
        new Parade();
    }
}