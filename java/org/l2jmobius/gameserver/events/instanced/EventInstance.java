package org.l2jmobius.gameserver.events.instanced;

import org.l2jmobius.Config;
import org.l2jmobius.commons.concurrent.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.datatables.EventPrizesTable;
import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.enums.PartyDistributionType;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.instancemanager.InstanceManager;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.Party;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.clientpackets.Say2;
import org.l2jmobius.gameserver.network.serverpackets.*;
import org.l2jmobius.gameserver.taskmanager.AttackStanceTaskManager;
import org.l2jmobius.gameserver.util.Broadcast;
import org.l2jmobius.gameserver.util.Point3D;
import org.l2jmobius.loginserver.network.GameServerPacketHandler;

import java.util.List;
import java.util.Random;

public abstract class EventInstance {
    public enum EventType {
        TVT,
        LuckyChests,
        CaptureTheFlag,
        SimonSays,
        VIP,
        Survival,
        DeathMatch,
        KingOfTheHill,
        TeamSurvival,
        CursedBattle,
        DestroyTheGolem,
        FieldDomination,
        StalkedSalkers
    }

    public enum EventState {
        INACTIVE,
        READY,
        STARTED,
        REWARDING
    }

    protected final EventConfig config;
    protected final int id;
    protected final int instanceId;

    protected EventTeam[] teams = new EventTeam[4];
    protected EventState state = EventState.INACTIVE;

    private int participants;

    private long startTime = 0;

    public int randomBetween(int low, int high) {
        Random r = new Random();
        return r.nextInt(high-low) + low;
    }

    public EventInstance(int id, EventConfig config) {
        this.id = id;
        this.config = config;
        teams[0] = new EventTeam(0, config.getTeamName(0), config.getLocation().getSpawn(0));
        teams[1] = new EventTeam(1, config.getTeamName(1), config.getLocation().getSpawn(1));
        teams[2] = new EventTeam(2, config.getTeamName(2), config.getLocation().getSpawn(2));
        teams[3] = new EventTeam(3, config.getTeamName(3), config.getLocation().getSpawn(3));

        instanceId = id + 40000;
        InstanceManager.getInstance().createInstance();
        setState(EventState.READY);

        ThreadPool.schedule(() -> sendToAllParticipants("Match found! The event is starting in 20 seconds."), 5000L);

        ThreadPool.schedule(this::startFight, 20000L);
    }

