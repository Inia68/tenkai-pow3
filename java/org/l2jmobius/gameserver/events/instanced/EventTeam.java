package org.l2jmobius.gameserver.events.instanced;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.datatables.SpawnTable;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.util.Point3D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class EventTeam {
    /**
     * The name of the team<br>
     */
    private String name;
    /**
     * The team spot coordinated<br>
     */
    private Point3D coordinates;
    /**
     * The points of the team<br>
     */
    private short points;
    /**
     * Name and instance of all participated players in HashMap<br>
     */
    private Map<Integer, PlayerInstance> participatedPlayers = new LinkedHashMap<>();

    private int flagId = 44004;
    private Spawn flagSpawn;
    private int golemId = 44003;
    private Spawn golemSpawn;

    private PlayerInstance VIP;

    /**
     * C'tor initialize the team<br><br>
     *
     * @param name        as String<br>
     * @param coordinates as int[]<br>
     */
    public EventTeam(int id, String name, Point3D coordinates) {
        flagId = 9009113 + id;
        this.name = name;
        this.coordinates = coordinates;
        points = 0;
        flagSpawn = null;
    }

    /**
     * Adds a player to the team<br><br>
     *
     * @param playerInstance as Player<br>
     * @return boolean: true if success, otherwise false<br>
     */
    public boolean addPlayer(PlayerInstance playerInstance) {
        if (playerInstance == null) {
            return false;
        }

        synchronized (participatedPlayers) {
            participatedPlayers.put(playerInstance.getObjectId(), playerInstance);
        }

        return true;
    }

    /**
     * Removes a player from the team<br><br>
     */
    public void removePlayer(int playerObjectId) {
        synchronized (participatedPlayers) {
			/*if (!EventsManager.getInstance().isType(EventType.DM)
                    && !EventsManager.getInstance().isType(EventType.SS)
					&& !EventsManager.getInstance().isType(EventType.SS2))
				participatedPlayers.get(playerObjectId).setEvent(null);*/
            participatedPlayers.remove(playerObjectId);
        }
    }

    /**
     * Increases the points of the team<br>
     */
    public void increasePoints() {
        ++points;
    }

    public void decreasePoints() {
        --points;
    }

    /**
     * Cleanup the team and make it ready for adding players again<br>
     */
    public void cleanMe() {
        participatedPlayers.clear();
        participatedPlayers = new HashMap<>();
        points = 0;
    }

    public void onEventNotStarted() {
        for (PlayerInstance player : participatedPlayers.values()) {
            if (player != null) {
                player.setEvent(null);
            }
        }
        cleanMe();
    }

    /**
     * Is given player in this team?<br><br>
     *
     * @return boolean: true if player is in this team, otherwise false<br>
     */
    public boolean containsPlayer(int playerObjectId) {
        boolean containsPlayer;

        synchronized (participatedPlayers) {
            containsPlayer = participatedPlayers.containsKey(playerObjectId);
        }

        return containsPlayer;
    }

    /**
     * Returns the name of the team<br><br>
     *
     * @return String: name of the team<br>
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the coordinates of the team spot<br><br>
     *
     * @return int[]: team coordinates<br>
     */
    public Point3D getCoords() {
        return coordinates;
    }

    /**
     * Returns the points of the team<br><br>
     *
     * @return short: team points<br>
     */
    public short getPoints() {
        return points;
    }

    /**
     * Returns name and instance of all participated players in HashMap<br><br>
     *
     * @return Map<String   ,       Player>: map of players in this team<br>
     */
    public Map<Integer, PlayerInstance> getParticipatedPlayers() {
        // Map<Integer, PlayerInstance> participatedPlayers = null;

        synchronized (participatedPlayers) {
            // this.participatedPlayers = participatedPlayers;
        }

        return participatedPlayers;
    }

    /**
     * Returns player count of this team<br><br>
     *
     * @return int: number of players in team<br>
     */
    public int getParticipatedPlayerCount() {
        int participatedPlayerCount;

        synchronized (participatedPlayers) {
            participatedPlayerCount = participatedPlayers.size();
        }

        return participatedPlayerCount;
    }

    public int getAlivePlayerCount() {
        int alivePlayerCount = 0;

        ArrayList<PlayerInstance> toIterate = new ArrayList<>(participatedPlayers.values());
        for (PlayerInstance player : toIterate) {
            if (!player.isOnline() || player.getClient() == null || player.getEvent() == null) {
                participatedPlayers.remove(player.getObjectId());
            }
            if (!player.isDead()) {
                alivePlayerCount++;
            }
        }
        return alivePlayerCount;
    }

    public int getHealersCount() {
        int count = 0;

        for (PlayerInstance player : participatedPlayers.values()) {
            if (player.getClassId().getId() == 146) {
                count++;
            }
        }
        return count;
    }

    public PlayerInstance selectRandomParticipant() {
        int rnd = Rnd.get(participatedPlayers.size());
        int i = 0;
        for (PlayerInstance participant : participatedPlayers.values()) {
            if (i == rnd) {
                return participant;
            }
            i++;
        }
        return null;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCoords(Point3D coords) {
        coordinates = coords;
    }

    public Spawn getFlagSpawn() {
        return flagSpawn;
    }

    public void setFlagSpawn(Spawn spawn) {
        if (flagSpawn != null && flagSpawn.getLastSpawn() != null) {
            flagSpawn.getLastSpawn().deleteMe();
            flagSpawn.stopRespawn();
            SpawnTable.getInstance().deleteSpawn(flagSpawn, false);
        }

        flagSpawn = spawn;
    }

    public int getFlagId() {
        return flagId;
    }

    public void setVIP(PlayerInstance character) {
        VIP = character;
    }

    public PlayerInstance getVIP() {
        return VIP;
    }

    public boolean isAlive() {
        return getAlivePlayerCount() > 0;
    }

    public Spawn getGolemSpawn() {
        return golemSpawn;
    }

    public void setGolemSpawn(Spawn spawn) {
        if (golemSpawn != null && golemSpawn.getLastSpawn() != null) {
            golemSpawn.getLastSpawn().deleteMe();
        }
        golemSpawn = spawn;
    }

    public int getGolemId() {
        return golemId;
    }
}
