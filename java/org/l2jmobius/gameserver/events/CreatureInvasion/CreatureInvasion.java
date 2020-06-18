package org.l2jmobius.gameserver.events.CreatureInvasion;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;
import org.l2jmobius.gameserver.util.Broadcast;

import java.util.*;

public class CreatureInvasion extends Quest {
    //Config
    private static final int timeToEndInvasion = 15; //Minutes
    private static final int[] invaderIds = {900900, 900901};
    private static final int rewardId = 10639;
    private static final int[][] rewards = {{5, 10}, {10, 20}};
    private Map<Integer, int[]> invadersReward = new HashMap<Integer, int[]>();
    //Vars
    private static boolean isUnderInvasion = false;
    private Map<Integer, invaderInfo> attackInfo = new HashMap<Integer, invaderInfo>();
    private ArrayList<Creature> invaders = new ArrayList<Creature>();

    private static final int NPC_ID = 9009125;
    private Npc _teleporter;

    public void StartInvasion() {
        if (isUnderInvasion) {
            startQuestTimer("end_invasion", 1, null, null, false);
        } else {
            startQuestTimer("start_invasion", 1, null, null, false);
        }
    }

    public CreatureInvasion(int id) {
        super(id);
        int i = 0;
        for (int mob : invaderIds) {
            invadersReward.put(mob, rewards[i]);
            addAttackId(mob);
            addKillId(mob);
            i++;
        }
    }


    private class invaderInfo {
        private Long attackedTime;
        private int playerId;
        private String externalIP;

        private invaderInfo(int playerId, String externalIP) {
            this.playerId = playerId;
            this.externalIP = externalIP;
            setAttackedTime();
        }

        private long getAttackedTime() {
            return attackedTime;
        }

        private void setAttackedTime() {
            attackedTime = System.currentTimeMillis();
        }

        private int getPlayerId() {
            return playerId;
        }

        private String getExternalIP() {
            return externalIP;
        }

        private void updateInfo(int playerId, String externalIP) {
            this.playerId = playerId;
            this.externalIP = externalIP;
            setAttackedTime();
        }
    }

    @Override
    public String onAttack(Npc npc, PlayerInstance player, int damage, boolean isPet, Skill skill) {
        if (!isUnderInvasion) {
            player.doDie(npc);
            return "";
        }

        synchronized (attackInfo) {
            invaderInfo info = attackInfo.get(npc.getObjectId()); //Get the attack info from this npc

            int sameIPs = 0;
            int underAttack = 0;

            for (Map.Entry<Integer, invaderInfo> entry : attackInfo.entrySet()) {
                if (entry == null) {
                    continue;
                }

                invaderInfo i = entry.getValue();
                if (i == null) {
                    continue;
                }

                if (System.currentTimeMillis() < i.getAttackedTime() + 5000) {
                    if (i.getPlayerId() == player.getObjectId()) {
                        underAttack++;
                    }
                    if (i.getExternalIP().equalsIgnoreCase(player.getIPAddress())) {
                        sameIPs++;
                    }
                    if (underAttack > 1 || sameIPs > 1) {
                        player.doDie(npc);
                        if (underAttack > 1) {
                            npc.broadcastPacket(new NpcSay(npc.getObjectId(),
                                    ChatType.NPC_GENERAL,
                                    npc.getTemplate().getId(),
                                    player.getName() + " you cant attack more than one mob at same time!"));
                        } else if (sameIPs > 1) {
                            npc.broadcastPacket(new NpcSay(npc.getObjectId(),
                                    ChatType.NPC_GENERAL,
                                    npc.getTemplate().getId(),
                                    player.getName() + " dualbox is not allowed here!"));
                        }
                        return "";
                    }
                }
            }

            if (info == null) //Don't exist any info from this npc
            {
                //Add the correct info
                info = new invaderInfo(player.getObjectId(), player.getIPAddress());
                //Insert to the map
                attackInfo.put(npc.getObjectId(), info);
            } else {
                //Already exists information for this NPC
                //Check if the attacker is the same as the stored
                if (info.getPlayerId() != player.getObjectId()) {
                    //The attacker is not same
                    //If the last attacked stored info +10 seconds is bigger than the current time, this mob is currently attacked by someone
                    if (info.getAttackedTime() + 5000 > System.currentTimeMillis()) {
                        player.doDie(npc);
                        npc.broadcastPacket(new NpcSay(npc.getObjectId(),
                                ChatType.NPC_GENERAL,
                                npc.getTemplate().getId(),
                                player.getName() + " don't attack mobs from other players!"));
                        return "";
                    } else {
                        //Add new information, none is currently attacking this NPC
                        info.updateInfo(player.getObjectId(), player.getIPAddress());
                    }
                } else {
                    //player id is the same, update the attack time
                    info.setAttackedTime();
                }
            }
        }
        return super.onAttack(npc, player, damage, isPet, skill);
    }

