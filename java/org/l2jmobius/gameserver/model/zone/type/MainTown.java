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

import org.l2jmobius.commons.concurrent.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.enums.Race;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.instancemanager.UpgradableFarmZoneManager;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.zone.ZoneType;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * A custom farm zone
 *
 * @author Inia
 */
public class MainTown extends ZoneType {
    private List<NpcFollower> _followingNpcs;
    private int[] ids = {40020, 40021, 40022, 40023, 40024, 40025};

    public class NpcFollower {
        private PlayerInstance _target;
        private Npc _npc;
        ScheduledFuture<?> _task;

        NpcFollower(PlayerInstance target) {
            _target = target;
            _npc = spawn(getRandomEntry(ids), -114440, 252888, -1547, 6370, false);
            if (_npc != null) {
                startFollow();
            }
        }

        public NpcFollower(Npc npc, PlayerInstance target) {
            _target = target;
            _npc = npc;
            if (_npc != null) {
                startFollow();
            }
        }

        void startFollow() {
            _npc.setRunning();
            _npc.setTarget(_target);
            _npc.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, _target);
            _task = ThreadPool.scheduleAtFixedRate(this::checkTarget, 1, 5000);
        }

        void checkTarget() {
            if (_target == null || _npc.calculateDistance3D(_target.getLocation()) > 1000) {
                _npc.deleteMe();
                _followingNpcs.remove(this);
                _task.cancel(true);
            }
        }


        public Npc getNpc() {return _npc;}
        public PlayerInstance getTarget() {return _target;}

    }

    public MainTown(int id) {
        super(id);
        _followingNpcs = new LinkedList<>();

    }

    public Npc spawn(int id, int x, int y, int z, int heading, boolean randomOffset) {
        try {
            final Spawn spawn = new Spawn(id);
            if ((x == 0) && (y == 0)) {
                LOGGER.severe("addSpawn(): invalid spawn coordinates for NPC #" + id + "!");
                return null;
            }

            if (randomOffset) {
                int offset = Rnd.get(50, 100);
                if (Rnd.nextBoolean()) {
                    offset *= -1;
                }
                x += offset;
                offset = Rnd.get(50, 100);
                if (Rnd.nextBoolean()) {
                    offset *= -1;
                }
                y += offset;
            }

            spawn.setHeading(heading);
            spawn.setXYZ(x, y, z);
            spawn.stopRespawn();

            final Npc npc = spawn.doSpawn(false);
            return npc;
        } catch (Exception e) {
            LOGGER.warning("Could not spawn NPC #" + id + "; error: " + e.getMessage());
        }
        return null;
    }


    @Override
    protected void onEnter(Creature creature) {

        if (creature.isPlayer() && (creature.getActingPlayer().getRace() == Race.ERTHEIA || creature.getActingPlayer().getRace() == Race.DWARF)) {
            PlayerInstance player = creature.getActingPlayer();
            creature.setInsideZone(ZoneId.MAINTOWN, true);
            NpcFollower follower = new NpcFollower(player);
            if (follower != null) {
                _followingNpcs.add(follower);
            }
        }
    }

    public static int getRandom(int max) {
        return Rnd.get(max);
    }

    public static int getRandomEntry(int... array) {
        return array[getRandom(array.length)];
    }

    @Override
    protected void onExit(Creature creature) {
        if (creature.isPlayer()) {
            creature.setInsideZone(ZoneId.MAINTOWN, false);
            _followingNpcs.forEach(npc -> {
                if (npc.getTarget().getObjectId() == creature.getActingPlayer().getObjectId()) {
                    _followingNpcs.remove(npc);
                    npc.getNpc().deleteMe();
                }
            });
        }
    }

    @Override
    public void onDieInside(Creature character, Creature killer) {

    }


}
