package org.l2jmobius.gameserver.events.instanced;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.util.xml.XmlNode;

import java.util.ArrayList;
import java.util.List;

public class EventPrize {
    protected final float chance;
    protected final boolean dependsOnPerformance;

    public EventPrize(XmlNode node) {
        chance = node.getFloat("chance");
        dependsOnPerformance = node.getBool("dependsOnPerformance", false);
    }

    public EventPrizeItem getItem() {
        return null;
    }

    public static class EventPrizeItem extends EventPrize {
        private final int id;
        private final int min;
        private final int max;

        public EventPrizeItem(XmlNode node) {
            super(node);
            id = node.getInt("id");
            min = node.getInt("min");
            max = node.getInt("max");
        }

        public int getId() {
            return id;
        }

        public int getMin() {
            return min;
        }

        public int getMax() {
            return max;
        }

        @Override
        public EventPrizeItem getItem() {
            return this;
        }
    }

    public static class EventPrizeCategory extends EventPrize {
        private final List<EventPrizeItem> items = new ArrayList<>();

        public EventPrizeCategory(XmlNode node) {
            super(node);
            for (XmlNode subNode : node.getChildren()) {
                if (subNode.getName().equalsIgnoreCase("item")) {
                    items.add(new EventPrizeItem(subNode));
                }
            }
        }

        @Override
        public EventPrizeItem getItem() {
            float rnd = Rnd.get(100000) / 1000.0f;
            float percent = 0.0f;
            for (EventPrizeItem item : items) {
                percent += item.getChance();
                if (percent > rnd) {
                    return item;
                }
            }

            return items.get(0);
        }
    }

    public float getChance() {
        return chance;
    }

    public boolean dependsOnPerformance() {
        return dependsOnPerformance;
    }
}
