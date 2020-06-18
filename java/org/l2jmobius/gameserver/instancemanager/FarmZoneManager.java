package org.l2jmobius.gameserver.instancemanager;

import org.l2jmobius.Config;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.impl.NpcData;
import org.l2jmobius.gameserver.datatables.ItemTable;
import org.l2jmobius.gameserver.enums.DropType;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.MonsterInstance;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.holders.DropHolder;
import org.l2jmobius.gameserver.model.items.Item;
import org.l2jmobius.gameserver.util.xml.XmlDocument;
import org.l2jmobius.gameserver.util.xml.XmlNode;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

public class FarmZoneManager {
    protected static final Logger LOGGER = Logger.getLogger(NpcData.class.getName());

    public class FarmZone {
        private String _name;
        private Set<NpcTemplate> _mobs = new HashSet<>();
        private boolean _showInCb;

        public FarmZone(String name) {
            _name = name;
            _showInCb = false;
        }

        public void showInCb(boolean value) { _showInCb = value; }

        public void addMob(NpcTemplate mob) {
            _mobs.add(mob);
        }

        public String getName() {
            return _name;
        }

        public Set<NpcTemplate> getMobs() {
            return _mobs;
        }

        public boolean getShowInCb() { return _showInCb; }
    }

    private Map<String, FarmZone> _farmZones = new HashMap<>();

    private static FarmZoneManager _instance;

    public double randomBetween(double low, double high) {
        Random r = new Random();
        return  low + (high - low) * r.nextDouble();
    }

    public Map<String, FarmZone> getFarmZones() {
        return _farmZones;
    }

    public static FarmZoneManager getInstance() {
        return _instance == null ? (_instance = new FarmZoneManager()) : _instance;
    }

    private FarmZoneManager() {

        load();
    }

