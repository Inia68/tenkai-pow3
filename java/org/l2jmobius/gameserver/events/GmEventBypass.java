package org.l2jmobius.gameserver.events;

import org.l2jmobius.gameserver.events.CreatureInvasion.CreatureInvasion;
import org.l2jmobius.gameserver.events.instanced.EventsManager;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;

import java.util.StringTokenizer;

public class GmEventBypass {

    private static GmEventBypass instance = null;


    public void handleBypass(PlayerInstance activeChar, String command) {
        if (activeChar == null) {
            return;
        }
        final StringTokenizer st = new StringTokenizer(command, " ");
        st.nextToken();
        String eventName = "";
        String eventAction = "";

        if (st.hasMoreTokens()) {
            eventName = st.nextToken();
        }
        if (st.hasMoreTokens()) {
            eventAction = st.nextToken();
        }

        switch (eventName) {
            case "creature_invasion": {
                CreatureInvasion.getInstance().onAdvEvent(eventAction, null, activeChar);
                break;
            }
            case "piggy_raidboss": {
                switch (eventAction) {
                    case "teleport_to_fantasy": {
                        activeChar.teleToLocation(-51624, -68940, -3418);
                        break;
                    }
                }
            }
        }

    }


    public static GmEventBypass getInstance() {
        if (instance == null) {
            instance = new GmEventBypass();
        }
        return instance;
    }
}