    @Override
    public String onKill(Npc npc, PlayerInstance player, boolean isPet) {
        synchronized (attackInfo) {
            invaderInfo info = attackInfo.get(npc.getObjectId()); //Get the attack info
            if (info != null) {
                attackInfo.remove(npc.getObjectId()); //Delete the stored info for this npc
            }
        }

        if (isUnderInvasion) {
            Npc inv =
                    addSpawn(invaderIds[Rnd.get(invaderIds.length)], npc.getX() + Rnd.get(100), npc.getY() + Rnd.get(100), npc.getZ(), 0, false, 0);
            invaders.add(inv);
        }
        int[] rewards = invadersReward.get(npc.getId());
        player.addItem("event", rewardId, randomBetween(rewards[0], rewards[1]), player, true);
        return super.onKill(npc, player, isPet);
    }

    public int randomBetween(int low, int high) {
        Random r = new Random();
        return r.nextInt(high - low) + low;
    }

    @SuppressWarnings("unused")
    @Override
    public String onAdvEvent(String event, Npc npc, PlayerInstance player) {
        if (event.equalsIgnoreCase("teleport_to_fantasy")) {
            player.teleToLocation(-59004, -56889, -2032);
        } else if (event.startsWith("start_invasion")) {
            if (isUnderInvasion) {
                return "";
            }
            isUnderInvasion = true;

            addStartNpc(NPC_ID);
            addTalkId(NPC_ID);
            addFirstTalkId(NPC_ID);

            _teleporter = addSpawn(NPC_ID, 147448, 27928, -2271, 20352, false, 0);
            _teleporter.setTitle("Creature Invasion");

            int radius = 1000;
            for (int a = 0; a < 2; a++) {
                for (int i = 0; i < 50; i++) {
                    int x = (int) (radius * Math.cos(i * 0.618));
                    int y = (int) (radius * Math.sin(i * 0.618));

                    Npc inv = addSpawn(invaderIds[Rnd.get(invaderIds.length)], -59718 + x, -56909 + y, -2029 + 20, -1, false, 0, false, 0);
                    invaders.add(inv);
                }
                radius += 300;
            }

            Broadcast.toAllOnlinePlayers("Fantasy Island is under invasion!");
            Broadcast.toAllOnlinePlayers("Don't attack mobs from other players!");
            Broadcast.toAllOnlinePlayers("Dualbox is not allowed on the event!");
            Broadcast.toAllOnlinePlayers("The invasion will lasts for: " + timeToEndInvasion + " minute(s)!");

            startQuestTimer("end_invasion", timeToEndInvasion * 60000, null, null);
        } else if (event.startsWith("end_invasion")) {
            isUnderInvasion = false;

            if (_teleporter != null) {
                _teleporter.deleteMe();
            }

            for (Creature chara : invaders) {
                if (chara == null) {
                    continue;
                }
                chara.deleteMe();
            }

            invaders.clear();
            attackInfo.clear();

            Broadcast.toAllOnlinePlayers("The invasion has been ended!");
        }
        return "";
    }

    @Override
    public String onFirstTalk(Npc npc, PlayerInstance player) {
        StringBuilder tb = new StringBuilder();
        tb.append("<html><center><font color=\"3D81A8\">Creature Invasion</font></center><br1>Hi " + player.getName() + "<br>");
        tb.append("<font color=\"3D81A8\">Available Actons:</font><br>");
        tb.append("<br>");
        tb.append("   <center><button value=\"Teleport to Fantasy Island\" action=\"bypass -h gm_event creature_invasion teleport_to_fantasy\"\n" +
                "                            width=200\n" +
                "                            height=40 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></center>\n" +
                "               ");
        tb.append("</body></html>");
        NpcHtmlMessage msg = new NpcHtmlMessage(NPC_ID);
        msg.setHtml(tb.toString());
        player.sendPacket(msg);
        return "";
    }

    public static CreatureInvasion getInstance() {
        return CreatureInvasion.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        protected static final CreatureInvasion INSTANCE = new CreatureInvasion(999120);
    }

}