    public boolean startFight() {
        for (EventTeam team : teams) {
            for (PlayerInstance player : team.getParticipatedPlayers().values()) {
                if (player != null &&
                        (OlympiadManager.getInstance().isRegisteredInComp(player) || player.isInOlympiadMode() || player.isOlympiadStart() ||
                                player.isFlyingMounted() || player.inObserverMode())) {
                    removeParticipant(player.getObjectId());
                }
            }
        }

        if (id != 100) {
            // Check for enough participants
            if (!config.isAllVsAll()) {
                if (config.getLocation().getTeamCount() != 4) {
                    if (teams[0].getParticipatedPlayerCount() < Config.INSTANCED_EVENT_MIN_PLAYERS_IN_TEAMS  ||
                            teams[1].getParticipatedPlayerCount() <  Config.INSTANCED_EVENT_MIN_PLAYERS_IN_TEAMS ) {
                        // Set state INACTIVE
                        setState(EventState.INACTIVE);
                        // Cleanup of teams
                        teams[0].onEventNotStarted();
                        teams[1].onEventNotStarted();
                        return false;
                    }
                } else {
                    if (teams[0].getParticipatedPlayerCount() < Config.INSTANCED_EVENT_MIN_PLAYERS_IN_TEAMS  ||
                            teams[1].getParticipatedPlayerCount() < Config.INSTANCED_EVENT_MIN_PLAYERS_IN_TEAMS ||
                            teams[2].getParticipatedPlayerCount() < Config.INSTANCED_EVENT_MIN_PLAYERS_IN_TEAMS ||
                            teams[3].getParticipatedPlayerCount() < Config.INSTANCED_EVENT_MIN_PLAYERS_IN_TEAMS ) {
                        // Set state INACTIVE
                        setState(EventState.INACTIVE);
                        // Cleanup of teams
                        teams[0].onEventNotStarted();
                        teams[1].onEventNotStarted();
                        teams[2].onEventNotStarted();
                        teams[3].onEventNotStarted();
                        return false;
                    }
                }
            } else {
                if (teams[0].getParticipatedPlayerCount() < 2) {
                    setState(EventState.INACTIVE);
                    teams[0].onEventNotStarted();
                    return false;
                }
                participants = teams[0].getParticipatedPlayerCount();
            }
        }

        // Iterate over all teams
        for (EventTeam team : teams) {
            int divider = 7;
            if (team.getParticipatedPlayerCount() > 2) {
                while (team.getParticipatedPlayerCount() % divider > 0 && team.getParticipatedPlayerCount() % divider <= 2 && divider > 5) {
                    divider--;
                }
            }

            int partyCount = team.getParticipatedPlayerCount() / divider;
            if (team.getParticipatedPlayerCount() % divider > 0) {
                partyCount++;
            }
            Party[] parties = new Party[partyCount];
            int currentParty = 0;
            // Iterate over all participated player instances in this team
            for (PlayerInstance playerInstance : team.getParticipatedPlayers().values()) {
                if (playerInstance != null) {
                    playerInstance.setEventPoints(0);

                    try {
                        playerInstance.eventSaveData();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (playerInstance.isMounted()) {
                        playerInstance.dismount();
                    }
                    // Teleporter implements Runnable and starts itself
                    new EventTeleporter(playerInstance, team.getCoords(), false, false);

                    playerInstance.leaveParty();
                    if (!config.isAllVsAll()) {
                        // Add the player into the current party or create it if it still doesn't exist
                        if (parties[currentParty] == null) {
                            parties[currentParty] = new Party(playerInstance, PartyDistributionType.FINDERS_KEEPERS);
                            playerInstance.setParty(parties[currentParty]);
                        } else {
                            playerInstance.joinParty(parties[currentParty]);
                        }

                        // Rotate current party index
                        currentParty++;
                        if (currentParty >= partyCount) {
                            currentParty = 0;
                        }
                    }
                }
            }
        }

        Broadcast.toAllOnlinePlayers("The " + config.getEventName() + " has started.");

        if (!config.isType(EventType.TeamSurvival) && !config.isType(EventType.Survival) && !config.isType(EventType.SimonSays)) {
            ThreadPool.schedule(this::stopFight, 60000 * Config.INSTANCED_EVENT_RUNNING_TIME );
        }

        // Set state STARTED
        setState(EventState.STARTED);
        startTime = System.currentTimeMillis();

        return true;
    }

    public long getStartTime() {
        return startTime;
    }

    public abstract void calculateRewards();

    protected void onContribution(PlayerInstance player, int weight) {
        if (player == null) {
        }

		/*
		int rewardId = 6392;
		int rewardQt = weight;
		player.addItem("Instanced Events", rewardId, rewardQt, player, true);
		StatusUpdate statusUpdate = new StatusUpdate(player);
		statusUpdate.addAttribute(StatusUpdate.CUR_LOAD, player.getCurrentLoad());
		player.sendPacket(statusUpdate);
		 */
    }

    protected void rewardTeams(int winnerTeam) {
        EventTeam winner = null;
        if (winnerTeam >= 0) {
            winner = teams[winnerTeam];
        }

        for (EventTeam team : teams) {
            int totalPoints = 0;
            for (PlayerInstance player : team.getParticipatedPlayers().values()) {
                if (player != null) {
                    totalPoints += player.getEventPoints();
                }
            }

            float teamMultiplier = 0.5f;
            if (team == winner) {
                if (config.getLocation().getTeamCount() == 4) {
                    teamMultiplier = 2.5f;
                } else {
                    teamMultiplier = 1.5f;
                }
            }

            for (PlayerInstance player : team.getParticipatedPlayers().values()) {
                if (player == null) {
                    continue;
                }

                if (team == winner) {
                    player.sendPacket(new CreatureSay(player, ChatType.PARTYROOM_ALL, "Instanced Events", "Your team has won!!!"));
                } else {
                    player.sendPacket(new CreatureSay(player, ChatType.PARTYROOM_ALL, "Instanced Events", "Your team has lost :("));
                }

                float performanceMultiplier = player.getEventPoints() * ((float) team.getParticipatedPlayerCount() / (float) totalPoints);
                String performanceString;
                if (player.getEventPoints() == 0) {
                    performanceMultiplier = 0;
                }
                if (performanceMultiplier < 0.35) {
                    performanceString = "horrible";
                } else if (performanceMultiplier < 0.8) {
                    performanceString = "bad";
                } else if (performanceMultiplier < 1.2) {
                    performanceString = "okay";
                } else if (performanceMultiplier < 1.8) {
                    performanceString = "good";
                } else if (performanceMultiplier < 3.0) {
                    performanceString = "very good";
                } else {
                    performanceString = "amazing";
                }
                player.sendPacket(new CreatureSay(player,
                        ChatType.PARTYROOM_ALL,
                        "Instanced Events",
                        "Your performance in this event has been " + performanceString + "."));

                EventPrizesTable.getInstance().rewardPlayer("InstancedEvents", player, teamMultiplier, performanceMultiplier);

                StatusUpdate statusUpdate = new StatusUpdate(player);
                player.sendPacket(statusUpdate);
            }
        }
    }

    protected void rewardPlayers(List<PlayerInstance> players) {
        if (players == null || players.isEmpty()) {
            return;
        }

        int totalPoints = 0;
        for (PlayerInstance player : players) {
            if (player != null) {
                totalPoints += player.getEventPoints();
            }
        }

        for (PlayerInstance player : players) {
            if (player == null) {
                continue;
            }

            // Avoid 0 division
            if (totalPoints <= 0) {
                totalPoints = 1;
            }

            float performanceMultiplier = player.getEventPoints() * ((float) participants / (float) totalPoints);
            EventPrizesTable.getInstance().rewardPlayer("InstancedEvents", player, 1.0f, performanceMultiplier);

            StatusUpdate statusUpdate = new StatusUpdate(player);

            NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);
            npcHtmlMessage.setHtml(
                    "<html><head><title>Instanced Events</title></head><body>The event has finished. Look at your inventory, there should be your reward.</body></html>");
            player.sendPacket(npcHtmlMessage);

            // TODO: Place this at the HTML
            player.sendPacket(new CreatureSay(player, ChatType.PARTYROOM_ALL, "Instanced Events", "Event score:"));
            for (PlayerInstance rewarded : players) {
                if (rewarded != null) {
                    player.sendPacket(new CreatureSay(player, ChatType.PARTYROOM_ALL,
                            "Instanced Events",
                            rewarded.getName() + ": " + rewarded.getEventPoints()));
                }
            }
        }
    }