    private void load() {
        File file = new File(Config.DATAPACK_ROOT, "/data/farm_zones/farmZone.xml");
        XmlDocument doc = new XmlDocument(file);
        for (XmlNode farmNode : doc.getChildren()) {
            if (farmNode.getName().equalsIgnoreCase("farmZone")) {
                String name = farmNode.getString("name");
                boolean showInCb = farmNode.getBool("showInCb", false);
                FarmZone farmZone = new FarmZone(name);
                if (showInCb) { farmZone.showInCb(true); }
                for (XmlNode mobNode : farmNode.getChildren()) {
                    if (mobNode.getName().equalsIgnoreCase("mob")) {
                        int mobId = mobNode.getInt("id");
                        NpcTemplate temp = NpcData.getInstance().getTemplate(mobId);
                        if (temp == null) {
                            continue;
                        }
                        farmZone.addMob(temp);
                    }
                }

                _farmZones.put(name, farmZone);
            }
        }
        LOGGER.info("Farm Zone Manager: loaded " + _farmZones.size() + " farm zone definitions.");


        List<DropHolder> eventDrops = new ArrayList<>();
        file = new File(Config.DATAPACK_ROOT, "/data/farm_zones/event.xml");
        doc = new XmlDocument(file);
        for (XmlNode eventNode : doc.getChildren()) {
            if (eventNode.getName().equalsIgnoreCase("event") && eventNode.getBool("active")) {
                String name = eventNode.getString("name");
                for (XmlNode dropNode : eventNode.getChildren()) {
                    if (dropNode.getName().equalsIgnoreCase("itemDrop")) {
                        int itemId = dropNode.getInt("itemId");
                        int min = dropNode.getInt("min");
                        int max = dropNode.getInt("max");
                        float chance = dropNode.getFloat("chance");

                        DropHolder dd = new DropHolder(DropType.DROP, itemId, min, max, chance);

                        Item item = ItemTable.getInstance().getTemplate(dd.getItemId());
                        if (item == null) {
                            LOGGER.warning("Drop data for undefined item template in custom drop definitions!");
                            continue;
                        }
                        if (dropNode.getName().equalsIgnoreCase("itemDrop")) {
                            eventDrops.add(dd);
                        }
                    }
                }
            }
        }
        LOGGER.info("Farm Zone Manager: loaded " + _farmZones.size() + " farm zone definitions.");

        file = new File(Config.DATAPACK_ROOT, "data/farm_zones/customFarmZone.xml");
        if (!file.exists()) {
            return;
        }

        doc = new XmlDocument(file);
        int customized = 0;
        for (XmlNode farmNode : doc.getChildren()) {
            if (farmNode.getName().equalsIgnoreCase("excludeDrop")) {
                int itemId = farmNode.getInt("itemId");
                for (NpcTemplate npc : NpcData.getInstance().getAllTemplates()) {
                    List<DropHolder> dropsToRemove = new ArrayList<>();
                    for (DropHolder dd : npc.getDropList(DropType.DROP)) {
                        if (dd.getItemId() == itemId) {
                            dropsToRemove.add(dd);
                        }
                    }

                    for (DropHolder dd : dropsToRemove)
                    {
                        npc.getDropList(DropType.DROP).remove(dd);
                    }

                }
            } else if (farmNode.getName().equalsIgnoreCase("customFarm")) {
                Set<NpcTemplate> mobs = new HashSet<>();
                if (farmNode.hasAttribute("farmZone")) {
                    String name = farmNode.getString("farmZone");
                    FarmZone farmZone = _farmZones.get(name);
                    for (NpcTemplate mob : farmZone.getMobs()) {
                        mobs.add(mob);
                    }
                } else if (farmNode.hasAttribute("levelRange")) {
                    String[] levelRange = farmNode.getString("levelRange").split("-");
                    int minLvl = Integer.parseInt(levelRange[0]);
                    int maxLvl = Integer.parseInt(levelRange[1]);
                    for (NpcTemplate mob : NpcData.getInstance().getAllTemplates()) {
                        if (mob.getLevel() >= minLvl && mob.getLevel() <= maxLvl) {
                            mobs.add(mob);
                        }
                    }
                } else {
                    LOGGER.warning("There's a farm customization without any monster group specified!");
                    continue;
                }

                float hpMultiplier = farmNode.getFloat("hpMultiplier", 1.0f);
                float atkMultiplier = farmNode.getFloat("atkMultiplier", 1.0f);
                float defMultiplier = farmNode.getFloat("defMultiplier", 1.0f);
                float mdefMultiplier = farmNode.getFloat("mdefMultiplier", 1.0f);
                int level = farmNode.getInt("overrideLevels", 0);
                boolean overrideDrops = farmNode.getBool("overrideDrops", false);
                float expMultiplier = farmNode.getFloat("expMultiplier", 1.0f);
                boolean includeEvent = farmNode.getBool("includeEvent", false);
                long adenaMin = farmNode.getLong("adenaMin", 0);
                long adenaMax = farmNode.getLong("adenaMax", adenaMin);
                double adenaChance = farmNode.getDouble("adenaChance", 100);
                float randomStatPercent = farmNode.getFloat("randomStatPercent", 0.0f);

                float baseMobFarmCost = 0.0f;
                if (farmNode.hasAttribute("adjustDropsPerMob")) {
                    int baseMobId = farmNode.getInt("adjustDropsPerMob");
                    NpcTemplate baseMobTemplate = NpcData.getInstance().getTemplate(baseMobId);
                    if (baseMobTemplate != null) {
                        try {
                            Spawn spawn = new Spawn(baseMobTemplate);
                            spawn.setXYZ(100, 100, 100);
                            spawn.setAmount(1);
                            spawn.setHeading(100);
                            spawn.doSpawn();
                            Npc baseMob = spawn.getSpawnedNpcs().getFirst();
                            baseMobFarmCost = baseMob.getMaxHp() *
                                    (float) (baseMob.getPDef() + baseMob.getMDef());
                            baseMob.deleteMe();
                            if (baseMob.getSpawn() != null) {
                                baseMob.getSpawn().stopRespawn();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                List<DropHolder> drops = new ArrayList<>();
                List<DropHolder> spoilDrops = new ArrayList<>();
                for (XmlNode dropNode : farmNode.getChildren()) {
                    if (dropNode.getName().equalsIgnoreCase("itemDrop") ||
                            dropNode.getName().equalsIgnoreCase("spoilDrop")) {
                        int itemId = dropNode.getInt("itemId");
                        int min = dropNode.getInt("min");
                        int max = dropNode.getInt("max");
                        float chance = dropNode.getFloat("chance");

                        DropHolder dd = new DropHolder(DropType.DROP, itemId, min, max, chance);

                        Item item = ItemTable.getInstance().getTemplate(dd.getItemId());
                        if (item == null) {
                            LOGGER.warning("Drop data for undefined item template in custom drop definitions!");
                            continue;
                        }

                        if (dropNode.getName().equalsIgnoreCase("itemDrop")) {
                            drops.add(dd);
                        } else {
                            spoilDrops.add(dd);
                        }
                    }
                }

                if (includeEvent && eventDrops.size() > 0) {
                    eventDrops.forEach(dropHolder -> {
                        drops.add(dropHolder);
                    });
                }



                for (NpcTemplate mob : mobs) {


                    float dropMultiplier = 1.0f;
                    if (baseMobFarmCost > 0.0f) {
                        try {
                            Spawn spawn = new Spawn(mob);
                            spawn.setXYZ(100, 100, 100);
                            spawn.setAmount(1);
                            spawn.setHeading(100);
                               spawn.doSpawn();
                            Npc mobInstance = spawn.getLastSpawn();
                            float mobFarmCost = mobInstance.getMaxHp() *
                                    (float) (mobInstance.getPDef() + mobInstance.getMDef());
                            dropMultiplier = mobFarmCost / baseMobFarmCost;
                            mobInstance.deleteMe();
                            if (mobInstance.getSpawn() != null) {
                                mobInstance.getSpawn().stopRespawn();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }



                    mob.setBaseHpMax(mob.getBaseHpMax() * hpMultiplier);
                    mob.setBasePAtk(mob.getBasePAtk() * atkMultiplier);
                    mob.setBasePDef(mob.getBasePDef() * defMultiplier);
                    mob.setBaseMDef(mob.getBaseMDef() * mdefMultiplier);


                    // mob.RewardExp = (long) (mob.RewardExp * expMultiplier);

                    if (level > 0) {
                        // mob.getLevel() = (byte) level;
                    }

                    if (randomStatPercent > 0.0f) {
                        mob.randomStat = randomStatPercent;
                    }

                    if (overrideDrops) {
                        mob.getDropList(DropType.DROP).clear();
                    }

                    if (adenaMin > 0) {
                        DropHolder dd = new DropHolder(DropType.DROP, 57,
                                (long)(Math.round(adenaMin * dropMultiplier) / Config.RATE_DROP_AMOUNT_BY_ID.get(57)), (long)(Math.round(adenaMax * dropMultiplier) / Config.RATE_DROP_AMOUNT_BY_ID.get(57)), adenaChance);
                        mob.addDrop(dd);
                    }

                    if (mob != null && mob.getDropList(DropType.DROP) != null) {
                        for (DropHolder drop : mob.getDropList(DropType.DROP)) {
                            if (drop.getItemId() == 57) {
                                DropHolder adena = new DropHolder(DropType.DROP, 57, drop.getMin(), drop.getMax(), 100);
                                mob.getDropList(DropType.DROP).remove(drop);
                                mob.addDrop(adena);
                            }
                        }
                    }

                    for (DropHolder drop : drops) {
                        long min = drop.getMin();
                        long max = drop.getMax();
                        double chance = drop.getChance() * dropMultiplier;
                        if (min >= 10 && chance > 100.0f ||
                                min * dropMultiplier >= 5 && chance / dropMultiplier <= 100.01f) {
                            min = Math.round(min * dropMultiplier);
                            max = Math.round(max * dropMultiplier);
                            chance /= dropMultiplier;
                        }

                        while (chance > 100.01f) {
                            min *= 2;
                            max *= 2;
                            chance /= 2.0f;
                        }

                        while (chance < 50.0f && min > 1) {
                            min /= 2;
                            max /= 2;
                            chance *= 2.0f;
                        }

                        mob.addDrop(new DropHolder(DropType.DROP, drop.getItemId(), min, max, chance));
                    }

                    for (DropHolder drop : spoilDrops) {
                        long min = drop.getMin();
                        long max = drop.getMax();
                        double chance = drop.getChance() * dropMultiplier;
                        if (min >= 10 && chance > 100.0f ||
                                min * dropMultiplier >= 5 && chance / dropMultiplier <= 100.01f) {
                            min = Math.round(min * dropMultiplier);
                            max = Math.round(max * dropMultiplier);
                            chance /= dropMultiplier;
                        }

                        while (chance > 100.01f) {
                            min *= 2;
                            max *= 2;
                            chance /= 2.0f;
                        }

                        while (chance < 50.0f && min > 1) {
                            min /= 2;
                            max /= 2;
                            chance *= 2.0f;
                        }

                        mob.addSpoil(new DropHolder(DropType.SPOIL, drop.getItemId(), min, max, chance));
                    }

                }

                customized++;
            }
        }
        handlers.communityboard.DropSearchBoard.getInstance().updateDropIndex();
        LOGGER.info("Farm Zone Manager: loaded " + customized + " farm zone customizations.");
    }

}
