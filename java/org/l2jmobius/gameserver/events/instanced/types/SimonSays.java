package org.l2jmobius.gameserver.events.instanced.types;

import org.l2jmobius.commons.concurrent.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.enums.ChatType;
import org.l2jmobius.gameserver.events.instanced.EventConfig;
import org.l2jmobius.gameserver.events.instanced.EventInstance;
import org.l2jmobius.gameserver.events.instanced.EventTeleporter;
import org.l2jmobius.gameserver.events.instanced.EventsManager;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.clientpackets.Say2;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.util.Broadcast;
import org.l2jmobius.gameserver.util.Point3D;

import java.util.ArrayList;
import java.util.List;

public class SimonSays extends EventInstance {

    private boolean someoneFailed = false;
    private int currentSocialActionId = 2;
    private SimonSaysTask simonSaysTask;
    private ArrayList<Integer> actedPlayers = new ArrayList<>();

    List<PlayerInstance> winners = new ArrayList<>();

    public SimonSays(int id, EventConfig config) {
        super(id, config);
    }

    @Override
    public boolean startFight() {
        if (!super.startFight()) {
            return false;
        }

        startSimonSaysTask();

        return true;
    }

    @Override
    public void calculateRewards() {
        PlayerInstance winner = null;
        if (teams[0].getParticipatedPlayerCount() != 1) {
            Broadcast.toAllOnlinePlayers("The event has ended in a tie");
            return;
        }

        for (PlayerInstance playerInstance : teams[0].getParticipatedPlayers().values()) {
            winner = playerInstance;
        }

        if (winner != null) {
            winners.add(0, winner);
        }

        if (!winners.isEmpty()) {
            rewardPlayers(winners);
            Broadcast.toAllOnlinePlayers("The event has ended. The player " + winners.get(0).getName() + " has won being the last one standing!");
        } else {
            Broadcast.toAllOnlinePlayers("The event has ended in a tie due to the fact there wasn't anyone left");
        }
    }

    @Override
    public String getRunningInfo(PlayerInstance player) {
        String html = "";
        if (teams[0].getParticipatedPlayerCount() > 0) {
            html += "Players staying:<br>";
            for (PlayerInstance participant : teams[0].getParticipatedPlayers().values()) {
                if (participant != null) {
                    html += EventsManager.getInstance().getPlayerString(participant, player) + ", ";
                }
            }
            html = html.substring(0, html.length() - 2) + ".";
        }
        return html;
    }

    @Override
    public boolean onAction(PlayerInstance playerInstance, int targetedPlayerObjectId) {
        return false;
    }

    @Override
    public void onKill(Creature killerCharacter, PlayerInstance killedPlayerInstance) {
        if (killedPlayerInstance == null || !isState(EventState.STARTED)) {
            return;
        }

        new EventTeleporter(killedPlayerInstance, teams[0].getCoords(), false, false);
    }

    public void onSocialAction(PlayerInstance player, int actionId) {
        if (simonSaysTask.isWaiting() || !isPlayerParticipant(player.getObjectId())) {
            return;
        }

        if (actionId == currentSocialActionId) {
            actedPlayers.add(player.getObjectId());
            player.sendPacket(new CreatureSay(player, ChatType.SHOUT, "Instanced Events", "Ok!"));

            player.addEventPoints(winners.size() + teams[0].getParticipatedPlayers().size() - actedPlayers.size());
        } else {
            someoneFailed = true;
            player.sendPacket(new CreatureSay(player, ChatType.SHOUT, "Instanced Events", "Ooh, error! You have been disqualified."));
            removeParticipant(player.getObjectId());
            winners.add(0, player);
            new EventTeleporter(player, new Point3D(0, 0, 0), false, true);
        }
    }