    public void stopFight() {
        Broadcast.toAllOnlinePlayers("The " + config.getEventName() + " has ended.");
        calculateRewards();

        // Iterate over all teams
        for (EventTeam team : teams) {
            for (PlayerInstance playerInstance : team.getParticipatedPlayers().values()) {
                // Check for nullpointer
                if (playerInstance != null) {
                    if (playerInstance.getCtfFlag() != null) {
                        playerInstance.setCtfFlag(null);
                    }
                    playerInstance.setEventPoints(-1);
                    // TODO: EVENT: TELEPORT TO MAIN TOWN
                    new EventTeleporter(playerInstance, new Point3D(playerInstance.getEventSavedPosition().getX(), playerInstance.getEventSavedPosition().getY(), playerInstance.getEventSavedPosition().getZ()), true, true);
                }
                team.setVIP(null);
            }
        }

        // Cleanup of teams
        teams[0].cleanMe();
        teams[1].cleanMe();
        if (config.getLocation().getTeamCount() == 4) {
            teams[2].cleanMe();
            teams[3].cleanMe();
        }

        ThreadPool.schedule(() -> {
            // Set state INACTIVE
            setState(EventState.INACTIVE);
        }, 5000L);
    }

    public synchronized boolean addParticipant(PlayerInstance playerInstance) {
        // Check for nullpointer
        if (playerInstance == null) {
            return false;
        }

        playerInstance.setEvent(this);
        byte teamId = 0;
        if (config.isAllVsAll()) {
            return teams[teamId].addPlayer(playerInstance);
        }

        if (playerInstance.getClassId().getId() == 146) {
            if (config.getLocation().getTeamCount() == 2) {
                // Check to which team the player should be added
                if (teams[0].getHealersCount() == teams[1].getHealersCount()) {
                    teamId = (byte) Rnd.get(2);
                } else {
                    teamId = (byte) (teams[0].getParticipatedPlayerCount() > teams[1].getParticipatedPlayerCount() ? 1 : 0);
                }
            } else {
                int minHealers = 50;
                for (byte i = 0; i < 4; i++) {
                    if (teams[i].getHealersCount() < minHealers) {
                        teamId = i;
                        minHealers = teams[i].getHealersCount();
                    }
                }
            }
        } else if (config.getLocation().getTeamCount() == 2) {
            // Check to which team the player should be added
            if (teams[0].getParticipatedPlayerCount() == teams[1].getParticipatedPlayerCount()) {
                teamId = (byte) Rnd.get(2);
            } else {
                teamId = (byte) (teams[0].getParticipatedPlayerCount() > teams[1].getParticipatedPlayerCount() ? 1 : 0);
            }
        } else {
            // Check to which team the player should be added

            if (teams[0].getParticipatedPlayerCount() < teams[1].getParticipatedPlayerCount() &&
                    teams[0].getParticipatedPlayerCount() < teams[2].getParticipatedPlayerCount() &&
                    teams[0].getParticipatedPlayerCount() < teams[3].getParticipatedPlayerCount()) {
                teamId = (byte) 0;
            } else if (teams[1].getParticipatedPlayerCount() < teams[0].getParticipatedPlayerCount() &&
                    teams[1].getParticipatedPlayerCount() < teams[2].getParticipatedPlayerCount() &&
                    teams[1].getParticipatedPlayerCount() < teams[3].getParticipatedPlayerCount()) {
                teamId = (byte) 1;
            } else if (teams[2].getParticipatedPlayerCount() < teams[0].getParticipatedPlayerCount() &&
                    teams[2].getParticipatedPlayerCount() < teams[1].getParticipatedPlayerCount() &&
                    teams[2].getParticipatedPlayerCount() < teams[3].getParticipatedPlayerCount()) {
                teamId = (byte) 2;
            } else if (teams[3].getParticipatedPlayerCount() < teams[0].getParticipatedPlayerCount() &&
                    teams[3].getParticipatedPlayerCount() < teams[1].getParticipatedPlayerCount() &&
                    teams[3].getParticipatedPlayerCount() < teams[2].getParticipatedPlayerCount()) {
                teamId = (byte) 3;
            } else if (teams[0].getParticipatedPlayerCount() > teams[1].getParticipatedPlayerCount() &&
                    teams[2].getParticipatedPlayerCount() > teams[1].getParticipatedPlayerCount() &&
                    teams[1].getParticipatedPlayerCount() == teams[3].getParticipatedPlayerCount()) {
                while (teamId == 0 || teamId == 2) {
                    teamId = (byte) Rnd.get(4);
                }
            } else if (teams[0].getParticipatedPlayerCount() > teams[1].getParticipatedPlayerCount() &&
                    teams[3].getParticipatedPlayerCount() > teams[1].getParticipatedPlayerCount() &&
                    teams[1].getParticipatedPlayerCount() == teams[2].getParticipatedPlayerCount()) {
                while (teamId == 0 || teamId == 3) {
                    teamId = (byte) Rnd.get(4);
                }
            } else if (teams[0].getParticipatedPlayerCount() > teams[2].getParticipatedPlayerCount() &&
                    teams[1].getParticipatedPlayerCount() > teams[2].getParticipatedPlayerCount() &&
                    teams[2].getParticipatedPlayerCount() == teams[3].getParticipatedPlayerCount()) {
                while (teamId == 0 || teamId == 1) {
                    teamId = (byte) Rnd.get(4);
                }
            } else if (teams[0].getParticipatedPlayerCount() < teams[1].getParticipatedPlayerCount() &&
                    teams[0].getParticipatedPlayerCount() < teams[3].getParticipatedPlayerCount() &&
                    teams[0].getParticipatedPlayerCount() == teams[2].getParticipatedPlayerCount()) {
                while (teamId == 1 || teamId == 3) {
                    teamId = (byte) Rnd.get(4);
                }
            } else if (teams[0].getParticipatedPlayerCount() < teams[1].getParticipatedPlayerCount() &&
                    teams[0].getParticipatedPlayerCount() < teams[2].getParticipatedPlayerCount() &&
                    teams[0].getParticipatedPlayerCount() == teams[3].getParticipatedPlayerCount()) {
                while (teamId == 1 || teamId == 2) {
                    teamId = (byte) Rnd.get(4);
                }
            } else if (teams[0].getParticipatedPlayerCount() < teams[2].getParticipatedPlayerCount() &&
                    teams[0].getParticipatedPlayerCount() < teams[3].getParticipatedPlayerCount() &&
                    teams[0].getParticipatedPlayerCount() == teams[1].getParticipatedPlayerCount()) {
                while (teamId == 2 || teamId == 3) {
                    teamId = (byte) Rnd.get(4);
                }
            } else if (teams[0].getParticipatedPlayerCount() > teams[1].getParticipatedPlayerCount() &&
                    teams[0].getParticipatedPlayerCount() > teams[2].getParticipatedPlayerCount() &&
                    teams[0].getParticipatedPlayerCount() > teams[3].getParticipatedPlayerCount()) {
                while (teamId == 0) {
                    teamId = (byte) Rnd.get(4);
                }
            } else if (teams[1].getParticipatedPlayerCount() > teams[0].getParticipatedPlayerCount() &&
                    teams[1].getParticipatedPlayerCount() > teams[2].getParticipatedPlayerCount() &&
                    teams[1].getParticipatedPlayerCount() > teams[3].getParticipatedPlayerCount()) {
                while (teamId == 1) {
                    teamId = (byte) Rnd.get(4);
                }
            } else if (teams[2].getParticipatedPlayerCount() > teams[0].getParticipatedPlayerCount() &&
                    teams[2].getParticipatedPlayerCount() > teams[1].getParticipatedPlayerCount() &&
                    teams[2].getParticipatedPlayerCount() > teams[3].getParticipatedPlayerCount()) {
                while (teamId == 2) {
                    teamId = (byte) Rnd.get(4);
                }
            } else if (teams[3].getParticipatedPlayerCount() > teams[0].getParticipatedPlayerCount() &&
                    teams[3].getParticipatedPlayerCount() > teams[1].getParticipatedPlayerCount() &&
                    teams[3].getParticipatedPlayerCount() > teams[2].getParticipatedPlayerCount()) {
                teamId = (byte) Rnd.get(3);
            } else {
                teamId = (byte) Rnd.get(4);
            }
        }

        return teams[teamId].addPlayer(playerInstance);
    }

