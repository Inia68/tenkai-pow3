package org.l2jmobius.gameserver.instancemanager;

import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;

import java.util.*;
import java.util.Map.Entry;

public class PlayerAssistsManager {
    public class PlayerInfo {
        public Map<PlayerInstance, Long> AttackTimers = new HashMap<>();
        public Map<PlayerInstance, Long> HelpTimers = new HashMap<>();
    }

    Map<Integer, PlayerInfo> players = new HashMap<>();

    public void updateAttackTimer(PlayerInstance attacker, PlayerInstance target) {
        synchronized (players) {
            PlayerInfo playerInfo = players.get(target.getObjectId());
            if (playerInfo == null) {
                playerInfo = new PlayerInfo();
                players.put(target.getObjectId(), playerInfo);
            }

            synchronized (playerInfo) {
                long time = System.currentTimeMillis() + 10000L;
                playerInfo.AttackTimers.put(attacker, time);
            }
        }
    }

    public void updateHelpTimer(PlayerInstance helper, PlayerInstance target) {
        synchronized (players) {
            PlayerInfo playerInfo = players.get(target.getObjectId());
            if (playerInfo == null) {
                playerInfo = new PlayerInfo();
                players.put(target.getObjectId(), playerInfo);
            }

            synchronized (playerInfo) {
                long time = System.currentTimeMillis() + 10000L;
                playerInfo.HelpTimers.put(helper, time);
            }
        }
    }

    public List<PlayerInstance> getAssistants(PlayerInstance killer, PlayerInstance victim, boolean killed) {
        long curTime = System.currentTimeMillis();
        Set<PlayerInstance> assistants = new HashSet<>();
        if (killer != null && players.containsKey(killer.getObjectId())) {
            PlayerInfo killerInfo = players.get(killer.getObjectId());

            // Gather the assistants
            List<PlayerInstance> toDeleteList = new ArrayList<>();
            for (PlayerInstance assistant : killerInfo.HelpTimers.keySet()) {
                if (killerInfo.HelpTimers.get(assistant) > curTime) {
                    assistants.add(assistant);
                } else {
                    toDeleteList.add(assistant);
                }
            }

            // Delete unnecessary assistants
            for (PlayerInstance toDelete : toDeleteList) {
                killerInfo.HelpTimers.remove(toDelete);
            }
        }

        if (victim != null && players.containsKey(victim.getObjectId())) {
            PlayerInfo victimInfo = players.get(victim.getObjectId());

            // Gather more assistants
            for (PlayerInstance assistant : victimInfo.AttackTimers.keySet()) {
                if (victimInfo.AttackTimers.get(assistant) > curTime) {
                    assistants.add(assistant);
                    if (players.containsKey(assistant.getObjectId())) {
                        PlayerInfo assistantInfo = players.get(assistant.getObjectId());

                        // Gather the assistant's assistants
                        List<PlayerInstance> toDeleteList = new ArrayList<>();
                        for (Entry<PlayerInstance, Long> assistantsAssistant : assistantInfo.HelpTimers.entrySet()) {
                            if (assistantsAssistant.getValue() > curTime) {
                                assistants.add(assistantsAssistant.getKey());
                            } else {
                                toDeleteList.add(assistantsAssistant.getKey());
                            }
                        }

                        // Delete unnecessary assistants
                        for (PlayerInstance toDelete : toDeleteList) {
                            assistantInfo.HelpTimers.remove(toDelete);
                        }
                    }
                }
            }

            if (killed) {
                victimInfo.AttackTimers.clear();
            }
        }

        assistants.remove(killer);
        assistants.remove(victim);
        return new ArrayList<>(assistants);
    }

    public static PlayerAssistsManager getInstance() {
        return SingletonHolder.instance;
    }

    @SuppressWarnings("synthetic-access")
    private static class SingletonHolder {
        protected static final PlayerAssistsManager instance = new PlayerAssistsManager();
    }
}