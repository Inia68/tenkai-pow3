package org.l2jmobius.gameserver.instancemanager;

import org.l2jmobius.Config;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.impl.NpcData;
import org.l2jmobius.gameserver.datatables.ItemTable;
import org.l2jmobius.gameserver.datatables.SpawnTable;
import org.l2jmobius.gameserver.enums.DropType;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.model.DailyMissionPlayerEntry;
import org.l2jmobius.gameserver.model.Location;
import org.l2jmobius.gameserver.model.Spawn;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.instance.MonsterInstance;
import org.l2jmobius.gameserver.model.actor.instance.NpcInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.holders.DropHolder;
import org.l2jmobius.gameserver.model.items.Item;
import org.l2jmobius.gameserver.model.zone.ZoneType;
import org.l2jmobius.gameserver.model.zone.type.TeleportZone;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.BuilderUtil;
import org.l2jmobius.gameserver.util.xml.XmlDocument;
import org.l2jmobius.gameserver.util.xml.XmlNode;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.l2jmobius.gameserver.model.events.AbstractScript.getRandom;

public class UpgradableFarmZoneManager {
    protected static final Logger LOGGER = Logger.getLogger(NpcData.class.getName());
    public static Map<Integer, FarmZone> _farmZones = new LinkedHashMap<Integer, FarmZone>();

    private static UpgradableFarmZoneManager _instance;

    public class FarmZone {
        private int _id;
        private String _name;
        private int _level;
        private long _adenaInvested;
        private int _maxMobs;
        private int _dropLevel;
        private ZoneType _zone;
        Map<Integer, List<NpcTemplate>> _mobs;
        Map<Integer, List<DropHolder>> _drops;
        List<Spawn> _monsters;
        int _availablePoints;
        Map<Integer, Map<String, Long>> _levelup;

        private int POINT_PRICE = 10000000;