    public boolean removeParticipant(int playerObjectId) {
        // Get the teamId of the player
        byte teamId = getParticipantTeamId(playerObjectId);

        // Check if the player is participant
        if (teamId != -1) {
            // Remove the player from team
            teams[teamId].removePlayer(playerObjectId);
            return true;
        }

        return false;
    }

    public void sendToAllParticipants(IClientOutgoingPacket packet) {
        for (PlayerInstance playerInstance : teams[0].getParticipatedPlayers().values()) {
            if (playerInstance != null) {
                playerInstance.sendPacket(packet);
            }
        }

        for (PlayerInstance playerInstance : teams[1].getParticipatedPlayers().values()) {
            if (playerInstance != null) {
                playerInstance.sendPacket(packet);
            }
        }

        if (config.getLocation().getTeamCount() == 4) {
            for (PlayerInstance playerInstance : teams[2].getParticipatedPlayers().values()) {
                if (playerInstance != null) {
                    playerInstance.sendPacket(packet);
                }
            }
            for (PlayerInstance playerInstance : teams[3].getParticipatedPlayers().values()) {
                if (playerInstance != null) {
                    playerInstance.sendPacket(packet);
                }
            }
        }
    }

    public void sendToAllParticipants(String message) {
        sendToAllParticipants(new CreatureSay(null, ChatType.PARTYROOM_ALL, "Instanced Events", message));
    }

