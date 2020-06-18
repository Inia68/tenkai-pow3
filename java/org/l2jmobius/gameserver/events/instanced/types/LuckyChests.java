package org.l2jmobius.gameserver.events.instanced.types;


import org.l2jmobius.Config;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.impl.NpcData;
import org.l2jmobius.gameserver.datatables.SpawnTable;
import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.enums.Position;
import org.l2jmobius.gameserver.events.instanced.EventConfig;
import org.l2jmobius.gameserver.events.instanced.EventInstance;
import org.l2jmobius.gameserver.events.instanced.EventTeam;
import org.l2jmobius.gameserver.events.instanced.EventTeleporter;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.network.clientpackets.Say2;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.util.Broadcast;
import org.l2jmobius.gameserver.util.BuilderUtil;
import org.slf4j.LoggerFactory;

public class LuckyChests extends EventInstance {

    private boolean chestsSpawned = false;
    private Spawn[] chestSpawns = new Spawn[200];

    public LuckyChests(int id, EventConfig config) {
        super(id, config);
    }

    @Override
    public boolean startFight() {
        if (!super.startFight()) {
            return false;
        }

        if (!chestsSpawned) {
            spawnChests();
        }

        return true;
    }

    @Override
    public void calculateRewards() {
        EventTeam team;
        if (config.getLocation().getTeamCount() != 4) {
            if (teams[0].getPoints() == teams[1].getPoints()) {
                // Check if one of the teams have no more players left
                if (teams[0].getParticipatedPlayerCount() == 0 || teams[1].getParticipatedPlayerCount() == 0) {
                    // set state to rewarding
                    setState(EventState.REWARDING);
                    // return here, the Fight can't be completed
                    Broadcast.toAllOnlinePlayers("The event has ended. No team won due to inactivity!");
                    return;
                }

                // Both teams have equals points
                if (Config.INSTANCED_EVENT_REWARD_TEAM_TIE) {
                    rewardTeams(-1);
                }
                Broadcast.toAllOnlinePlayers("The event has ended in a tie");
                return;
            }

            // Set state REWARDING so nobody can point anymore
            setState(EventState.REWARDING);

            // Get team which has more points
            team = teams[teams[0].getPoints() > teams[1].getPoints() ? 0 : 1];

            if (team == teams[0]) {
                rewardTeams(0);
            } else {
                rewardTeams(1);
            }
        } else {
            // Set state REWARDING so nobody can point anymore
            setState(EventState.REWARDING);
            if (teams[0].getPoints() > teams[1].getPoints() && teams[0].getPoints() > teams[2].getPoints() &&
                    teams[0].getPoints() > teams[3].getPoints()) {
                rewardTeams(0);
                team = teams[0];
            } else if (teams[1].getPoints() > teams[0].getPoints() && teams[1].getPoints() > teams[2].getPoints() &&
                    teams[1].getPoints() > teams[3].getPoints()) {
                rewardTeams(1);
                team = teams[1];
            } else if (teams[2].getPoints() > teams[0].getPoints() && teams[2].getPoints() > teams[1].getPoints() &&
                    teams[2].getPoints() > teams[3].getPoints()) {
                rewardTeams(2);
                team = teams[2];
            } else if (teams[3].getPoints() > teams[0].getPoints() && teams[3].getPoints() > teams[1].getPoints() &&
                    teams[3].getPoints() > teams[2].getPoints()) {
                rewardTeams(3);
                team = teams[3];
            } else {
                Broadcast.toAllOnlinePlayers("The event has ended in a tie");
                return;
            }
        }

        Broadcast.toAllOnlinePlayers("The event has ended. Team " + team.getName() + " won with " + team.getPoints() + " points.");
    }

    @Override
    public void stopFight() {
        super.stopFight();
        unspawnChests();
    }

    @Override
    public String getRunningInfo(PlayerInstance player) {
        String html = "";
        for (EventTeam team : teams) {
            if (team.getParticipatedPlayerCount() > 0) {
                html += "Team " + team.getName() + " points: " + team.getPoints() + "<br>";
            }
        }
        if (html.length() > 4) {
            html = html.substring(0, html.length() - 4);
        }
        return html;
    }

    public void chestPoints(PlayerInstance playerInstance, int points) {
        EventTeam team = getParticipantTeam(playerInstance.getObjectId());
        if (!isState(EventState.STARTED) || team == null) {
            return;
        }

        CreatureSay cs = null;
        if (points == 1) {
            playerInstance.addEventPoints(1);
            team.increasePoints();
            cs = new CreatureSay(playerInstance, ChatType.WHISPER, playerInstance.getName(), "I have opened a chest that contained 1 point.");
        } else if (points == 5) {
            playerInstance.addEventPoints(5);
            for (int i = 0; i < 5; i++) {
                team.increasePoints();
            }
            cs = new CreatureSay(playerInstance, ChatType.SHOUT, playerInstance.getName(), "I have opened a chest that contained 5 points!");
        } else if (points == 20) {
            playerInstance.addEventPoints(20);
            for (int i = 0; i < 20; i++) {
                team.increasePoints();
            }
            cs = new CreatureSay(playerInstance, ChatType.WHISPER,
                    playerInstance.getName(),
                    "I have opened a chest that contained 20 points!!!");
        }
        for (PlayerInstance character : team.getParticipatedPlayers().values()) {
            if (character != null) {
                character.sendPacket(cs);
            }
        }
    }

    @Override
    public void onKill(Creature killerCharacter, PlayerInstance killedPlayerInstance) {
        if (killedPlayerInstance == null || !isState(EventState.STARTED)) {
            return;
        }

        byte killedTeamId = getParticipantTeamId(killedPlayerInstance.getObjectId());

        if (killedTeamId == -1) {
            return;
        }

        new EventTeleporter(killedPlayerInstance, teams[killedTeamId].getCoords(), false, false);
    }

    private void spawnChests() {
        NpcTemplate tmpl = NpcData.getInstance().getTemplate(900911);
        try {
            int chestAmount;
            if (getId() == 100) {
                chestAmount = 200;
            } else if (config.getLocation().getTeamCount() == 4) {
                chestAmount = teams[0].getParticipatedPlayerCount() * 10;
            } else {
                chestAmount = teams[0].getParticipatedPlayerCount() * 5;
            }
            int i;
            for (i = 0; i < chestAmount && i < 200; i++) {
                chestSpawns[i] = new Spawn(tmpl);
                Location pos = config.getLocation().getZone().getZone().getRandomPoint();
                chestSpawns[i].setXYZ(pos.getX(), pos.getY(), pos.getZ());
                chestSpawns[i].setHeading(Rnd.get(65536));
                chestSpawns[i].setRespawnDelay(9999);
                chestSpawns[i].setAmount(1);
                chestSpawns[i].setInstanceId(getInstanceId());

                SpawnTable.getInstance().addNewSpawn(chestSpawns[i], false);

                chestSpawns[i].init();
                chestSpawns[i].stopRespawn();
                chestSpawns[i].getLastSpawn().broadcastInfo();
            }
            chestsSpawned = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unspawnChests() {
        int i;
        for (i = 0; i < 200; i++) {
            if (chestSpawns[i] != null && chestSpawns[i].getLastSpawn() != null) {
                chestSpawns[i].getLastSpawn().deleteMe();
                chestSpawns[i].stopRespawn();
                SpawnTable.getInstance().deleteSpawn(chestSpawns[i], false);
            }
        }
        chestsSpawned = false;
    }

    class UnspawnChestsTask implements Runnable {
        @Override
        @SuppressWarnings("synthetic-access")
        public void run() {
            unspawnChests();
        }
    }
}