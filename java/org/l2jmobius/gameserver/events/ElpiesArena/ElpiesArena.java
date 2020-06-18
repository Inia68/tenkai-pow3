package org.l2jmobius.gameserver.events.ElpiesArena;

import org.l2jmobius.commons.concurrent.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.util.Broadcast;

import java.util.*;

public class ElpiesArena {
    public static enum State {
        INNACTIVE,
        REGISTRATION,
        ACTIVE
    }

    public static State state = State.INNACTIVE;
    public static Vector<PlayerInstance> registered = new Vector<>();
    public static Map<Integer, Integer> elpy = new HashMap<Integer, Integer>();
    public static int runEach = 1; //Min
    public static int registTime = 60; //Seconds
    public static int time = 5; //Min
    public static int minPlayers = 2;
    public static int life = 6;

    public void openRegistration() {
        if (state != State.INNACTIVE) {
            return;
        }
        state = State.REGISTRATION;
        Broadcast.toAllOnlinePlayers("The registrations for Elpy Event are open.");
        Broadcast.toAllOnlinePlayers("Write .elpy to register.");
        ThreadPool.schedule(this::runEvent, 1000 * registTime);
    }

    public void addPlayer(PlayerInstance player) {
        if (player == null) {
            return;
        }
        if (state != State.REGISTRATION) {
            player.sendMessage("You can't register for the moment.");
            return;
        }
        registered.add(player);
        elpy.put(player.getObjectId(), life);
        player.setTitle("Life : " + life);
        player.broadcastTitleInfo();
        player.broadcastUserInfo();
        player.sendMessage("You're now registered for the event!");
        return;
    }

    public void removePlayer(PlayerInstance player) {
        if (player == null) {
            return;
        }
        if (!registered.contains(player)) {
            player.sendMessage("You're not registered!");
            return;
        }
        registered.remove(player);
        elpy.remove(player.getObjectId());

        player.sendMessage("You left the event.");
        if (registered.size() < 2) {
            stopEvent();
        }

        return;
    }

    public void runEvent() {
        if (state != State.REGISTRATION) {
            return;
        }
        if (registered.size() < minPlayers) {
            Broadcast.toAllOnlinePlayers("Event aborted, not enough players registered");
            state = State.INNACTIVE;
            registered.clear();
            return;
        }

        state = State.ACTIVE;
        for (PlayerInstance player : registered) {
            if (player == null) {
                continue;
            }
            player.transform(105, true);
            player.setCurrentHp(player.getMaxHp());
            player.setPvpFlag(1);
            player.setPvpFlagLasts(30);
            player.setTitle("Life : " + life);
            player.broadcastTitleInfo();
            player.teleToLocation(-88082 + Rnd.get(400), -252843 + Rnd.get(400), -3336);
        }
        ThreadPool.schedule(this::stopEvent, 300000);
    }

    public void onAttack(PlayerInstance attacker, PlayerInstance target) {
        int hit = elpy.get(target.getObjectId());
        int atLife = elpy.get(attacker.getObjectId());

        Creature one = (Creature) attacker;

        if (!one.isTransformed() || !attacker.isTransformed()) {
            attacker.sendMessage("You're not an Elpy.");
            elpy.remove(attacker);
            registered.remove(attacker);
            attacker.doDie(target);
            one.doDie(target);
            attacker.teleToLocation(-114435, 253417, -1546);
            attacker.setPvpFlag(0);
            return;
        }

        elpy.put(target.getObjectId(), hit - 1);
        target.setTitle("Life : " + elpy.get(target.getObjectId()));
        target.broadcastUserInfo();
        target.broadcastTitleInfo();

        attacker.setTitle("Life : " + elpy.get(attacker.getObjectId()));
        attacker.broadcastUserInfo();
        attacker.broadcastTitleInfo();

        if (elpy.get(target.getObjectId()) <= 0) {
            onDie(attacker, target);
            attacker.sendMessage("You killed " + target.getName());
            int amount = (int) ((12 / elpy.size()) + 1);
            attacker.addItem("Kail's Coin", 5899, amount, attacker, true);
            if (atLife + 2 <= life) {
                elpy.put(attacker.getObjectId(), atLife + 2);
                attacker.setTitle("Life : " + elpy.get(attacker.getObjectId()));
                attacker.broadcastUserInfo();
                attacker.broadcastTitleInfo();
            } else if (atLife + 1 <= life) {
                elpy.put(attacker.getObjectId(), atLife + 1);
                attacker.setTitle("Life : " + elpy.get(attacker.getObjectId()));
                attacker.broadcastUserInfo();
                attacker.broadcastTitleInfo();
            }
            if (elpy.size() <= 1 || registered.size() <= 1) {
                stopEvent();
            }
        }
    }


    public void stopEvent() {
        if (state != State.ACTIVE) {
            return;
        }
        Broadcast.toAllOnlinePlayers("Event finished.");
        for (PlayerInstance player : registered) {
            if (player == null) {
                continue;
            }
            player.stopTransformation(true);
            player.teleToLocation(-114435, 253417, -1546);
            player.setPvpFlag(0);
            player.setCurrentHp(player.getMaxHp());
            player.setTitle("");
        }
        registered.clear();
        elpy.clear();
        state = State.INNACTIVE;
    }


    public void onDie(PlayerInstance attacker, PlayerInstance target) {
        target.setTitle("");
        target.stopTransformation(true);
        target.teleToLocation(-114435, 253417, -1546);
        target.setCurrentHp(target.getMaxHp());
        registered.remove(target);
        elpy.remove(target.getObjectId());
        if (elpy.size() <= 1) {
            Broadcast.toAllOnlinePlayers("King of elpies -> " + attacker.getName());
            stopEvent();
        }
    }

    protected ElpiesArena() {
        if (state != State.INNACTIVE) {
            return;
        }
        openRegistration();
    }

    protected class Event {

    }


    public static ElpiesArena getInstance() {
        return SingletonHolder.instance;
    }

    private static class SingletonHolder {
        protected static final ElpiesArena instance = new ElpiesArena();
    }
}