    public void onLogin(PlayerInstance playerInstance) {
        if (playerInstance != null && isPlayerParticipant(playerInstance.getObjectId())) {
            removeParticipant(playerInstance.getObjectId());
            // TODO: EVENT:  Teleport port back to main town
            /*EventTeam team = getParticipantTeam(playerInstance.getObjectId());
			team.addPlayer(playerInstance);
			if (isState(EventState.STARTING) || isState(EventState.STARTED))
			{
				for (L2Effect effect : playerInstance.getAllEffects()) if (effect != null) effect.exit();
				new EventTeleporter(playerInstance, getParticipantTeam(playerInstance.getObjectId()).getCoords(), true, false);
			}*/
        }
    }

    public void onLogout(PlayerInstance playerInstance) {
        if (playerInstance != null && isPlayerParticipant(playerInstance.getObjectId())) {
            removeParticipant(playerInstance.getObjectId());
        }
    }

    public String getInfo(PlayerInstance player) {
        String html = "<center><font color=\"LEVEL\">" + config.getEventString() + "</font></center><br>";
        if (isState(EventState.READY)) {
            if (config.isAllVsAll()) {
                if (teams[0].getParticipatedPlayerCount() > 0) {
                    html += "Participants:<br>";
                    for (PlayerInstance participant : teams[0].getParticipatedPlayers().values()) {
                        if (participant != null) {
                            html += EventsManager.getInstance().getPlayerString(participant, player) + ", ";
                        }
                    }
                    html = html.substring(0, html.length() - 2) + ".";
                }
            } else {
                for (EventTeam team : teams) {
                    if (team.getParticipatedPlayerCount() > 0) {
                        html += "Team " + team.getName() + " participants:<br>";
                        for (PlayerInstance participant : team.getParticipatedPlayers().values()) {
                            if (participant != null) {
                                html += EventsManager.getInstance().getPlayerString(participant, player) + ", ";
                            }
                        }
                        html = html.substring(0, html.length() - 2) + ".<br>";
                    }
                }
                if (html.length() > 4) {
                    html = html.substring(0, html.length() - 4);
                }
            }
        } else if (isState(EventState.STARTED)) {
            html += getRunningInfo(player);
        } else if (isState(EventState.INACTIVE)) {
            html += "This event has ended.";
        }

        return html;
    }

