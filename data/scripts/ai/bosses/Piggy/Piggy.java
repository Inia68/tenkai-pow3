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
package ai.bosses.Piggy;

import org.l2jmobius.commons.concurrent.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.data.xml.impl.SkillData;
import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.enums.MountType;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.Attackable;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.holders.SkillHolder;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExSendUIEvent;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;
import org.l2jmobius.gameserver.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Future;

import ai.AbstractNpcAI;

/**
 * Piggy' AI.
 *
 * @author Barroso_K
 */
public class Piggy extends AbstractNpcAI {
    // NPC
    private static final int PIGGY = 9009120;

    private static final String[] PIGGY_SPEACH_ON_HIT = {
            "Stop hitting me please",
            "What have i done to deserve that ?",
            "Why are you so mean to me ?",
            "Stop it hurts",
            "I have kids !",
            "Stop i'll give you some adena !",
            "I won't forgive you"
    };
    private long _lastSpoke = 0;
    private int _speakInterval = 10; // Seconds

    private int _angryTime = 5; // Seconds
    public static Future<?> _timer;
    public int _timeLeft = 0;

    private final PiggySkill[] PIGGY_SKILLS =
            {
                    new PiggySkill(15271, 20, 100, true, 1000), // Body Slam
                    new PiggySkill(15287, 25, 250, false), // Twister
                    new PiggySkill(15305, 30, 300, false, 400), // Hurricane
                    new PiggySkill(15342, 10, 500, false) // Shock Smash
            };

    private class PiggySkill {
        private int skillId;
        private int brutDamages;
        private int percentDamage;
        private boolean isAoe;
        private int range;

        PiggySkill(int skillId, int percentDamage, int brutDamages, boolean isAoe, int range) {
            this.skillId = skillId;
            this.percentDamage = percentDamage;
            this.brutDamages = brutDamages;
            this.isAoe = isAoe;
            this.range = range;
        }

        PiggySkill(int skillId, int percentDamage, int brutDamages, boolean isAoe) {
            this.skillId = skillId;
            this.percentDamage = percentDamage;
            this.brutDamages = brutDamages;
            this.isAoe = isAoe;
        }

        public int getSkillId() {
            return this.skillId;
        }

        public int getPercentDamage() {
            return this.percentDamage;
        }

        public boolean isAoe() {
            return this.isAoe;
        }

        public int getRange() {
            return this.range;
        }

        public Skill getSkill() {
            return SkillData.getInstance().getSkill(this.skillId, 1);
        }

        public int getBrutDamages() {
            return this.brutDamages;
        }
    }

    private Playable _actualVictim;
    private boolean _isAngry = false;
    private static Npc _piggy;
    public static Location PIGGY_LOC = new Location(-51640, -70248, -3418);
    public static boolean isSpawned = false;

    public static int[] servitors = {9009121, 9009122, 9009123};
    public static Vector<Npc> _servitors = new Vector<>();
    private static final SkillHolder SERVITOR_HEAL = new SkillHolder(1401, 10);
    private static final String[] SERVITORS_SPEACH = {
            "Here's some heal master !!",
            "Are you okay king ?",
            "I'm coming for you master !",
            "I'm here to help you !",
            "I'll take care of you don't worry master",
            "The doctor is here !",
            "Let me take care of this my king",
            "You will soon feel better believe me"
    };

    public static int SCARECROW = 9009124;
    public static Location SCARECROW_LOC = new Location(-51624, -68840, -3418);
    public static Npc _scarecrow;
    public static boolean _scarecrowSpawned = false;
    public int _totalhits = 0;
    public int _maxHits = 100;

    private static final int NPC_ID = 9009126;
    private Npc _teleporter;

    public Piggy() {
        registerMobs(PIGGY);
        addAttackId(SCARECROW);
        for (int mob : servitors) {
            addAttackId(mob);
            addKillId(mob);
        }
        if (isSpawned) {
            return;
        }

        addStartNpc(NPC_ID);
        addTalkId(NPC_ID);
        addFirstTalkId(NPC_ID);

        _teleporter = addSpawn(NPC_ID, 147414, 27928, -2271, 20352, false, 0);
        _teleporter.setTitle("Piggy Raidboss");


        if (_piggy != null) {
            _piggy.deleteMe();
        }
        _piggy = addSpawn(PIGGY, PIGGY_LOC, false, 0);
        isSpawned = true;
        startQuestTimer("regen_task", 10000, _piggy, null, true);
        startQuestTimer("think", 10000, _piggy, null, true);
        startQuestTimer("skill_task", 3000, _piggy, null, true);
        startQuestTimer("spawn_servitors", 100, _piggy, null, false);
        startQuestTimer("heal_the_king", 10000, _piggy, null, true);
    }

