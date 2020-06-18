package org.l2jmobius.gameserver.communitybbs;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.data.xml.impl.NpcData;
import org.l2jmobius.gameserver.datatables.ItemTable;
import org.l2jmobius.gameserver.engines.items.ItemDataHolder;
import org.l2jmobius.gameserver.enums.DropType;
import org.l2jmobius.gameserver.events.instanced.EventInstance;
import org.l2jmobius.gameserver.events.instanced.EventsManager;
import org.l2jmobius.gameserver.instancemanager.FarmZoneManager;
import org.l2jmobius.gameserver.model.ItemInfo;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.holders.DropHolder;
import org.l2jmobius.gameserver.model.items.Item;

import java.util.List;
import java.util.Map;

public class FarmZones {
    public static FarmZones instance = null;
    Map<String, FarmZoneManager.FarmZone> farmZones = null;

    FarmZones() {
        farmZones = FarmZoneManager.getInstance().getFarmZones();
    }

    String capitalize(String str) {
        String str2 =  str.replaceAll("_", " ");
        String words[]=str2.split("\\s");
        String capitalizeWord="";
        for(String w:words){
            String first=w.substring(0,1);
            String afterfirst=w.substring(1);
            capitalizeWord+=first.toUpperCase()+afterfirst+" ";
        }
        return capitalizeWord.trim();
    }

    public String getFarmZonePage(String command, PlayerInstance player) {

        final String[] cmd = command.split(";");

        if (cmd.length == 3) {
            return getFarmZoneDetail(cmd[1], Integer.parseInt(cmd[2]), player);
        } else if (cmd.length == 2) {
            return getFarmZoneMob(cmd[1], player);
        }


        String result = "";
        result += "<html><body>";
        for (Map.Entry<String, FarmZoneManager.FarmZone> entry : farmZones.entrySet()) {
            String name = entry.getKey();
            FarmZoneManager.FarmZone farmzone = entry.getValue();
            if (name == null || farmzone == null || !farmzone.getShowInCb()) {
                continue;
            }
            result += "<button action=\"bypass _bbsfarmzone;" + name + "\" align=left icon=teleport>" + capitalize(farmzone.getName()) + "</button>";
        }
        result += "</body></html>";


        String res = HtmCache.getInstance().getHtm(null, "data/html/CommunityBoard/Custom/farm/farmZone.htm");
        final String navigation = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/navigation.html");
        res = res.replace("%navigation%", navigation);
        res = res.replace("%farmZones%", result);

        return res;
    }

    public String getFarmZoneMob(String name, PlayerInstance player) {
        String result = "";

        FarmZoneManager.FarmZone farmZone = farmZones.get(name);

        if (farmZone == null) {
            return "";
        }
        result += "<html><body>";


        result += "<button action=\"bypass _bbsfarmzone;\" align=left icon=teleport>Back</button>";
        for (NpcTemplate mob : farmZone.getMobs()) {
            result += "<button action=\"bypass _bbsfarmzone;" + name + ";" + mob.getId() + "\" align=left icon=teleport>" + mob.getName() + "</button>";

        }

        result += "</body></html>";

        String res = HtmCache.getInstance().getHtm(null, "data/html/CommunityBoard/Custom/farm/farmZone.htm");
        final String navigation = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/navigation.html");
        res = res.replace("%navigation%", navigation);
        res = res.replace("%farmZones%", result);

        return res;
    }


    public String getFarmZoneDetail(String name, int mobId, PlayerInstance player) {
        String result = "";
        NpcTemplate mob = NpcData.getInstance().getTemplate(mobId);

        if (mob == null) {
            return "";
        }
        result += "<html><body>";

        result += "<button value=\"Back\" action=\"bypass _bbsfarmzone;" + name + "\" width=40 height=21 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">";
        result += "<table width=270 >\n";
        result += "<tr><td><table width=270 border=0 bgcolor=131210><tr><td align=center width=170>" + mob.getName() + "</td></tr></table></td></tr>\n";
        for (DropHolder drop : mob.getDropList(DropType.DROP)) {
            Item item = ItemTable.getInstance().getTemplate(drop.getItemId());
            if (item == null) {
                continue;
            }
            result += "<tr><td><table width=270 border=0 bgcolor=131210><tr><td width=100><img src=\"" + item.getIcon() + "\" width=32 height=32></td><td align=center width=170>" + item.getName() + "</td></tr></table></td></tr>\n";
            result += "<tr><td><table width=270 border=0 bgcolor=131210><tr><td width=100><font color=\"LEVEL\">Chance:</font></td><td align=right width=170>" + drop.getChance() + "</td></tr></table></td></tr>\n";
            result += "<tr><td><table width=270 border=0 bgcolor=131210><tr><td width=100><font color=\"LEVEL\">Count:</font></td><td align=right width=170>" + (drop.getItemId() == 57 ? drop.getMin() * Config.RATE_DROP_AMOUNT_BY_ID.get(57) : drop.getMin()) + " - " + (drop.getItemId() == 57 ? drop.getMax() * Config.RATE_DROP_AMOUNT_BY_ID.get(57) : drop.getMax()) + "</td></tr></table></td></tr>\n";

            result += "<tr height=50><td height=50></td></tr>";
        }
        result += "</table>";
        result += "</body></html>";

        String res = HtmCache.getInstance().getHtm(null, "data/html/CommunityBoard/Custom/farm/farmZone.htm");
        final String navigation = HtmCache.getInstance().getHtm(player, "data/html/CommunityBoard/Custom/navigation.html");
        res = res.replace("%navigation%", navigation);
        res = res.replace("%farmZones%", result);

        return res;
    }

    public static FarmZones getInstance() {
        if (instance == null) {
            instance = new FarmZones();
        }
        return instance;
    }

}