    public abstract String getRunningInfo(PlayerInstance player);

    public void observe(PlayerInstance playerInstance) {
        if (playerInstance.getEvent() != null) {
            playerInstance.sendMessage("You cannot observe an event when you are participating on it.");
            playerInstance.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }
        if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(playerInstance)) {
            playerInstance.sendMessage("You cannot observe an event while fighting.");
            playerInstance.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }
        if (playerInstance.isInsideZone(ZoneId.NO_SUMMON_FRIEND) || playerInstance.inObserverMode()) {
            playerInstance.sendMessage("You cannot observe an event from here.");
            playerInstance.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        int x;
        int y;
        int z;
        if (config.getLocation().getZone() == null) {
            int rndTm = Rnd.get(config.getLocation().getTeamCount());
            x = config.getLocation().getSpawn(rndTm).getX();
            y = config.getLocation().getSpawn(rndTm).getY();
            z = GeoEngine.getInstance().getHeight(x, y, config.getLocation().getGlobalZ());
        } else {
            Location pos = config.getLocation().getZone().getZone().getRandomPoint();
            x = pos.getX();
            y = pos.getY();
            z = GeoEngine.getInstance().getHeight(pos.getX(), pos.getY(), pos.getZ());
        }
        playerInstance.setInstanceId(instanceId);
        playerInstance.enterEventObserverMode(x, y, z);
    }