    @Override
    public String onAdvEvent(String event, Npc npc, PlayerInstance player) {
        if (npc != null) {
            if (event.equalsIgnoreCase("skill_task")) {
                callSkillAI(npc);
            } else if (event.equalsIgnoreCase("spawn_scarecrow")) {
                if (_scarecrow != null) {
                    return "";
                }
                _scarecrow = addSpawn(SCARECROW, SCARECROW_LOC.getX(), SCARECROW_LOC.getY(), SCARECROW_LOC.getZ(), -1, false, 0, true, 0);
                _scarecrowSpawned = true;
                startQuestTimer("unspawn_scarecrow", 10000, _piggy, null, false);
            } else if (event.equalsIgnoreCase("think")) {
                if (getRandom(20) == 0 && !_isAngry && !_scarecrowSpawned) {
                    startQuestTimer("spawn_scarecrow", 1, _piggy, null, false);
                }
            } else if (event.equalsIgnoreCase("unspawn_scarecrow")) {
                _scarecrow.deleteMe();
                _scarecrowSpawned = false;
                if (_totalhits > _maxHits) {
                    _totalhits = _maxHits;
                }
                _piggy.reduceCurrentHp(getDamages(_piggy.getMaxHp(), (int) ((float) _totalhits / _maxHits * 100), 0), null, null, false, true, false, false);
                _totalhits = 0;
            } else if (event.equalsIgnoreCase("spawn_servitors")) {
                if (_servitors.size() >= 15) {
                    return "";
                }
                int radius = 250;
                for (int a = 0; a < 2; a++) {
                    for (int i = 0; i < 7; i++) {
                        int x = (int) (radius * Math.cos(i * 0.618));
                        int y = (int) (radius * Math.sin(i * 0.618));
                        Npc pig = addSpawn(servitors[Rnd.get(servitors.length)], PIGGY_LOC.getX() + x, PIGGY_LOC.getY() + y, PIGGY_LOC.getZ() + 20, -1, false, 0, true, 0);
                        _servitors.add(pig);
                    }
                    radius += 300;
                }
                startQuestTimer("spawn_servitors", 300000, _piggy, null, true);
            } else if (event.equalsIgnoreCase("heal_the_king")) {
                for (Npc pig : _servitors) {
                    if (getRandom(20) == 0) {
                        if (Util.checkIfInRange((SERVITOR_HEAL.getSkill().getCastRange() < 600) ? 600 : SERVITOR_HEAL.getSkill().getCastRange(), pig, _piggy, true)) {
                            pig.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
                            pig.setTarget(_piggy);
                            pig.doCast(SERVITOR_HEAL.getSkill());
                            pig.broadcastPacket(new NpcSay(pig.getObjectId(),
                                    ChatType.NPC_GENERAL,
                                    pig.getTemplate().getId(), getRandomEntry(SERVITORS_SPEACH)));
                        } else {
                            pig.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, _piggy, null);
                        }
                    }
                }
            } else if (event.equalsIgnoreCase("get_angry")) {
                _isAngry = true;
                _timeLeft = _angryTime;
                _timer = ThreadPool.scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        World.getInstance().forEachVisibleObjectInRange(_piggy, Playable.class, 1200, c ->
                        {
                            // c.sendPacket(new ExSendUIEvent(c.getActingPlayer(), ExSendUIEvent.TYPE_NORNIL, _timeLeft--, 0, 0, 0, 0, 2518008));
                            c.sendPacket(new ExSendUIEvent(c.getActingPlayer(), ExSendUIEvent.TYPE_NORNIL, _timeLeft--, _angryTime, NpcStringId.YOU_MADE_ME_ANGRY));
                        });

                    }
                }, 0, 1000);

                startQuestTimer("stop_angry", _angryTime * 1000, _piggy, null, false);
            } else if (event.equalsIgnoreCase("stop_angry")) {
                if (!_isAngry) {
                    return "";
                }
                _timer.cancel(true);
                _isAngry = false;
                _piggy.broadcastPacket(new NpcSay(_piggy.getObjectId(),
                        ChatType.NPC_GENERAL,
                        _piggy.getTemplate().getId(), "Thanks i'm a bit more relaxed now"));

            }
        }
        return super.onAdvEvent(event, npc, player);
    }

    @Override
    public String onSpawn(Npc npc) {
        ((Attackable) npc).setCanReturnToSpawnPoint(false);
        npc.setRandomWalking(true);
        npc.disableCoreAI(true);
        return super.onSpawn(npc);
    }

    @Override
    public String onAttack(Npc npc, PlayerInstance attacker, int damage, boolean isSummon) {
        if (npc.getId() == PIGGY) {

            if (npc.isHpBlocked()) {
                return null;
            }

            if (_lastSpoke + (_speakInterval * 1000) < System.currentTimeMillis()) {
                _piggy.broadcastPacket(new NpcSay(_piggy.getObjectId(),
                        ChatType.NPC_GENERAL,
                        _piggy.getTemplate().getId(), getRandomEntry(PIGGY_SPEACH_ON_HIT)));
                _lastSpoke = System.currentTimeMillis();
            }

            if (_isAngry) {
                _piggy.broadcastPacket(new NpcSay(_piggy.getObjectId(),
                        ChatType.NPC_GENERAL,
                        _piggy.getTemplate().getId(), "I TOLD YOU I WAS ANGRY !"));
                _piggy.doCast(PIGGY_SKILLS[0].getSkill());
                ThreadPool.schedule(() -> {
                    World.getInstance().forEachVisibleObjectInRange(_piggy, Playable.class, 1200, c ->
                    {
                        final int hpRatio = (int) ((c.getCurrentHp() / c.getMaxHp()) * 100);
                        if (hpRatio > 10) {
                            c.reduceCurrentHp(getDamages(c.getMaxHp(), PIGGY_SKILLS[0].getPercentDamage() * 2, PIGGY_SKILLS[0].getBrutDamages()), _piggy, PIGGY_SKILLS[0].getSkill(), false, true, false, false);
                        }
                    });
                    _timer.cancel(true);
                    _isAngry = false;
                }, PIGGY_SKILLS[0].getSkill().getHitTime());
                return "";
            }

            // Debuff strider-mounted players.
            if ((attacker.getMountType() == MountType.STRIDER) && !attacker.isAffectedBySkill(4258)) {
                npc.setTarget(attacker);
                npc.doCast(SkillData.getInstance().getSkill(4258, 1));
            }
        } else if (npc.getId() == SCARECROW) {
            _totalhits++;
            World.getInstance().forEachVisibleObjectInRange(_piggy, Playable.class, 1200, c ->
            {
                c.sendPacket(new ExSendUIEvent(c.getActingPlayer(), ExSendUIEvent.TYPE_NORNIL, _totalhits, _maxHits, NpcStringId.HIT_ME_MORE_HIT_ME_MORE));
            });

        } else {

        }
        return super.onAttack(npc, attacker, damage, isSummon);
    }

    @Override
    public String onKill(Npc npc, PlayerInstance killer, boolean isSummon) {

        if (npc.getId() == PIGGY) {
            cancelQuestTimer("regen_task", npc, null);
            cancelQuestTimer("think", npc, null);
            cancelQuestTimer("skill_task", npc, null);
            cancelQuestTimer("spawn_servitors", npc, null);
            cancelQuestTimer("heal_king", npc, null);
            isSpawned = false;
            for (Npc s : _servitors) {
                s.deleteMe();
            }
        }
        return super.onKill(npc, killer, isSummon);
    }

    @Override
    public String onAggroRangeEnter(Npc npc, PlayerInstance player, boolean isSummon) {
        return null;
    }

    private void callSkillAI(Npc npc) {
        if (npc.isInvul() || npc.isCastingNow()) {
            return;
        }

        if (!_isAngry && getRandom(20) == 0 && !_scarecrowSpawned) {
            _piggy.broadcastPacket(new NpcSay(npc.getObjectId(),
                    ChatType.NPC_GENERAL,
                    _piggy.getTemplate().getId(), "If you touch me i'll get angry guys"));

            World.getInstance().forEachVisibleObjectInRange(_piggy, Playable.class, 1200, c ->
            {
                c.sendPacket(new ExShowScreenMessage("Piggy will be angry very soon, care !", 5000));
            });
            startQuestTimer("get_angry", 3000, _piggy, null, false);
        }

        // Pickup a target if no or dead victim. 10% luck he decides to reconsiders his target.
        if ((_actualVictim == null) || _actualVictim.isDead() || !(npc.isInSurroundingRegion(_actualVictim)) || (getRandom(10) == 0)) {
            _actualVictim = getRandomTarget(npc);
        }

        if (_actualVictim == null) {
            if (getRandom(10) == 0) {
                final int x = npc.getX();
                final int y = npc.getY();
                final int z = npc.getZ();

                final int posX = x + getRandom(-1400, 1400);
                final int posY = y + getRandom(-1400, 1400);

                if (GeoEngine.getInstance().canMoveToTarget(x, y, z, posX, posY, z, npc.getInstanceWorld())) {
                    npc.getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new Location(posX, posY, z, 0));
                }
            }
            return;
        }

        final PiggySkill skill = getRandomSkill(npc);

        // Cast the skill or follow the target.
        if (Util.checkIfInRange((skill.getSkill().getCastRange() < 600) ? 600 : skill.getSkill().getCastRange(), npc, _actualVictim, true)) {
            npc.getAI().setIntention(CtrlIntention.AI_INTENTION_IDLE);
            npc.setTarget(_actualVictim);
            System.out.println("Skill: " + skill.getSkill().getName());
            npc.doCast(skill.getSkill());
            ThreadPool.schedule(() -> {
                if (skill.isAoe()) {
                    World.getInstance().forEachVisibleObjectInRange(_piggy, Playable.class, skill.getRange(), c ->
                    {
                        final int hpRatio = (int) ((c.getCurrentHp() / c.getMaxHp()) * 100);
                        if (hpRatio > 10) {
                            c.reduceCurrentHp(getDamages(c.getMaxHp(), skill.getPercentDamage(), skill.getBrutDamages()), _piggy, skill.getSkill(), false, true, false, false);
                        }
                    });

                } else {
                    _actualVictim.reduceCurrentHp(getDamages(_actualVictim.getMaxHp(), skill.getPercentDamage(), skill.getBrutDamages()), _piggy, skill.getSkill(), false, true, false, false);
                }
            }, skill.getSkill().getHitTime());
        } else {
            npc.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, _actualVictim, null);
        }
    }

    private float getDamages(int totalHp, int percent, int brut) {
        return (totalHp - totalHp * (((100 - percent) / 100f))) + brut;
    }

    /**
     * Pick a random skill.<br>
     * Piggy will mostly use utility skills. If Piggy feels surrounded, he will use AoE skills.<br>
     * Lower than 50% HPs, he will begin to use Meteor skill.
     *
     * @param npc piggy
     * @return a skill holder
     */
    private PiggySkill getRandomSkill(Npc npc) {
        final int hpRatio = (int) ((npc.getCurrentHp() / npc.getMaxHp()) * 100);

        return getRandomEntry(PIGGY_SKILLS);
    }

    /**
     * Pickup a random Playable from the zone, deads targets aren't included.
     *
     * @param npc
     * @return a random Playable.
     */
    private Playable getRandomTarget(Npc npc) {
        final List<Playable> result = new ArrayList<>();

        World.getInstance().forEachVisibleObject(npc, Playable.class, obj ->
        {
            if ((obj == null) || obj.isPet()) {
                return;
            } else if (!obj.isDead() && obj.isPlayable()) {
                result.add(obj);
            }
        });

        return getRandomEntry(result);
    }

    @Override
    public String onFirstTalk(Npc npc, PlayerInstance player) {
        StringBuilder tb = new StringBuilder();
        tb.append("<html><center><font color=\"3D81A8\">Piggy Raidboss</font></center><br1>Hi " + player.getName() + "<br>");
        tb.append("<font color=\"3D81A8\">Available Actons:</font><br>");
        tb.append("<br>");
        tb.append("<center><button value=\"Teleport to Fantasy Island\" action=\"bypass -h gm_event piggy_raidboss teleport_to_fantasy\"\n" +
                "                            width=200\n" +
                "                            height=40 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">\n" +
                "              </center> ");
        tb.append("</body></html>");
        NpcHtmlMessage msg = new NpcHtmlMessage(NPC_ID);
        msg.setHtml(tb.toString());
        player.sendPacket(msg);
        return "";
    }


}