        public FarmZone(int id, ZoneType zone, Map<Integer, List<NpcTemplate>> mobs, Map<Integer, List<DropHolder>> drops, Map<Integer, Map<String, Long>> levelup) {
            _id = id;
            _level = 0;
            _maxMobs = 0;
            _dropLevel = 0;
            _zone = zone;
            _mobs = mobs;
            _drops = drops;
            _monsters = new LinkedList<>();
            _levelup = levelup;

            try (Connection con = DatabaseFactory.getConnection();
                 PreparedStatement ps = con.prepareStatement("SELECT * FROM upgradable_farmzone WHERE id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        _name = rs.getString("name");
                        _level = rs.getInt("level");
                        _maxMobs = rs.getInt("max_mobs");
                        _dropLevel = rs.getInt("drop_level");
                        _availablePoints = rs.getInt("available_points");
                        _adenaInvested = rs.getLong("adena_invested");
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error while loading farmone in database: ", e);
            }
            spawnMobs();
        }

        public boolean increaseMobAmount(PlayerInstance player) {
            if (_maxMobs + 1 >= 100) {
                player.sendMessage("You already reached the max monster amount");
                return false;
            }

            int requiredPoints = 2;

            if (_availablePoints < requiredPoints) {
                player.sendMessage("You need " + requiredPoints + " points to increase the number of monsters");
                return false;
            }

            _availablePoints -= requiredPoints;
            _maxMobs += 1;

            try (Connection con = DatabaseFactory.getConnection()) {
                final PreparedStatement statement = con.prepareStatement("UPDATE upgradable_farmzone SET max_mobs=?, available_points=? WHERE id=?");
                statement.setInt(1, _maxMobs);
                statement.setInt(2, _availablePoints);
                statement.setInt(3, _id);
                statement.execute();
            } catch (SQLException se) {
            }
            spawnMobs();
            return true;
        }

        public boolean increaseLevel(PlayerInstance player) {
            if (_mobs.get(_level + 1) == null) {
                player.sendMessage("There's no more level for this zone");
                return false;
            }

            long requiredAdena = _levelup.get(_level + 1).get("adena");
            long requiredBe = _levelup.get(_level + 1).get("be");
            long requiredPoints = _levelup.get(_level + 1).get("points");

            long playerAdena = player.getInventory().getInventoryItemCount(57, 0);
            long playerBlueEva = player.getInventory().getInventoryItemCount(4355, 0);

            if (playerAdena < requiredAdena) {
                player.sendMessage("You need " + requiredAdena + " adena to level up the zone");
                return false;
            }

            if (playerBlueEva < requiredBe) {
                player.sendMessage("You need" + requiredBe + " blue eva to level up the zone");
                return false;
            }

            if (_availablePoints < requiredPoints) {
                player.sendMessage("You need " + requiredPoints + " points to level up the zone");
                return false;
            }

            _availablePoints -= requiredPoints;
            _level += 1;

            try (Connection con = DatabaseFactory.getConnection()) {
                final PreparedStatement statement = con.prepareStatement("UPDATE upgradable_farmzone SET level=?, available_points=? WHERE id=?");
                statement.setInt(1, _level);
                statement.setInt(2, _availablePoints);
                statement.setInt(3, _id);
                statement.execute();
            } catch (SQLException se) {
            }
            spawnMobs();
            return true;
        }

        public boolean increaseDrop(PlayerInstance player) {
            if (_drops.get(_dropLevel + 1) == null) {
                player.sendMessage("There's no more level for this zone");
                return false;
            }

            int requiredPoints = _dropLevel * 5;

            if (_availablePoints < requiredPoints) {
                player.sendMessage("You need " + requiredPoints + " points to level up the drop of the zone");
                return false;
            }

            _availablePoints -= requiredPoints;
            _dropLevel += 1;

            try (Connection con = DatabaseFactory.getConnection()) {
                final PreparedStatement statement = con.prepareStatement("UPDATE upgradable_farmzone SET drop_level =?, available_points=? WHERE id=?");
                statement.setInt(1, _dropLevel);
                statement.setInt(2, _availablePoints);
                statement.setInt(3, _id);
                statement.execute();
            } catch (SQLException se) {
            }
            player.sendMessage("The drop level has been changed !");
            return true;
        }

        public boolean buyPoint(PlayerInstance player) {

            boolean done = player.destroyItemByItemId("buy point", 57, POINT_PRICE, null, true);

            if (!done) {
                player.sendMessage("Not enough adena");
                return false;
            }

            _availablePoints += 1;
            _adenaInvested += POINT_PRICE;
            try (Connection con = DatabaseFactory.getConnection()) {
                final PreparedStatement statement = con.prepareStatement("UPDATE upgradable_farmzone SET available_points=?, adena_invested=? WHERE id=?");
                statement.setInt(1, _availablePoints);
                statement.setLong(2, _adenaInvested);
                statement.setInt(3, _id);
                statement.execute();
            } catch (SQLException se) {
            }
            return true;

        }

        public void spawnMobs() {
            if (_monsters.isEmpty() == false) {
                _monsters.forEach(monsterInstance -> {
                    monsterInstance.getLastSpawn().deleteMe();
                });
                _monsters.clear();
                ;
            }
            int amount = 0;
            while (amount < _maxMobs) {

                try {
                    Location spawnLocation = _zone.getZone().getRandomPoint();
                    Spawn mob = new Spawn(getRandomEntry(_mobs.get(_level)));
                    mob.setXYZ(spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ());
                    mob.setHeading(Rnd.get(65536));
                    mob.setRespawnDelay(50);
                    mob.setAmount(1);
                    SpawnTable.getInstance().addNewSpawn(mob, false);
                    mob.init();
                    mob.getLastSpawn().broadcastInfo();
                    _monsters.add(mob);
                    amount++;
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

            }
        }

        public void onDie(PlayerInstance killed, PlayerInstance killer) {
        }

        public void onMonsterDie(Creature mob) {
            Npc monster = (Npc) mob;
            for (DropHolder drop : _drops.get(_dropLevel)) {
                double chance = getRandom(0, 100);
                if (chance <= drop.getChance()) {
                    monster.dropItem(monster, drop.getItemId(), getRandom((int) drop.getMin(), (int) drop.getMax()));
                }
            }

        }

        public Map<Integer, List<NpcTemplate>> getMobs() {
            return _mobs;
        }

        public Map<Integer, List<DropHolder>> getDrops() {
            return _drops;
        }

        public Map<Integer, Map<String, Long>> getLevelUp() { return _levelup; }

        public int getLevel() {
            return _level;
        }

        public int getId() {
            return _id;
        }

        public String getName() {
            return _name;
        }

        public int getMaxMobs() {
            return _maxMobs;
        }

        public int getDropLevel() {
            return _dropLevel;
        }

        public ZoneType getZone() {
            return _zone;
        }

        public int getAvailablePoints() {
            return _availablePoints;
        }
    }


    public static UpgradableFarmZoneManager getInstance() {
        return _instance == null ? (_instance = new UpgradableFarmZoneManager()) : _instance;
    }

    public void getDropList(PlayerInstance player, FarmZone farmzone) {
        String html = "<html>" + "<body>";

        html += "<table width=280 >\n";
        html += "<tr><td><table width=280 border=0 bgcolor=131210><tr><td align=center width=170>DROP</td></tr></table></td></tr>\n";
        for (DropHolder drop : farmzone.getDrops().get(farmzone.getDropLevel())) {
            Item item = ItemTable.getInstance().getTemplate(drop.getItemId());
            if (item == null) {
                continue;
            }
            html += "<tr><td><table width=280 border=0 bgcolor=131210><tr><td width=100><img src=\"" + item.getIcon() + "\" width=32 height=32></td><td align=center width=170>" + item.getName() + "</td></tr></table></td></tr>\n";
            html += "<tr><td><table width=280 border=0 bgcolor=131210><tr><td width=100><font color=\"LEVEL\">Chance:</font></td><td align=right width=170>" + drop.getChance() + "</td></tr></table></td></tr>\n";
            html += "<tr><td><table width=280 border=0 bgcolor=131210><tr><td width=100><font color=\"LEVEL\">Count:</font></td><td align=right width=170>" + (drop.getItemId() == 57 ? drop.getMin() * Config.RATE_DROP_AMOUNT_BY_ID.get(57) : drop.getMin()) + " - " + (drop.getItemId() == 57 ? drop.getMax() * Config.RATE_DROP_AMOUNT_BY_ID.get(57) : drop.getMax()) + "</td></tr></table></td></tr>\n";

            html += "<tr height=25><td height=25></td></tr>";
        }
        html += "</table>";


        html += "<Button ALIGN=LEFT ICON=\"NORMAL\" action=\"bypass -h farm_zone;" + farmzone.getId() + "\">Back</Button>" + "<br>";
        html += "</body></html>";
        player.sendPacket(new NpcHtmlMessage(0, html));
    }

    public void getMobsList(PlayerInstance player, FarmZone farmzone) {
        String html = "<html>" + "<body>";

        for (NpcTemplate mob : farmzone.getMobs().get(farmzone.getLevel())) {
            html += "" + mob.getName() + " Lv." + mob.getLevel() + "<br>";
        }

        html += "<Button ALIGN=LEFT ICON=\"NORMAL\" action=\"bypass -h farm_zone;" + farmzone.getId() + "\">Back</Button>" + "<br>";
        html += "</body></html>";
        player.sendPacket(new NpcHtmlMessage(0, html));
    }

    public void getUpgradePage(PlayerInstance player, FarmZone farmzone) {
        String html = "<html>" + "<body>";

        html += "Available points: " + farmzone.getAvailablePoints() + "<br>";
        html += "<Button ALIGN=LEFT ICON=\"NORMAL\" action=\"bypass -h farm_zone;" + farmzone.getId() + ";upgrade;mob_amount\">increase mob limit</Button>";
        html += "<Button ALIGN=LEFT ICON=\"NORMAL\" action=\"bypass -h farm_zone;" + farmzone.getId() + ";upgrade;level\">level up zone</Button>";
        html += "<Button ALIGN=LEFT ICON=\"NORMAL\" action=\"bypass -h farm_zone;" + farmzone.getId() + ";upgrade;drop\">level up drops</Button>";
        html += "<br><br>";
        html += "<Button ALIGN=LEFT ICON=\"NORMAL\" action=\"bypass -h farm_zone;" + farmzone.getId() + ";upgrade;point\">Buy 1 point</Button>";
        html += "<Button ALIGN=LEFT ICON=\"NORMAL\" action=\"bypass -h farm_zone;" + farmzone.getId() + "\">Back</Button>";
        html += "</body></html>";
        player.sendPacket(new NpcHtmlMessage(0, html));
    }

    public void getPanel(PlayerInstance player, String params) {

        final StringTokenizer st = new StringTokenizer(params, ";");
        st.nextToken();
        final int zoneId = Integer.parseInt(st.nextToken());
        String option = null;
        if (st.hasMoreTokens()) {
            option = st.nextToken();
        }
        String option2 = null;

        FarmZone farmZone = _farmZones.get(zoneId);
        if (farmZone == null) {
            return;
        }

        if (option != null) {
            switch (option) {
                case "mobs":
                    getMobsList(player, farmZone);
                    break;
                case "drops":
                    getDropList(player, farmZone);
                    break;
                case "upgrade":
                    if (st.hasMoreTokens()) {
                        option2 = st.nextToken();
                        switch (option2) {
                            case "point": {
                                farmZone.buyPoint(player);
                                break;
                            }
                            case "level": {
                                farmZone.increaseLevel(player);
                                break;
                            }
                            case "drop": {
                                farmZone.increaseDrop(player);
                                break;
                            }
                            case "mob_amount": {
                                farmZone.increaseMobAmount(player);
                                break;
                            }
                        }
                    }
                    getUpgradePage(player, farmZone);
                    break;
            }
            return;
        }

        String html = "<html>" + "<body>";
        html += "Zone id : " + zoneId + "<br>";
        html += "Zone id : " + farmZone.getName() + "<br>";
        html += "Level : " + farmZone.getLevel() + "<br>";
        html += "Drop level : " + farmZone.getDropLevel() + "<br>";
        html += "Max Mobs: " + farmZone.getMaxMobs() + "<br>";
        html += "<Button ALIGN=LEFT ICON=\"NORMAL\" action=\"bypass -h farm_zone;" + farmZone.getId() + ";upgrade\">Upgrade</Button>";
        html += "<Button ALIGN=LEFT ICON=\"NORMAL\" action=\"bypass -h farm_zone;" + farmZone.getId() + ";mobs\">Mobs</Button>";
        html += "<Button ALIGN=LEFT ICON=\"NORMAL\" action=\"bypass -h farm_zone;" + farmZone.getId() + ";drops\">Drops</Button>";
        html += "</body></html>";
        player.sendPacket(new NpcHtmlMessage(0, html));
    }

    private UpgradableFarmZoneManager() {
        load();
    }

    private void load() {

        File file = new File(Config.DATAPACK_ROOT, "/data/farm_zones/upgradableFarmZone.xml");
        XmlDocument doc = new XmlDocument(file);

        for (XmlNode farmNode : doc.getChildren()) {
            if (farmNode.getName().equalsIgnoreCase("zone")) {
                ZoneType zone = ZoneManager.getInstance().getZoneByName(farmNode.getString("name"));
                int id = farmNode.getInt("id");
                Map<Integer, List<NpcTemplate>> mobs = new LinkedHashMap<Integer, List<NpcTemplate>>();
                Map<Integer, List<DropHolder>> drops = new LinkedHashMap<Integer, List<DropHolder>>();
                Map<Integer, Map<String, Long>> levelup = new LinkedHashMap<Integer, Map<String, Long>>();
                for (XmlNode zoneNode : farmNode.getChildren()) {
                    if (zoneNode.getName().equalsIgnoreCase("mobs")) {
                        for (XmlNode groupNode : zoneNode.getChildren()) {
                            if (groupNode.getName().equalsIgnoreCase("group")) {
                                List<NpcTemplate> mob = new LinkedList<NpcTemplate>();
                                long adena = groupNode.getLong("adena", 0);
                                long blueEva = groupNode.getLong("be", 0);
                                int level = groupNode.getInt("level");
                                long points = groupNode.getLong("points", level * 5);

                                if (level > 1) {
                                    Map<String, Long> conditions = new HashMap<String, Long>();
                                    conditions.put("adena", adena);
                                    conditions.put("be", blueEva);
                                    conditions.put("points", points);
                                    levelup.put(level, conditions);
                                }

                                for (XmlNode mobNode : groupNode.getChildren()) {
                                    int mobId = mobNode.getInt("id");
                                    NpcTemplate temp = NpcData.getInstance().getTemplate(mobId);
                                    if (temp == null) {
                                        continue;
                                    }
                                    mob.add(temp);
                                }
                                mobs.put(level, mob);
                            }
                        }
                    }
                    if (zoneNode.getName().equalsIgnoreCase("drops")) {
                        for (XmlNode dropNode : zoneNode.getChildren()) {
                            if (dropNode.getName().equalsIgnoreCase("group")) {
                                int dropLevel = dropNode.getInt("level");
                                List<DropHolder> drop = new LinkedList<>();
                                for (XmlNode groupDropNode : dropNode.getChildren()) {
                                    if (groupDropNode.getName().equalsIgnoreCase("itemDrop")) {
                                        int itemId = groupDropNode.getInt("itemId");
                                        int min = groupDropNode.getInt("min");
                                        int max = groupDropNode.getInt("max");
                                        float chance = groupDropNode.getFloat("chance");

                                        DropHolder dd = new DropHolder(DropType.DROP, itemId, min, max, chance);

                                        Item item = ItemTable.getInstance().getTemplate(dd.getItemId());
                                        if (item == null) {
                                            LOGGER.warning("Drop data for undefined item template in custom drop definitions!");
                                            continue;
                                        }
                                        drop.add(dd);

                                    }
                                    drops.put(dropLevel, drop);
                                }
                            }
                        }
                    }
                }
                LOGGER.info("Upgradable Farm Zone Manager: " + zone.getName() + "  loaded");
                FarmZone farmzone = new FarmZone(id, zone, mobs, drops, levelup);
                _farmZones.put(id, farmzone);
            }
        }

        LOGGER.info("Upgradable Farm Zone Manager: loaded");
    }

    public double randomBetween(double low, double high) {
        Random r = new Random();
        return low + (high - low) * r.nextDouble();
    }

    public static <T> T getRandomEntry(List<T> list) {
        if (list.isEmpty()) {
            return null;
        }
        return list.get(getRandom(list.size()));
    }
}