    public boolean onAction(PlayerInstance playerInstance, int targetPlayerObjectId) {
        EventTeam playerTeam = getParticipantTeam(playerInstance.getObjectId());
        EventTeam targetPlayerTeam = getParticipantTeam(targetPlayerObjectId);

        if (playerTeam == null || targetPlayerTeam == null) {
            return false;
        }

        return !(playerTeam == targetPlayerTeam && playerInstance.getObjectId() != targetPlayerObjectId &&
                !Config.INSTANCED_EVENT_TARGET_TEAM_MEMBERS_ALLOWED);
    }

    public boolean onForcedAttack(PlayerInstance playerInstance, int targetPlayerObjectId) {
        EventTeam playerTeam = getParticipantTeam(playerInstance.getObjectId());
        EventTeam targetPlayerTeam = getParticipantTeam(targetPlayerObjectId);

        if (playerTeam == null || targetPlayerTeam == null) {
            return false;
        }

        return !(playerTeam == targetPlayerTeam && playerInstance.getObjectId() != targetPlayerObjectId && config.isPvp());
    }

    public boolean onScrollUse(int playerObjectId) {
        if (!isState(EventState.STARTED)) {
            return true;
        }

        return !(isPlayerParticipant(playerObjectId) && !Config.INSTANCED_EVENT_SCROLL_ALLOWED);
    }

    public boolean onPotionUse(int playerObjectId) {
        if (!isState(EventState.STARTED)) {
            return true;
        }

        return !(isPlayerParticipant(playerObjectId) && !Config.INSTANCED_EVENT_POTIONS_ALLOWED);
    }

    public boolean onEscapeUse(int playerObjectId) {
        if (!isState(EventState.STARTED)) {
            return true;
        }

        return !isPlayerParticipant(playerObjectId);
    }

    public boolean onItemSummon(int playerObjectId) {
        if (!isState(EventState.STARTED)) {
            return true;
        }

        return !(isPlayerParticipant(playerObjectId) && !Config.INSTANCED_EVENT_SUMMON_BY_ITEM_ALLOWED);
    }

    public abstract void onKill(Creature killerCharacter, PlayerInstance killedPlayerInstance);

    public boolean isType(EventType type) {
        return config.getType() == type;
    }

    public EventType getType() {
        return config.getType();
    }

    public void setState(EventState state) {
        this.state = state;
    }

    public boolean isState(EventState state) {
        return this.state == state;
    }

    public byte getParticipantTeamId(int playerObjectId) {
        if (config.getLocation().getTeamCount() != 4) {
            return (byte) (teams[0].containsPlayer(playerObjectId) ? 0 : teams[1].containsPlayer(playerObjectId) ? 1 : -1);
        } else {
            return (byte) (teams[0].containsPlayer(playerObjectId) ? 0 : teams[1].containsPlayer(playerObjectId) ? 1 :
                    teams[2].containsPlayer(playerObjectId) ? 2 : teams[3].containsPlayer(playerObjectId) ? 3 : -1);
        }
    }

    public EventTeam getParticipantTeam(int playerObjectId) {
        if (config.getLocation().getTeamCount() != 4) {
            return teams[0].containsPlayer(playerObjectId) ? teams[0] : teams[1].containsPlayer(playerObjectId) ? teams[1] : null;
        } else {
            return teams[0].containsPlayer(playerObjectId) ? teams[0] : teams[1].containsPlayer(playerObjectId) ? teams[1] :
                    teams[2].containsPlayer(playerObjectId) ? teams[2] : teams[3].containsPlayer(playerObjectId) ? teams[3] : null;
        }
    }

