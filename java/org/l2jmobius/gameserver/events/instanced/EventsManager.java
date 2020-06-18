package org.l2jmobius.gameserver.events.instanced;

import org.l2jmobius.Config;
import org.l2jmobius.commons.concurrent.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.instancemanager.ZoneManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.Broadcast;
import org.l2jmobius.gameserver.util.xml.XmlDocument;
import org.l2jmobius.gameserver.util.xml.XmlNode;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EventsManager {
    public static EventsManager instance = null;

    private HashMap<Integer, EventLocation> locations = new HashMap<>();

    private EventManagerTask task;
    private ConcurrentHashMap<Integer, EventInstance> instances = new ConcurrentHashMap<>();
    private int nextInstanceId = 1;

    private EventConfig currentConfig = null;
    private Map<Integer, PlayerInstance> registeredPlayers = new HashMap<>();

    public static EventsManager getInstance() {
        if (instance == null) {
            instance = new EventsManager();
        }
        return instance;
    }

    public void skipEvent(PlayerInstance player) {
        currentConfig = new EventConfig();
        Broadcast.toAllOnlinePlayers("" + player.getName() + " skipped event");
    }

    public void start() {


        // Load the configuration
        loadConfig();

        task = new EventManagerTask();
        ThreadPool.scheduleAtFixedRate(task, 10000L, 60000L);
        currentConfig = new EventConfig();

    }

    public void loadConfig() {
        locations.clear();

        XmlDocument doc = new XmlDocument(new File(Config.DATAPACK_ROOT,  "config/eventsConfig.xml"));
        int locCount = 0;
        for (XmlNode node : doc.getChildren()) {
            if (node.getName().equals("location")) {
                EventLocation loc = new EventLocation(node);
                locations.put(loc.getId(), loc);

                locCount++;
            }
        }

        System.out.println("Events Manager: loaded " + locCount + " locations");
    }

    public EventLocation getRandomLocation() {
        EventLocation loc;
        do {
            loc = locations.get(Rnd.get(100));
        } while (loc == null);
        return loc;
    }

    public EventLocation getLocation(int id) {
        return locations.get(id);
    }

    public EventManagerTask getTask() {
        return task;
    }

    class EventManagerTask implements Runnable {
        private int minutesToStart;

        public EventManagerTask() {
            minutesToStart = Config.INSTANCED_EVENT_INTERVAL;
        }

        @Override
        public void run() {
            List<Integer> toRemove = new ArrayList<>();
            try {
                for (EventInstance event : instances.values()) {
                    if (event == null) {
                        continue;
                    }

                    if (event.isState(EventInstance.EventState.INACTIVE)) {
                        toRemove.add(event.getId());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                instances.clear();
            }

            for (int eventId : toRemove) {
                instances.remove(eventId);
            }

            minutesToStart--;

            if (minutesToStart <= 0) {
                // Prepare an instance
                if (!prepare()) {
                    Broadcast.toAllOnlinePlayers("The event could not start because it lacked registered players.");
                }

                currentConfig = new EventConfig();
                minutesToStart = Config.INSTANCED_EVENT_INTERVAL;
            } else if (minutesToStart == 10 || minutesToStart == 5 || minutesToStart == 2 || minutesToStart == 1) {
                // Auto join!
				/*if (minutesToStart == 1)
                {
					for (Player player : World.getInstance().getAllPlayers().values())
					{
						if (player == null || player.isGM())
							continue;
						if (player.isOnline() && !isPlayerParticipant(player.getObjectId()))
							join(player);
					}
				}*/

                Broadcast.toAllOnlinePlayers(new ExShowScreenMessage(
                        "The " + currentConfig.getEventName() + " will start in " + minutesToStart + " minute" + (minutesToStart > 1 ? "s" : "") +
                                ".", 5000));
                ThreadPool.schedule(() -> Broadcast.toAllOnlinePlayers(new ExShowScreenMessage(
                                "Use the Community Board's (ALT+B) \"Join Events\" menu to join.",
                                5000)), 5000L);
            }
        }

        public int getMinutesToStart() {
            return minutesToStart;
        }
    }

    public EventConfig getCurrentConfig() {
        return currentConfig;
    }

    public Map<Integer, PlayerInstance> getRegisteredPlayers() {
        return registeredPlayers;
    }

    private boolean prepare() {
        if (registeredPlayers.isEmpty()) {
            return false;
        }

        // First sort the registered players
        int[][] sorted = new int[registeredPlayers.size()][2];
        int i = 0;
        for (PlayerInstance player : registeredPlayers.values()) {
            if (player == null || OlympiadManager.getInstance().isRegisteredInComp(player) || player.isInOlympiadMode() || player.isOlympiadStart() ||
                    player.isFlyingMounted() || player.inObserverMode()) {
                continue;
            }

            int objId = player.getObjectId();
            int strPoints = player.getSTR();
            // Find the index of where the current player should be put
            int j = 0;
            while (j < i && strPoints < sorted[j][1]) {
                j++;
            }
            // Move the rest
            for (int k = i; k > j; k--) {
                int temp1 = sorted[k][0];
                int temp2 = sorted[k][1];
                sorted[k][0] = sorted[k - 1][0];
                sorted[k][1] = sorted[k - 1][1];
                sorted[k - 1][0] = temp1;
                sorted[k - 1][1] = temp2;
            }
            // And put the current player in the blank space
            sorted[j][0] = objId;
            sorted[j][1] = strPoints;

            i++;
        }

        // Next divide all the registered players in groups, depending on the location's maximum room
        List<List<Integer>> groups = new ArrayList<>();
        i = 0;
        while (i < sorted.length) {
            List<Integer> group = new ArrayList<>();
            int j = 0;
            while (i + j < sorted.length) {
                group.add(sorted[i + j][0]);

                //if (Config.isServer(Config.TENKAI) && j >= currentConfig.getLocation().getMaxPlayers())
                //	break;

                j++;
            }

            if (j < currentConfig.getMinPlayers()) {
                if (!groups.isEmpty()) {
                    groups.get(groups.size() - 1).addAll(group);
                }

                break;
            }

            groups.add(group);
            i += j;
        }

        // And finally create the event instances according to the generated groups
        for (List<Integer> group : groups) {
            EventInstance ei = createInstance(nextInstanceId++, group, currentConfig);
            if (ei != null) {
                instances.put(ei.getId(), ei);
                Broadcast.toAllOnlinePlayers("Event registrations closed.");// The next event will be a " + currentConfig.getEventString() + ". Type .event to join.");

                for (EventTeam team : ei.getTeams()) {
                    for (int memberId : team.getParticipatedPlayers().keySet()) {
                        registeredPlayers.remove(memberId);
                    }
                }
            }

            return true;
        }

        return false;
    }

    public void onLogin(PlayerInstance playerInstance) {
        if (playerInstance != null && isPlayerParticipant(playerInstance.getObjectId())) {
            removeParticipant(playerInstance.getObjectId());
            if (playerInstance.getEvent() != null) {
                playerInstance.stopAllEffects();
                playerInstance.getEvent().onLogin(playerInstance);
            }
        }
    }

    public void onLogout(PlayerInstance playerInstance) {
        if (playerInstance != null && isPlayerParticipant(playerInstance.getObjectId())) {
            if (playerInstance.getEvent() != null) {
                playerInstance.stopAllEffects();
                playerInstance.getEvent().onLogout(playerInstance);
            }

            removeParticipant(playerInstance.getObjectId());
        }

        playerInstance.setEvent(null);
    }

    public void join(PlayerInstance playerInstance) {
        if (isPlayerParticipant(playerInstance.getObjectId())) {
            return;
        }

        NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(0);

        if (playerInstance.isCursedWeaponEquipped()) {
            npcHtmlMessage.setHtml(
                    "<html><head><title>Instanced Events</title></head><body>Cursed weapon owners are not allowed to participate.</body></html>");
        } else if (OlympiadManager.getInstance().isRegisteredInComp(playerInstance)) {
            npcHtmlMessage.setHtml(
                    "<html><head><title>Instanced Events</title></head><body>You can not participate when registered for Olympiad.</body></html>");
        } else if (playerInstance.getReputation() < 0) {
            npcHtmlMessage.setHtml(
                    "<html><head><title>Instanced Events</title></head><body>Chaotic players are not allowed to participate.</body></html>");
        } else if (playerInstance.getClassId().getId() < 139) {
            npcHtmlMessage.setHtml(
                    "<html><head><title>Instanced Events</title></head><body>You cannot participate, you must be an awakened class.</body></html>");
        } else if (playerInstance.isJailed()) {
            npcHtmlMessage.setHtml(
                    "<html><head><title>Instanced Events</title></head><body>You cannot participate, you must wait your jail time.</body></html>");
        } else if (playerInstance.isCastingNow()) {
            npcHtmlMessage.setHtml("<html><head><title>Instanced Events</title></head><body>You can't register while casting a skill.</body></html>");
        } /*else if (checkDualBox(playerInstance)) {
            npcHtmlMessage.setHtml(
                    "<html><head><title>Instanced Events</title></head><body>You have another character already registered for this event!</body></html>");
        }*/ else if (playerInstance.getInstanceId() != 0) {
            npcHtmlMessage.setHtml(
                    "<html><head><title>Instanced Events</title></head><body>You can't join one event while in other instance!</body></html>");
        } else if (playerInstance.isInDuel() || playerInstance.isDead()) {
            npcHtmlMessage.setHtml("<html><head><title>Instanced Events</title></head><body>You can't join one event at this moment!</body></html>");
        } else {
            if (addParticipant(playerInstance)) {
                CommunityBoardHandler.separateAndSend(getEventInfoPage(playerInstance), playerInstance);
            }

            return;
        }

        playerInstance.sendPacket(npcHtmlMessage);
    }

    public synchronized boolean addParticipant(PlayerInstance playerInstance) {
        // Check for nullpoitner
        if (playerInstance == null) {
            return false;
        }

        registeredPlayers.put(playerInstance.getObjectId(), playerInstance);

        return true;
    }

    public void leave(PlayerInstance playerInstance) {
        if (!isPlayerParticipant(playerInstance.getObjectId())) {
            return;
        }

        // If the event is started the player shouldn't be allowed to leave
        if (playerInstance.getEvent() != null && playerInstance.getEvent().isState(EventInstance.EventState.STARTED)) {
            return;
        }

        if (removeParticipant(playerInstance.getObjectId())) {
            CommunityBoardHandler.separateAndSend(getEventInfoPage(playerInstance), playerInstance);
        }
    }

    public boolean removeParticipant(int playerObjectId) {
        if (registeredPlayers.remove(playerObjectId) != null) {
            return true;
        }

        EventInstance event = getParticipantEvent(playerObjectId);
        if (event != null) {
            return event.removeParticipant(playerObjectId);
        }

        return false;
    }

    public String getEventInfoPage(PlayerInstance player) {
        if (!Config.INSTANCED_EVENT_ENABLED) {
            return "";
        }

        String result = null;
        if (player.getEvent() != null && player.getEvent().isState(EventInstance.EventState.STARTED)) {
            result = HtmCache.getInstance().getHtm(null, "data/html/CommunityBoard/Custom/event/runningEvent.htm");
            result = result.replace("%runningEventInfo%", player.getEvent().getInfo(player));
            return result;
        } else {
            result = HtmCache.getInstance().getHtm(null, "data/html/CommunityBoard/Custom/event/joinEvents.html");
        }

        final String navigation = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/navigation.html");
        result = result.replace("%navigation%", navigation);
        //PvP Event
        result = result.replace("%pvpEventName%", currentConfig.getEventName());
        result = result.replace("%pvpEventLocation%", currentConfig.getEventLocationName());
        result = result.replace("%pvpEventTime%", task.getMinutesToStart() + " minute" + (task.getMinutesToStart() > 1 ? "s" : ""));
        result = result.replace("%pvpEventId%", String.valueOf(currentConfig.getEventImageId()));
        result = result.replace("%pvpInfoLink%", String.valueOf(currentConfig.getType()));

        if (registeredPlayers.isEmpty()) {
            result = result.replace("%pvpEventPlayers%", "");
            result = result.replace("Registered Players for the Event", "");
        } else {
            result = result.replace("%pvpEventPlayers%", getRegisteredPlayers(player));
        }

        //Both events
        if (isPlayerParticipant(player.getObjectId())) {
            result = result.replace("%leaveButton%",
                    "<button value=\"Leave Match making\" action=\"bypass -h InstancedEventLeave\" width=255 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
            result = result.replace("%pvpEventJoinButton%", "");
        } else {
            result = result.replace("%pvpEventJoinButton%",
                    "<button value=\"Join Match making\" action=\"bypass -h InstancedEventJoin true\" width=255 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
            result = result.replace("%leaveButton%", "");
        }

        //Observe part?
        if (instances.isEmpty()) {
            result = result.replace("Current Observable Events", "");
            result = result.replace("%observeEvents%", "");
        } else {
            int remaining = instances.size();
            int pageCheck = 1;
            int total = 1;
            String eventString = "";

            for (EventInstance event : instances.values()) {
                if (!event.isState(EventInstance.EventState.STARTED)) {
                    remaining--;
                    if (!eventString.isEmpty() && (pageCheck == 6 || remaining == 0)) {
                        pageCheck = 1;
                        eventString += "</tr>";
                    }

                    continue;
                }

                if (pageCheck == 1) {
                    eventString += "<tr>";
                }

                eventString += "<td align=center><button value=\"" + event.getConfig().getEventName() + " #" + total +
                        "\" action=\"bypass -h InstancedEventObserve " + event.getId() +
                        "\" width=110 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>";

                // temp fix
                event.getInfo(player);

                pageCheck++;
                remaining--;
                total++;

                if (pageCheck == 6 || remaining == 0) {
                    pageCheck = 1;
                    eventString += "</tr>";
                }
            }

            result = result.replace("%observeEvents%", eventString);
            //result += "<br><br><br><br>";
        }

        return result;
    }

    private String getRegisteredPlayers(PlayerInstance player) {
        String result = "";

        if (registeredPlayers.isEmpty()) {
            return result;
        }

        for (PlayerInstance participant : registeredPlayers.values()) {
            if (participant == null) {
                continue;
            }

            result += getPlayerString(participant, player) + ", ";
        }

        if (!result.isEmpty()) {
            result = result.substring(0, result.length() - 2);
            result += ".";
        }

        return result;
    }

    public String getPlayerString(PlayerInstance player, PlayerInstance reader) {
        String color = "FFFFFF";
        if (player == reader) {
            color = "FFFF00";
        } else if (player.getFriendList().contains(reader.getObjectId())) {
            color = "00FFFF";
        } else if (reader.getParty() != null && reader.getParty() == player.getParty()) {
            color = "00FF00";
        } else if (reader.getClan() != null) {
            if (reader.getClanId() > 0 && reader.getClanId() == player.getClanId()) {
                color = "8888FF";
            } else if (reader.getAllyId() > 0 && reader.getAllyId() == player.getAllyId()) {
                color = "88FF88";
            } else if (reader.getClan().isAtWarWith(player.getClanId())) {
                color = "CC0000";
            }
        }
        return "<font color=\"" + color + "\">" + player.getName() + "</font>";
    }

    public int getParticipantEventId(int playerObjectId) {
        for (EventInstance event : instances.values()) {
            if (event.isPlayerParticipant(playerObjectId)) {
                return event.getId();
            }
        }
        return -1;
    }

    public EventInstance getParticipantEvent(int playerObjectId) {
        for (EventInstance event : instances.values()) {
            if (event.isPlayerParticipant(playerObjectId)) {
                return event;
            }
        }
        return null;
    }

    public byte getParticipantTeamId(int playerObjectId) {
        EventInstance event = getParticipantEvent(playerObjectId);
        if (event == null) {
            return -1;
        }
        return event.getParticipantTeamId(playerObjectId);
    }

    public EventTeam getParticipantTeam(int playerObjectId) {
        EventInstance event = getParticipantEvent(playerObjectId);
        if (event == null) {
            return null;
        }
        return getParticipantEvent(playerObjectId).getParticipantTeam(playerObjectId);
    }

    public EventTeam getParticipantEnemyTeam(int playerObjectId) {
        EventInstance event = getParticipantEvent(playerObjectId);
        if (event == null) {
            return null;
        }
        return getParticipantEvent(playerObjectId).getParticipantEnemyTeam(playerObjectId);
    }

    public boolean isPlayerParticipant(int playerObjectId) {
        if (registeredPlayers.containsKey(playerObjectId)) {
            return true;
        }

        for (EventInstance event : instances.values()) {
            if (event.isPlayerParticipant(playerObjectId)) {
                return true;
            }
        }
        return false;
    }

    public EventInstance createInstance(int id, List<Integer> group, EventConfig config) {
        // A map of lists to access the players sorted by class
        Map<Integer, List<PlayerInstance>> playersByClass = new HashMap<>();
        // Classify the players according to their class
        for (int playerId : group) {
            if (playerId == 0) {
                continue;
            }

            PlayerInstance player = World.getInstance().getPlayer(playerId);
            int classId = player.getClassId().getId();
            if (classId == -1) {
                classId = 147;
            }

            List<PlayerInstance> players = playersByClass.get(classId);
            if (players == null) {
                players = new ArrayList<>();
                playersByClass.put(classId, players);
            }

            players.add(player);
        }

        // If we found none, don't do anything
        if (playersByClass.isEmpty()) {
            return null;
        }

        // Create the event and fill it with the players, in class order
        EventInstance event = config.createInstance(id);
        for (int classId = 139; classId <= 190; classId++) {
            List<PlayerInstance> players = playersByClass.get(classId);
            if (players == null) {
                continue;
            }

            for (PlayerInstance player : players) {
                event.addParticipant(player);
            }
        }

        return event;
    }

    private boolean checkDualBox(PlayerInstance player) {
        if (player == null) {
            return false;
        }

        for (PlayerInstance registered : registeredPlayers.values()) {
            if (registered == null) {
                continue;
            }

            if (player.getIPAddress().equalsIgnoreCase(registered.getIPAddress())) {
                return true;
            }
        }

        return false;

        //TODO LasTravel: Hwid check don't work if we don't have LG
        /*String hwId = player.getClient().getHWId();
		for (Player registered : registeredPlayers.values())
		{
			if (registered.getClient() != null
					&& registered.getClient().getHWId() != null
					&& registered.getClient().getHWId().equals(hwId))
				return true;
		}
		return false;*/
    }

    public void handleBypass(PlayerInstance activeChar, String command) {
        if (activeChar == null) {
            return;
        }

        if (command.startsWith("InstancedEventJoin")) {
            join(activeChar);
        } else if (command.equals("InstancedEventLeave")) {
            leave(activeChar);
        } else if (command.startsWith("InstancedEventObserve")) {
            int eventId = Integer.valueOf(command.substring(22));
            if (instances.get(eventId) != null) {
                instances.get(eventId).observe(activeChar);
            }
        }

		/*else if (command.startsWith("InstancedEventParticipation"))
		{
			int eventId = Integer.valueOf(command.substring(25));
			if (Events.getInstance().Events.getInstance().get(eventId) != null)
				Events.getInstance().Events.getInstance().get(eventId).join(activeChar);
		}
		else if (command.startsWith("InstancedEventStatus"))
		{
			int eventId = Integer.valueOf(command.substring(18));
			if (Events.getInstance().Events.getInstance().get(eventId) != null)
				Events.getInstance().Events.getInstance().get(eventId).eventInfo(activeChar);
		}*/
    }

    public void reload() {
        locations.clear();
        loadConfig();
    }
}
