package org.l2jmobius.gameserver.events.instanced.types;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.events.instanced.EventConfig;
import org.l2jmobius.gameserver.events.instanced.EventInstance;
import org.l2jmobius.gameserver.events.instanced.EventTeam;
import org.l2jmobius.gameserver.events.instanced.EventTeleporter;
import org.l2jmobius.gameserver.instancemanager.PlayerAssistsManager;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.instance.PetInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.clientpackets.Say2;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.util.Broadcast;

import java.util.List;

public class TeamVsTeam extends EventInstance {
    public TeamVsTeam(int id, EventConfig config) {
        super(id, config);
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
                if (true) {
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

        Broadcast.toAllOnlinePlayers("The event has ended. Team " + team.getName() + " won with " + team.getPoints() + " kill points.");
    }

    @Override
    public String getRunningInfo(PlayerInstance player) {
        String html = "";
        for (EventTeam team : teams) {
            if (team.getParticipatedPlayerCount() > 0) {
                html += "Team " + team.getName() + " kills: " + team.getPoints() + "<br>";
            }
        }
        if (html.length() > 4) {
            html = html.substring(0, html.length() - 4);
        }
        return html;
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

        if (killerCharacter == null) {
            return;
        }

        PlayerInstance killerPlayerInstance = null;

        if (killerCharacter instanceof PetInstance || killerCharacter instanceof Summon) {
            killerPlayerInstance = ((Summon) killerCharacter).getOwner();

            if (killerPlayerInstance == null) {
                return;
            }
        } else if (killerCharacter instanceof PlayerInstance) {
            killerPlayerInstance = (PlayerInstance) killerCharacter;
        } else {
            return;
        }

        byte killerTeamId = getParticipantTeamId(killerPlayerInstance.getObjectId());

        boolean friendlyDeath = killerTeamId == killedTeamId;
        if (killerTeamId != -1 && !friendlyDeath) {
            EventTeam killerTeam = teams[killerTeamId];

            killerTeam.increasePoints();
            onContribution(killerPlayerInstance, 1);

            CreatureSay cs = new CreatureSay(killerPlayerInstance,
                    ChatType.NPC_WHISPER,
                    killerPlayerInstance.getName(),
                    "I have killed " + killedPlayerInstance.getName() + "!");
            for (PlayerInstance playerInstance : teams[killerTeamId].getParticipatedPlayers().values()) {
                if (playerInstance != null) {
                    playerInstance.sendPacket(cs);
                }
            }

            killerPlayerInstance.addEventPoints(3);
            List<PlayerInstance> assistants = PlayerAssistsManager.getInstance().getAssistants(killerPlayerInstance, killedPlayerInstance, true);
            for (PlayerInstance assistant : assistants) {
                assistant.addEventPoints(1);
            }
        }
    }
}