    public EventTeam getParticipantEnemyTeam(int playerObjectId) {
        if (config.getLocation().getTeamCount() != 4) {
            return teams[0].containsPlayer(playerObjectId) ? teams[1] : teams[1].containsPlayer(playerObjectId) ? teams[0] : null;
        } else {
            return teams[0].containsPlayer(playerObjectId) ? teams[1] : teams[1].containsPlayer(playerObjectId) ? teams[0] :
                    teams[2].containsPlayer(playerObjectId) ? teams[3] : teams[3].containsPlayer(playerObjectId) ? teams[2] : null;
        }
    }

    public Point3D getParticipantTeamCoordinates(int playerObjectId) {
        if (config.getLocation().getTeamCount() != 4) {
            return teams[0].containsPlayer(playerObjectId) ? teams[0].getCoords() :
                    teams[1].containsPlayer(playerObjectId) ? teams[1].getCoords() : null;
        } else {
            return teams[0].containsPlayer(playerObjectId) ? teams[0].getCoords() : teams[1].containsPlayer(playerObjectId) ? teams[1].getCoords() :
                    teams[2].containsPlayer(playerObjectId) ? teams[2].getCoords() :
                            teams[3].containsPlayer(playerObjectId) ? teams[3].getCoords() : null;
        }
    }

    public boolean isPlayerParticipant(int playerObjectId) {
        if (!isState(EventState.STARTED)) {
            return false;
        }

        if (config.getLocation().getTeamCount() != 4) {
            return teams[0].containsPlayer(playerObjectId) || teams[1].containsPlayer(playerObjectId);
        } else {
            return teams[0].containsPlayer(playerObjectId) || teams[1].containsPlayer(playerObjectId) || teams[2].containsPlayer(playerObjectId) ||
                    teams[3].containsPlayer(playerObjectId);
        }
    }

    public int getParticipatedPlayersCount() {
        //if (!isState(EventState.PARTICIPATING) && !isState(EventState.STARTING) && !isState(EventState.STARTED))
        //	return 0;

        int count = 0;
        for (int teamId = 0; teamId < config.getLocation().getTeamCount(); teamId++) {
            count += teams[teamId].getParticipatedPlayerCount();
        }
        return count;
    }

    protected PlayerInstance selectRandomParticipant() {
        return teams[0].selectRandomParticipant();
    }

    public int getId() {
        return id;
    }

    public EventConfig getConfig() {
        return config;
    }

    public int getInstanceId() {
        return instanceId;
    }

    public void setImportant(PlayerInstance player, boolean important) {
        if (player == null) {
            return;
        }

        if (config.getLocation().getTeamCount() != 4 && config.isAllVsAll()) {
            player.setTeam(getParticipantTeamId(player.getObjectId()) + 1);
        } else {
            if (getParticipantTeamId(player.getObjectId()) == 0) {
                player.getAppearance().setNameColor(Integer.decode("0xFF0000"));
                player.getAppearance().setTitleColor(Integer.decode("0xFF0000"));
            }
            if (getParticipantTeamId(player.getObjectId()) == 1) {
                player.getAppearance().setNameColor(Integer.decode("0x0000FF"));
                player.getAppearance().setTitleColor(Integer.decode("0x0000FF"));
            }
            if (getParticipantTeamId(player.getObjectId()) == 2) {
                player.getAppearance().setNameColor(Integer.decode("0x00FFFF"));
                player.getAppearance().setTitleColor(Integer.decode("0x00FFFF"));
            }
            if (getParticipantTeamId(player.getObjectId()) == 3) {
                player.getAppearance().setNameColor(Integer.decode("0x00FF00"));
                player.getAppearance().setTitleColor(Integer.decode("0x00FF00"));
            }
            player.setTeam(0);
        }
        player.broadcastUserInfo();
        if (isType(EventType.LuckyChests) || isType(EventType.StalkedSalkers) || isType(EventType.SimonSays)) {
            player.disarmWeapons();
            player.setIsEventDisarmed(true);
        }
    }

    public EventTeam[] getTeams() {
        return teams;
    }
}
