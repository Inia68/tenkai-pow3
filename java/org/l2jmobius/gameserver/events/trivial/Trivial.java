package org.l2jmobius.gameserver.events.trivial;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.instancemanager.ZoneManager;
import org.l2jmobius.gameserver.model.World;
import org.l2jmobius.gameserver.model.WorldObject;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.quest.Quest;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;
import org.l2jmobius.gameserver.util.Broadcast;

import java.util.*;

public class Trivial extends Quest {

    public enum EventState {
        REGISTRATION,
        STARTED,
        NOT_RUNNING
    }

    private static Map<Integer, Integer> _players = new HashMap<>(); // <ObjectId, Points>
    private PlayerInstance _gm;

    private final int REGISTRATION_TIME = 1; // Minutes
    private final int MINIMUM_PARTICIPANTS = 1;
    private final int TIME_TO_ANSWER = 15; // Seconds

    private final int ZONE_YES = 80070;
    private final int ZONE_NO = 80071;

    ZoneType _zoneNo = ZoneManager.getInstance().getZoneById(ZONE_NO);
    ZoneType _zoneYes = ZoneManager.getInstance().getZoneById(ZONE_YES);

    private EventState _eventState = EventState.NOT_RUNNING;
    List<Integer> _toRemove =  new LinkedList<Integer>();

    public boolean openRegistrations(PlayerInstance gm) {
        if (_eventState != EventState.NOT_RUNNING) {
            if (_players.isEmpty()) {
                _eventState = EventState.NOT_RUNNING;
            } else {
                return false;
            }
        }
        Broadcast.toAllOnlinePlayers("Trivial registrations are opened !");
        Broadcast.toAllOnlinePlayers("You have " + REGISTRATION_TIME * 60 + " seconds to register");
        Broadcast.toAllOnlinePlayers("Write .trivial to register");

        startQuestTimer("close_registrations", REGISTRATION_TIME * 10 * 1000, null, null, false);
        _gm = gm;
        _eventState = EventState.REGISTRATION;
        return true;
    }

    public boolean stopEvent() {
        if (_eventState != EventState.STARTED) {
            return false;
        }
        _players.clear();
        _eventState = EventState.NOT_RUNNING;
        _gm = null;
        return  true;
    }

    public void showPanel() {
        String html = "<html><body>";
        html += "<table width=270>\n" +
                "<tr>\n" +
                "<td>Question</td>\n" +
                "<td><edit var=\"qbox\" width=120 height=15></td>\n" +
                "</tr>\n" +
                "<tr align=center>\n" +
                "<td><button value=\"True\" action=\"bypass -h admin_gm_event ;start;trivial;true; $qbox \" width=40 height=21 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>\n" +
                "<td><button value=\"False\" action=\"bypass -h admin_gm_event ;start;trivial;false; $qbox \" width=40 height=21 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>\n" +
                "</tr>\n" +
                "<tr>" +
                "<td><button value=\"Stop Event\" action=\"bypass -h admin_gm_event;stop;trivial\" width=40 height=21 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>\n" +
                "</tr>" +
                "</table>";
        html += "</body></html>";

        _gm.sendPacket(new NpcHtmlMessage(0, html));
    }

    public void ask(String question, Boolean answer) {
        broadcastPacket(question, 5000);
        startQuestTimer("ask;" + answer.toString(), 10, null, null, false);
    }

    public Boolean register(PlayerInstance player) {
        if (_eventState != EventState.REGISTRATION) {
            player.sendMessage("The event isn't in registration mode");
            return false;
        }
        if (_players.get(player.getObjectId()) != null) {
            player.sendMessage("You're already registered.");
            return false;
        }
        if (player.isOnEvent()) {
            player.sendMessage("You're already inside an event !");
            return false;
        }
        if (player.isInInstance()) {
            player.sendMessage("You can't register while you're in an instance.");
            return false;
        }
        _players.put(player.getObjectId(), 0);
        player.sendMessage("Registered !");
        return true;
    }

    public String onAdvEvent(String event, Npc npc, PlayerInstance player) {
        if (event.equalsIgnoreCase("teleport_to_trivial")) {
            player.teleToLocation(-59004, -56889, -2032);
        }
        else if (event.equalsIgnoreCase("close_registrations")) {

            if (_players.size() < MINIMUM_PARTICIPANTS) {
                _eventState = EventState.NOT_RUNNING;
                sendToParticipants("Event aborted due to not enough participation.");
                return "";
            }

            _eventState = EventState.STARTED;
            _players.forEach((objectId, points) -> {
                // Teleport participants to the zone
                PlayerInstance p = World.getInstance().getPlayer(objectId);
                if (p == null) {
                    _toRemove.add(objectId);
                    return;
                }
                p.teleToLocation(-59704, -54008, -2115);
            });
            showPanel();
        }
        else if (event.equalsIgnoreCase("register")) {


            return "";
        }
        else if (event.startsWith("ask")) {
            final String[] params = event.split(";");
            startQuestTimer("immobilize;" + params[1], TIME_TO_ANSWER * 1000, null, null, false);
        }
        else if (event.startsWith("immobilize")) {
            _players.forEach((objectId, points) -> {
                PlayerInstance p = World.getInstance().getPlayer(objectId);
                if (p == null) {
                    _toRemove.add(objectId);
                    return;
                }
                p.setImmobilized(true);
            });
            final String[] params = event.split(";");
            startQuestTimer("answer;" + params[1], 5000, null, null, false);
        }
        else if (event.startsWith("answer")) {
            final String[] params = event.split(";");

            Boolean  answer = Boolean.parseBoolean(params[1]);

            for (int objectId: _players.keySet()) {
                PlayerInstance p = World.getInstance().getPlayer(objectId);
                if (p == null) {
                    _toRemove.add(objectId);
                    continue;
                }
                p.setImmobilized(false);
                if ((answer && _zoneYes.isInsideZone(p.getLocation())) || (answer == false && _zoneNo.isInsideZone(p.getLocation()))) {
                    p.sendPacket(new ExShowScreenMessage("You answered correctly !", 3000));
                    _players.put(objectId, _players.get(objectId) + 1);
                } else {
                    p.sendPacket(new ExShowScreenMessage("You answered wrongly !", 3000));
                    p.teleToLocation( -59720, -54280,-2033, 15459, true);
                    _toRemove.add(objectId);
                }
            };
            kickPlayers();
            if (_players.size() <= 1) {
                broadcastPacket("You won !", 3000);
                stopEvent();
                return "";
            }
            showPanel();
        }
        return "";
    }

    void kickPlayers() {
        _toRemove.forEach(id -> {
            if (_players.get(id) != null) {
                _players.remove(id);
            }
        });
        _toRemove.clear();
    }

    public void broadcastPacket(String msg, int time) {
        _players.forEach((objectId, points) -> {
            PlayerInstance p = World.getInstance().getPlayer(objectId);
            if (p == null) {
                _toRemove.add(objectId);
                return;
            }
            p.sendPacket(new ExShowScreenMessage(msg, time));
        });
    }


    public void sendToParticipants(String msg) {
        _players.forEach((objectId, points) -> {
            PlayerInstance p = World.getInstance().getPlayer(objectId);
            if (p == null) {
                _toRemove.add(objectId);
                return;
            }
            p.sendMessage(msg);
        });
    }

    public Boolean isRunning() {
        return _eventState == EventState.STARTED;
    }

    public Trivial(int id) {
        super(id);
    }

    public static Trivial getInstance() {
        return Trivial.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        protected static final Trivial INSTANCE = new Trivial(999121);
    }

}