package org.l2jmobius.gameserver.model.zone.type;

import org.l2jmobius.commons.concurrent.ThreadPool;
import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.events.instanced.EventInstance;
import org.l2jmobius.gameserver.events.instanced.EventInstance.EventState;
import org.l2jmobius.gameserver.events.instanced.EventInstance.EventType;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.network.clientpackets.Say2;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;

/**
 * @author Pere
 */
public class TenkaiEventZone extends ZoneType {
    public static int BASE_ID = 80000;

    public TenkaiEventZone(int id) {
        super(id);
    }

    @Override
    protected void onEnter(Creature character) {
    }

    @Override
    protected void onExit(Creature character) {
        if (character instanceof PlayerInstance) {
            PlayerInstance player = (PlayerInstance) character;
            EventInstance event = player.getEvent();
            if (event != null && event.getConfig().needsClosedArena() && player.getEvent().isState(EventState.STARTED) &&
                    event.getConfig().getLocation().getId() == getId() - BASE_ID &&
                    (!event.isType(EventType.VIP) || event.getParticipantTeam(player.getObjectId()).getVIP() == player) &&
                    (!event.isType(EventType.CaptureTheFlag) || player.getCtfFlag() != null)) {
                ThreadPool.execute(new OutOfEventZoneTask(player));
            }
        }
    }

    @Override
    public void onDieInside(Creature character, Creature killer) {
    }

    @Override
    public void onReviveInside(Creature character) {
    }

    class OutOfEventZoneTask implements Runnable {
        private PlayerInstance player;
        private int delay = 10;
        private boolean warned = false;

        public OutOfEventZoneTask(PlayerInstance player) {
            this.player = player;
        }

        @Override
        public void run() {
            if (!isInsideZone(player) && player.isPlayingEvent()) {
                if (getDistanceToZone(player) > 500 || getZone().getHighZ() < player.getZ() || getZone().getLowZ() > player.getZ()) {
                    if (delay > 0) {
                        if (!warned) {
                            player.sendPacket(new CreatureSay(player,
                                    ChatType.NPC_WHISPER,
                                    "Instanced Events",
                                    "You left the event zone. If you don't return in 10 seconds your character will die!"));
                            warned = true;
                        } else if (delay <= 5) {
                            player.sendPacket(new CreatureSay(player, ChatType.NPC_WHISPER, "Instanced Events", delay + " seconds to return."));
                        }

                        delay--;
                        ThreadPool.schedule(this, 1000L);
                    } else {
                        if (player.getEvent().isType(EventType.VIP)) {
                            player.getEvent().getParticipantTeam(player.getObjectId()).decreasePoints();
                        }
                        player.doDie(player);
                    }
                } else {
                    delay = 10;
                    ThreadPool.schedule(this, 1000L);
                }
            }
        }
    }
}