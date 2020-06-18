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
package scripts.instances.Survival;

import instances.AbstractInstance;
import org.l2jmobius.Config;
import org.l2jmobius.commons.util.CommonUtil;
import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.instancemanager.InstanceManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.MonsterInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.holders.SkillHolder;
import org.l2jmobius.gameserver.model.instancezone.Instance;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.*;

import java.util.*;
import java.util.logging.Level;

/**
 * Survival
 *
 * @author Inia
 */
public class Survival extends AbstractInstance {
    private static final int TEMPLATE_ID = 295;
    private static final int INTERVAL_BETWEEN_WAVE = 20; // seconds

    private static final Map<Integer, Map<String, int[]>> _monsters = Map.of(
            1, Map.of(
                    "mobs", new int[]{50000, 50001},
                    "amount", new int[]{5, 5}),
            2, Map.of(
                    "mobs", new int[]{50000, 50001},
                    "amount", new int[]{10, 10}),
            3, Map.of(
                    "mobs", new int[]{50000, 50001},
                    "amount", new int[]{20, 20}),
            4, Map.of(
                    "mobs", new int[]{50000, 50001},
                    "amount", new int[]{35, 35}),
            5, Map.of(
                    "mobs", new int[]{50000, 50001},
                    "amount", new int[]{50, 50})
    );

    private static final int _bossId = 50010;

    public Survival() {
        super(TEMPLATE_ID);
        _monsters.forEach((wave, options) -> {
            options.forEach((s, ids) -> {
                if (s.equalsIgnoreCase("mobs")) {
                    addKillId(ids);
                }
            });
        });
        addKillId(_bossId);
    }

    @Override
    public String onAdvEvent(String event, Npc npc, PlayerInstance player) {
        Instance world = player.getInstanceWorld();
        if (world == null ) { return ""; }
        int wave = world.getParameters().getInt("wave", 0);

        switch (event) {
            case "spawn_wave": {
                final List<Npc> spawnedMonsters = new LinkedList<>();

                if (wave <= _monsters.size()) {
                    for (int i = 0; i < getRandom(_monsters.get(wave).get("amount")[0], _monsters.get(wave).get("amount")[1]); i++) {
                        int x = (int) (600 * Math.cos(i * 0.618));
                        int y = (int) (600 * Math.sin(i * 0.618));

                        Npc minion =
                                addSpawn(getRandomEntry(_monsters.get(wave).get("mobs")), 153569 + x, 142075 + y, -12742 + 20, -1, false, 0, false, player.getInstanceWorld().getId());
                        spawnedMonsters.add(minion);
                    }
                    spawnedMonsters.forEach(n -> {
                        n.setRunning();
                        ((Attackable) n).addDamageHate(player, 0, 999999);
                        n.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, player);
                    });
                } else {
                    Npc minion =
                            addSpawn(_bossId, 153572, 142074, -12741, 52867, false, 0, false, player.getInstanceWorld().getId());
                    minion.setImmobilized(true);
                    disablePlayers(world);
                    broadcastPacket(world, new SpecialCamera(minion, 40, 200, 10, 0, 1000, 0, 0, 1, 0, 0));
                    broadcastPacket(world, new SpecialCamera(minion, 40, 200, 10, 0, 4000, 1000, 0, 1, 0, 0));
                    spawnedMonsters.add(minion);
                    startQuestTimer("start_boss", 5000, null, player);
                }

                world.setParameter("monsters", spawnedMonsters);
                break;
            }
            case "start_boss": {
                List<Npc> monsters = world.getParameters().getList("monsters", Npc.class);
                monsters.forEach(n -> {
                    n.setImmobilized(false);
                    n.setRunning();
                    ((Attackable) n).addDamageHate(player, 0, 999999);
                    n.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, player);

                });
                enablePlayers(world);
                break;
            }
            case "next_wave": {
                wave += 1;
                if (wave > _monsters.size()) {
                    broadcastPacket(world, new ExShowScreenMessage("The boss will appear in " + INTERVAL_BETWEEN_WAVE + " seconds", 3000));
                } else {
                    broadcastPacket(world, new ExShowScreenMessage("Wave " + wave + " starting in " + INTERVAL_BETWEEN_WAVE + " seconds", 3000));
                }
                player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
                startQuestTimer("spawn_wave", INTERVAL_BETWEEN_WAVE * 1000, null, player);
                world.setParameter("wave", wave);
                break;
            }
        }
        return null;
    }

    @Override
    public String onTalk(Npc npc, PlayerInstance player) {
        if (npc != null) {

            if (player.isInParty() && player.getParty().getMemberCount() > 2) {
                player.sendMessage("Max 2 members parties");
                return null;
            }

            if (player.isInParty() && player.getParty().getLeader() != player) {
                player.sendMessage("Only the party leader can start the instance");
                return null;
            }

            try {
                enterInstance(player, npc, TEMPLATE_ID);
                startQuestTimer("next_wave", 1000, npc, player);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "", e);
            }
        }
        return null;
    }

    @Override
    public String onSpawn(Npc npc) {
        return super.onSpawn(npc);
    }

    @Override
    public String onAttack(Npc npc, PlayerInstance attacker, int damage, boolean isSummon, Skill skill) {
        return null;
    }

    @Override
    public String onSpellFinished(Npc npc, PlayerInstance player, Skill skill) {
        return super.onSpellFinished(npc, player, skill);
    }

    @Override
    public String onKill(Npc npc, PlayerInstance killer, boolean isSummon) {
        Instance world = killer.getInstanceWorld();
        List<Npc> monsters = world.getParameters().getList("monsters", Npc.class);

        if (npc.getId() == _bossId) {
            world.getParameters().remove("monsters");
            world.getParameters().remove("wave");
            world.finishInstance();
        } else {
            int wave  = world.getParameters().getInt("wave", 0);
            if (monsters != null && CommonUtil.contains(_monsters.get(wave).get("mobs"), npc.getId()) && CommonUtil.contains(monsters.toArray(), npc)) {
                monsters.remove(npc);
                if (monsters.isEmpty()) {
                    startQuestTimer("next_wave", 5000, null, killer);
                    world.getPlayers().forEach(playerInstance -> {
                        playerInstance.addItem("wave reward", 80306, wave, null, true);
                    });
                }
            }
        }
        return super.onKill(npc, killer, isSummon);
    }

    private void disablePlayers(Instance world) {
        for (PlayerInstance player : world.getPlayers()) {
            if ((player != null) && player.isOnline()) {
                player.abortAttack();
                player.abortCast();
                player.disableAllSkills();
                player.setTarget(null);
                player.stopMove(null);
                player.setImmobilized(true);
                player.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
            }
        }
    }

    private void enablePlayers(Instance world) {
        for (PlayerInstance player : world.getPlayers()) {
            if ((player != null) && player.isOnline()) {
                player.enableAllSkills();
                player.setImmobilized(false);
            }
        }
    }

    void broadcastPacket(Instance world, IClientOutgoingPacket packet) {
        for (PlayerInstance player : world.getPlayers()) {
            if ((player != null) && player.isOnline()) {
                player.sendPacket(packet);
            }
        }
    }

    private void sendPacketX(Instance world, IClientOutgoingPacket packet1, IClientOutgoingPacket packet2, int x) {
        for (PlayerInstance player : world.getPlayers()) {
            if ((player != null) && player.isOnline()) {
                if (player.getX() < x) {
                    player.sendPacket(packet1);
                } else {
                    player.sendPacket(packet2);
                }
            }
        }
    }

    public static void main(String[] args) {
        new Survival();
    }
}