    public void simonSays() {
        currentSocialActionId = Rnd.get(16) + 2;
        if (currentSocialActionId > 15) {
            currentSocialActionId += 12;
        }

        CreatureSay cs = new CreatureSay(null, ChatType.BATTLEFIELD, "Simon", getActionString(currentSocialActionId));
        for (PlayerInstance playerInstance : teams[0].getParticipatedPlayers().values()) {
            if (playerInstance != null) {
                playerInstance.sendPacket(cs);
            }
        }
    }

    private String getActionString(int actionId) {
        String actionString;
        switch (actionId) {
            case 2:
                actionString = "Greet!";
                break;
            case 3:
                actionString = "Victory!";
                break;
            case 4:
                actionString = "Advance!";
                break;
            case 5:
                actionString = "No!";
                break;
            case 6:
                actionString = "Yes!";
                break;
            case 7:
                actionString = "Bow!";
                break;
            case 8:
                actionString = "Unaware!";
                break;
            case 9:
                actionString = "Waiting!";
                break;
            case 10:
                actionString = "Laugh!";
                break;
            case 11:
                actionString = "Applaud!";
                break;
            case 12:
                actionString = "Dance!";
                break;
            case 13:
                actionString = "Sorrow!";
                break;
            case 14:
                actionString = "Charm!";
                break;
            case 15:
                actionString = "Shyness!";
                break;
            case 28:
                actionString = "Propose!";
                break;
            default:
                actionString = "Provoke!";
                break;
        }
        return actionString;
    }

    public void endSimonSaysRound() {
        CreatureSay cs = new CreatureSay(null, ChatType.SHOUT, "Instanced Events", "Nice! You passed this round!");
        CreatureSay cs2 = new CreatureSay(null, ChatType.SHOUT, "Instanced Events", "You have been disqualified for being the last player acting!");
        CreatureSay cs3 = new CreatureSay(null, ChatType.SHOUT, "Instanced Events", "You have been disqualified for not doing anything!");

        List<PlayerInstance> participants = new ArrayList<>(teams[0].getParticipatedPlayers().values());
        for (PlayerInstance playerInstance : participants) {
            if (playerInstance != null && !actedPlayers.contains(playerInstance.getObjectId())) {
                someoneFailed = true;
                removeParticipant(playerInstance.getObjectId());
                new EventTeleporter(playerInstance, new Point3D(0, 0, 0), false, true);
                playerInstance.sendPacket(cs3);
            }
        }

        if (!someoneFailed && getParticipatedPlayersCount() > 1) {
            PlayerInstance player = teams[0].getParticipatedPlayers().get(actedPlayers.get(actedPlayers.size() - 1));
            if (player != null) {
                removeParticipant(player.getObjectId());
                new EventTeleporter(player, new Point3D(0, 0, 0), false, true);
                winners.add(0, player);
                player.sendPacket(cs2);
            }
        }

        for (PlayerInstance playerInstance : teams[0].getParticipatedPlayers().values()) {
            if (playerInstance != null) {
                playerInstance.sendPacket(cs);
            }
        }

        if (getParticipatedPlayersCount() <= 1) {
            stopFight();
        }

        someoneFailed = false;
        actedPlayers.clear();
    }

    class SimonSaysTask implements Runnable {
        boolean stop = false;
        boolean waiting = true;

        @Override
        public void run() {
            if (!stop && isState(EventState.STARTED)) {
                int delay;
                if (waiting) {
                    simonSays();
                    delay = 15000;
                } else {
                    endSimonSaysRound();
                    delay = 5000;
                }
                waiting = !waiting;

                ThreadPool.schedule(this, delay);
            }
        }

        public void stop() {
            stop = true;
        }

        public boolean isStopped() {
            return stop;
        }

        public boolean isWaiting() {
            return waiting;
        }
    }

    public void startSimonSaysTask() {
        if (simonSaysTask != null && !simonSaysTask.isStopped()) {
            simonSaysTask.stop();
        }
        simonSaysTask = new SimonSaysTask();
        ThreadPool.schedule(simonSaysTask, 30000);
    }
}
