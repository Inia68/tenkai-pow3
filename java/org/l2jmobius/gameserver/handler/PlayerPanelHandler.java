package org.l2jmobius.gameserver.handler;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.events.instanced.EventInstance;
import org.l2jmobius.gameserver.events.instanced.EventsManager;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;

import java.util.StringTokenizer;

public class PlayerPanelHandler {
    private static PlayerPanelHandler instance;

    public String getMainPage(PlayerInstance player) {
        String result = null;
        result = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/panel/main.html");
        final String navigation = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/navigation.html");
        result = result.replace("%navigation%", navigation);
        result = result.replace("%switchArmorGlowButton%",
                "<button value=\"Armor Glow " + (player.getIsArmorGlowDisabled() == true ? "Disabled" : "Enabled") +"  \" action=\"bypass -h PlayerPanel armorglow\" width=255 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");


        return result;
    }

    public void handleBypass(PlayerInstance activeChar, String command) {
        if (activeChar == null) {
            return;
        }

        final StringTokenizer st = new StringTokenizer(command, " ");
        final String Param1 = st.nextToken();
        final String param2 = st.nextToken();

        if (param2.startsWith("armorglow")) {
            switchArmorGlow(activeChar);
        }
    }

    private void switchArmorGlow(PlayerInstance player) {
        player.setIsArmorGlowDisabled(!player.getIsArmorGlowDisabled());
        player.broadcastUserInfo();
        player.updateAbnormalVisualEffects();
        CommunityBoardHandler.separateAndSend(getMainPage(player), player);
    }


    public static PlayerPanelHandler getInstance() {
        if (instance == null) {
            instance = new PlayerPanelHandler();
        }
        return instance;
    }

}
