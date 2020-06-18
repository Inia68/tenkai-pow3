package org.l2jmobius.gameserver.events.instanced;

import org.l2jmobius.commons.concurrent.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.instance.PetInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.skills.AbnormalVisualEffect;
import org.l2jmobius.gameserver.util.Point3D;
import org.l2jmobius.Config;
import org.l2jmobius.gameserver.events.instanced.EventInstance.EventState;
import org.l2jmobius.gameserver.events.instanced.EventInstance.EventType;

public class EventTeleporter implements Runnable {
    private PlayerInstance playerInstance = null;
    private Point3D coordinates = null;
    private boolean restore = false;
    private boolean heal = true;

    public EventTeleporter(PlayerInstance playerInstance, Point3D coordinates, boolean fastSchedule, boolean restore) {
        this.playerInstance = playerInstance;
        this.coordinates = coordinates;
        this.restore = restore;

        long delay = (playerInstance.getEvent() == null || playerInstance.getEvent().isState(EventState.STARTED) ?
                Config.INSTANCED_EVENT_RESPAWN_TELEPORT_DELAY  :  Config.INSTANCED_EVENT_START_LEAVE_TELEPORT_DELAY ) * 1000;

        ThreadPool.schedule(this, fastSchedule ? 0 : delay);
    }

    public EventTeleporter(PlayerInstance playerInstance, Point3D coordinates, boolean fastSchedule, boolean restore, boolean heal) {
        this.playerInstance = playerInstance;
        this.coordinates = coordinates;
        this.restore = restore;
        this.heal = heal;

        long delay = (playerInstance.getEvent() == null || playerInstance.getEvent().isState(EventState.STARTED) ?
                 Config.INSTANCED_EVENT_RESPAWN_TELEPORT_DELAY : Config.INSTANCED_EVENT_START_LEAVE_TELEPORT_DELAY) * 1000;

        ThreadPool.schedule(this, fastSchedule ? 0 : delay);
    }

    @Override
    public void run() {
        if (playerInstance == null) {
            return;
        }

        EventInstance event = playerInstance.getEvent();
        if (event == null) {
            return;
        }

        try {
            playerInstance.stopAllEffects();

            PetInstance pet = playerInstance.getPet();
            if (pet != null) {
                // In LC, SS and SS2, players don't need summons and summons are even able to attack during event so better unsummon
                if (pet.isMountable() || event.isType(EventType.LuckyChests) || event.isType(EventType.StalkedSalkers) ||
                        event.isType(EventType.SimonSays)) {
                    pet.unSummon(playerInstance);
                } else {
                    pet.stopAllEffects();
                }
            }

            if (event.getConfig().isAllVsAll()) {
                playerInstance.leaveParty();
            }

            for (Summon summon : playerInstance.getServitorsAndPets()) {
                // In LC, SS and SS2, players don't need summons and summons are even able to attack during event so better unsummon
                if (event.isType(EventType.LuckyChests) || event.isType(EventType.StalkedSalkers) || event.isType(EventType.SimonSays)) {
                    summon.unSummon(playerInstance);
                } else {
                    summon.stopAllEffects();
                }
            }

            if (playerInstance.isDead()) {
                playerInstance.restoreExp(100.0);
                playerInstance.doRevive();
            }

            if (heal) {
                playerInstance.setCurrentCp(playerInstance.getMaxCp());
                playerInstance.setCurrentHp(playerInstance.getMaxHp());
                playerInstance.setCurrentMp(playerInstance.getMaxMp());
            }

            int x = 0, y = 0, z = 0;
            if (event.isState(EventState.STARTED) && !restore) {
                playerInstance.setInstanceId(event.getInstanceId());
                if (event.getConfig().spawnsPlayersRandomly()) {
                    EventLocation location = event.getConfig().getLocation();
                    Location pos = location.getZone().getZone().getRandomPoint();
                    x = pos.getX();
                    y = pos.getY();
                    z = GeoEngine.getInstance().getHeight(pos.getX(), pos.getY(), pos.getZ());
                } else {
                    float r1 = Rnd.get(1000);
                    int r2 = Rnd.get(100);
                    x = Math.round((float) Math.cos(r1 / 1000 * 2 * Math.PI) * r2 + coordinates.getX());
                    y = Math.round((float) Math.sin(r1 / 1000 * 2 * Math.PI) * r2 + coordinates.getY());
                    z = GeoEngine.getInstance().getHeight(x, y, coordinates.getZ());
                }
            } else {
                playerInstance.setInstanceId(0);
                playerInstance.setEvent(null);
                playerInstance.returnedFromEvent();
                if (playerInstance.getEventSavedPosition().getX() == 0 && playerInstance.getEventSavedPosition().getY() == 0 &&
                        playerInstance.getEventSavedPosition().getZ() == 0) {
                    x = coordinates.getX();
                    y = coordinates.getY();
                    z = GeoEngine.getInstance().getHeight(coordinates.getX(), coordinates.getY(), coordinates.getZ());
                } else {
                    x = playerInstance.getEventSavedPosition().getX();
                    y = playerInstance.getEventSavedPosition().getY();
                    z = playerInstance.getEventSavedPosition().getZ();
                }
            }
            playerInstance.teleToLocation(x, y, z);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
