package org.l2jmobius.gameserver.datatables;


import org.l2jmobius.Config;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.events.instanced.EventPrize;
import org.l2jmobius.gameserver.events.instanced.EventPrize.EventPrizeCategory;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.util.xml.XmlDocument;
import org.l2jmobius.gameserver.util.xml.XmlNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventPrizesTable {
    private static Logger log = LoggerFactory.getLogger(EventPrizesTable.class.getName());

    private final Map<String, List<EventPrize>> prizes = new HashMap<>();

    private EventPrizesTable() {
    }

    public void load() {
        prizes.clear();

        XmlDocument doc = new XmlDocument(new File(Config.DATAPACK_ROOT, "config/eventPrizes.xml"));
        int przCount = 0;


            for (XmlNode prizeNode : doc.getChildren()) {
                if (!prizeNode.getName().equals("prizeList")) {
                    continue;
                }

                String name = prizeNode.getString("name");
                for (XmlNode node : prizeNode.getChildren()) {
                    List<EventPrize> list = prizes.get(name);
                    if (list == null) {
                        list = new ArrayList<>();
                        prizes.put(name, list);
                    }

                    if (node.getName().equalsIgnoreCase("prizeItem")) {
                        list.add(new EventPrize.EventPrizeItem(node));
                        przCount++;
                    } else if (node.getName().equalsIgnoreCase("prizeCategory")) {
                        list.add(new EventPrizeCategory(node));
                        przCount++;
                    }
                }
            }

        log.info("Event Prizes Table: loaded " + przCount + " prizes in " + prizes.size() + " categories.");
    }

    public void rewardPlayer(String prizeName, PlayerInstance player, float teamMultiplier, float performanceMultiplier) {
        List<EventPrize> list = prizes.get(prizeName);
        if (list == null) {
            return;
        }

        for (EventPrize prize : list) {
            float multiplier = teamMultiplier;
            if (prize.dependsOnPerformance()) {
                multiplier *= performanceMultiplier;
            }

            float chance = prize.getChance() * multiplier;
            if (chance < 100.0f) {
                float rnd = Rnd.get(100000) / 1000.0f;
                if (chance < rnd) {
                    continue;
                }

                multiplier = 1.0f;
            } else {
                multiplier = chance / 100.0f;
            }

            while (multiplier > 0) {
                EventPrize.EventPrizeItem prizeItem = prize.getItem();
                float mul = 1.0f;
                if (multiplier < 1.0f) {
                    mul = multiplier;
                }
                int prizeCount = Math.round(Rnd.get(prizeItem.getMin(), prizeItem.getMax()) * mul);
                if (prizeCount > 0) {
                    player.addItem("Event", prizeItem.getId(), prizeCount, player, true);
                }

                multiplier -= 1.0f;
            }
        }
    }

    private static EventPrizesTable instance;

    public static EventPrizesTable getInstance() {
        if (instance == null) {
            instance = new EventPrizesTable();
        }

        return instance;
    }
}