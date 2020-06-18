package handlers.admincommandhandlers;


import ai.bosses.Piggy.Piggy;
import org.l2jmobius.gameserver.events.trivial.Trivial;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.events.CreatureInvasion.CreatureInvasion;
import org.l2jmobius.gameserver.events.ElpiesArena.ElpiesArena;

import java.util.StringTokenizer;

public class AdminEvent implements IAdminCommandHandler {
    private static final String[] ADMIN_COMMANDS =
            {
                    "admin_gm_event",
            };

    @Override
    public String[] getAdminCommandList() {
        return ADMIN_COMMANDS;
    }

    @Override
    public boolean useAdminCommand(String command, PlayerInstance activeChar) {
        if (activeChar == null) {
            return false;
        }

        String eventCommand = "";
        String eventName = "";
        final StringTokenizer st = new StringTokenizer(command, ";");
        st.nextToken();
        if (st.hasMoreTokens()) {
            eventCommand = st.nextToken();
        }
        if (st.hasMoreTokens()) {
            eventName = st.nextToken();
        }

        if (command.startsWith("admin_gm_event")) {
            if (eventCommand.equalsIgnoreCase("start")) {
                if (eventName == null) {
                    return false;
                }
                switch (eventName) {
                    case "creature_invasion":
                        CreatureInvasion.getInstance().StartInvasion();
                        break;
                    case "elpies_arena":
                        ElpiesArena.getInstance().openRegistration();
                        break;
                    case "spawn_piggy":
                        ai.bosses.Piggy.Piggy piggy = new Piggy();
                        break;
                    case "trivial":
                        if (st.hasMoreTokens()) {
                            Boolean answer = Boolean.parseBoolean(st.nextToken());
                            String question = st.nextToken();
                            Trivial.getInstance().ask(question, answer);
                            return true;
                        }
                        Trivial.getInstance().openRegistrations(activeChar);
                        break;
                }
            } else if (eventCommand == "") {
                showMenu(activeChar);
            }
        }
        return false;
    }

    private void showMenu(PlayerInstance activeChar) {
        final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
        html.setFile(activeChar, "data/html/admin/gm_event.htm");
        activeChar.sendPacket(html);
    }
}