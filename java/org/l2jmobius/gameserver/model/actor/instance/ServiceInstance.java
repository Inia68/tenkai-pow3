/*
 * This file is part of the L2J Mobius project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.model.actor.instance;

import kotlin.reflect.jvm.internal.impl.serialization.deserialization.ClassData;
import org.l2jmobius.Config;
import org.l2jmobius.commons.concurrent.ThreadPool;
import org.l2jmobius.commons.util.EmptyQueue;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.data.sql.impl.ClanTable;
import org.l2jmobius.gameserver.data.xml.impl.*;
import org.l2jmobius.gameserver.enums.*;
import org.l2jmobius.gameserver.events.instanced.EventsManager;
import org.l2jmobius.gameserver.instancemanager.CastleManager;
import org.l2jmobius.gameserver.instancemanager.GlobalVariablesManager;
import org.l2jmobius.gameserver.model.SkillLearn;
import org.l2jmobius.gameserver.model.actor.Npc;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.actor.templates.PlayerTemplate;
import org.l2jmobius.gameserver.model.announce.Announcement;
import org.l2jmobius.gameserver.model.base.ClassId;
import org.l2jmobius.gameserver.model.base.ClassInfo;
import org.l2jmobius.gameserver.model.base.SubClass;
import org.l2jmobius.gameserver.model.clan.Clan;
import org.l2jmobius.gameserver.model.clan.ClanPrivilege;
import org.l2jmobius.gameserver.model.conditions.ConditionPlayerClassIdRestriction;
import org.l2jmobius.gameserver.model.entity.Castle;
import org.l2jmobius.gameserver.model.entity.Siege;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.items.Item;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.model.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.model.zone.type.MainTown;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.*;
import org.l2jmobius.gameserver.network.serverpackets.classchange.ExRequestClassChangeUi;
import org.l2jmobius.gameserver.taskmanager.AttackStanceTaskManager;
import org.l2jmobius.gameserver.util.Broadcast;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import static org.l2jmobius.gameserver.model.events.AbstractScript.showOnScreenMsg;

/**
 * @version $Revision: 1.10.4.9 $ $Date: 2005/04/11 10:06:08 $
 */
public class ServiceInstance extends NpcInstance {
    private List<NpcFollower> _followingNpcs;

    /**
     * @param template
     */
    public ServiceInstance(NpcTemplate template) {
        super(template);
        setInstanceType(InstanceType.BufferInstance);
    }

    public class NpcFollower {
        private PlayerInstance _target;
        private Npc _npc;
        ScheduledFuture<?> _task;

        public NpcFollower(Npc npc, PlayerInstance target) {
            _target = target;
            _npc = npc;
            if (_npc != null) {
                startFollow();
            }
        }

        void startFollow() {
            _npc.setRunning();
            _npc.setTarget(_target);
            _npc.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, _target);
            _task = ThreadPool.scheduleAtFixedRate(this::checkTarget, 1, 5000);
        }

        void checkTarget() {
            if (_target == null || _npc.calculateDistance3D(_target.getLocation()) > 1000) {
                _npc.deleteMe();
                _followingNpcs.remove(this);
                _task.cancel(true);
            }
        }


        public Npc getNpc() {return _npc;}
        public PlayerInstance getTarget() {return _target;}

    }

    @Override
    public boolean isNpc() {
        return true;
    }

    @Override
    public boolean isWarehouse() {
        return true;
    }

    @Override
    public void onBypassFeedback(PlayerInstance player, String command) {
        // lil check to prevent enchant exploit
        if (player.getActiveEnchantItem() != null) {
            //Util.handleIllegalPlayerAction(player, "Player " + player.getName() + " trying to use enchant exploit, ban this player!", IllegalPlayerAction.PUNISH_KICK);
            return;
        }

        String param[] = command.split("_");
        String[] commandStr = command.split(" ");
        String actualCommand = commandStr[0]; // Get actual command

        String cmdParams = "";
        String cmdParams2 = "";

        if (commandStr.length >= 2) {
            cmdParams = commandStr[1];
        }
        if (commandStr.length >= 3) {
            cmdParams2 = commandStr[2];
        }

        if (command.startsWith("1stClass")) {
            showHtmlMenu(player, getObjectId(), 1);
        } else if (command.startsWith("2ndClass")) {
            showHtmlMenu(player, getObjectId(), 2);
        } else if (command.startsWith("3rdClass")) {
            showHtmlMenu(player, getObjectId(), 3);
        } else if (command.startsWith("change_class")) {
            int val = Integer.parseInt(command.substring(13));

            if (checkAndChangeClass(player, val)) {
                NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
                html.setFile(player, "data/html/classmaster/ok.htm");
                html.replace("%name%", ClassListData.getInstance().getClass(val).getClassName());
                player.sendPacket(html);
            }
        } else if (command.startsWith("pippi")) {
            NpcTemplate npc = NpcData.getInstance().getTemplate(16108);
            if (npc != null) {
                NpcInstance n = new NpcInstance(npc);
                n.spawnMe(player.getX() + 50, player.getY(), player.getZ());
                n.getAI().setIntention(CtrlIntention.AI_INTENTION_FOLLOW, player);
                new NpcFollower(n, player);
            }
        } else if (command.startsWith("classmaster")) {
            final StringBuilder menu = new StringBuilder(100);
            NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
            ClassId cl = player.getClassId();

            int lvl = cl.level();
            switch (lvl) {
                case 0:
                    showHtmlMenu(player, getObjectId(), 1);
                    break;
                case 1:
                    showHtmlMenu(player, getObjectId(), 2);
                    break;
                case 2:
                    showHtmlMenu(player, getObjectId(), 3);
                    break;
                case 3:
                    showHtmlMenu(player, getObjectId(), 4);
            }
        } else if (command.startsWith("WithdrawP")) {
            player.sendPacket(ActionFailed.STATIC_PACKET);
            player.setActiveWarehouse(player.getWarehouse());

            if (player.getActiveWarehouse().getSize() == 0) {
                player.sendPacket(SystemMessageId.YOU_HAVE_NOT_DEPOSITED_ANY_ITEMS_IN_YOUR_WAREHOUSE);
                return;
            }

            player.sendPacket(new WareHouseWithdrawalList(1, player, WareHouseWithdrawalList.PRIVATE));
            player.sendPacket(new WareHouseWithdrawalList(2, player, WareHouseWithdrawalList.PRIVATE));

            return;
        } else if (command.equals("DepositP")) {
            player.sendPacket(ActionFailed.STATIC_PACKET);
            player.setActiveWarehouse(player.getWarehouse());
            player.setInventoryBlockingStatus(true);
            player.sendPacket(new WareHouseDepositList(1, player, WareHouseDepositList.PRIVATE));
            player.sendPacket(new WareHouseDepositList(2, player, WareHouseDepositList.PRIVATE));
            return;
        } else if (command.startsWith("WithdrawC")) {

            player.sendPacket(ActionFailed.STATIC_PACKET);

            if (!player.hasClanPrivilege(ClanPrivilege.CL_VIEW_WAREHOUSE)) {
                player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_THE_RIGHT_TO_USE_THE_CLAN_WAREHOUSE);
                return;
            }

            player.setActiveWarehouse(player.getClan().getWarehouse());

            if (player.getActiveWarehouse().getSize() == 0) {
                player.sendPacket(SystemMessageId.YOU_HAVE_NOT_DEPOSITED_ANY_ITEMS_IN_YOUR_WAREHOUSE);
                return;
            }

            for (ItemInstance i : player.getActiveWarehouse().getItems()) {
                if (i.isTimeLimitedItem() && (i.getRemainingTime() <= 0)) {
                    player.getActiveWarehouse().destroyItem("ItemInstance", i, player, null);
                }
            }

            player.sendPacket(new WareHouseWithdrawalList(1, player, WareHouseWithdrawalList.CLAN));
            player.sendPacket(new WareHouseWithdrawalList(2, player, WareHouseWithdrawalList.CLAN));
            return;
        } else if (command.equals("DepositC")) {
            player.sendPacket(ActionFailed.STATIC_PACKET);
            player.setActiveWarehouse(player.getClan().getWarehouse());
            player.setInventoryBlockingStatus(true);
            player.sendPacket(new WareHouseDepositList(1, player, WareHouseDepositList.CLAN));
            player.sendPacket(new WareHouseDepositList(2, player, WareHouseDepositList.CLAN));
            return;
        } else if (command.startsWith("Subclass")) {
            if (EventsManager.getInstance().isPlayerParticipant(player.getObjectId())) {
                player.sendMessage("You can't change sub classes if you are joined in an event.");
                return;
            }

            int cmdChoice = Integer.parseInt(command.substring(9, 10).trim());

            // Subclasses may not be changed while a skill is in use.
            if (player.isCastingNow() || player.isAllSkillsDisabled()) {
                player.sendPacket(SystemMessageId.SUBCLASSES_MAY_NOT_BE_CREATED_OR_CHANGED_WHILE_A_SKILL_IS_IN_USE);
                return;
            }

            String content = "<html><body>";
            NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());

            int paramOne = 0;
            int paramTwo = 0;

            try {
                int endIndex = command.indexOf(' ', 11);
                if (endIndex == -1) {
                    endIndex = command.length();
                }

                paramOne = Integer.parseInt(command.substring(11, endIndex).trim());
                if (command.length() > endIndex) {
                    paramTwo = Integer.parseInt(command.substring(endIndex).trim());
                }
            } catch (Exception ignored) {
            }

            int maxSubs = Config.MAX_SUBCLASS;
            if (player.getRace() == Race.ERTHEIA) {
                maxSubs = 1;
            }
            switch (cmdChoice) {
                case 0: // Subclass change menu
                    html.setFile(player, "data/html/villagemaster/SubClass.htm");
                    break;
                case 1: // Add Subclass - Initial
                    // Avoid giving player an option to add a new sub class, if they have three already.
                    if (player.getTotalSubClasses() >= maxSubs) {
                        html.setFile(player, "data/html/villagemaster/SubClass_Fail.htm");
                        break;
                    }

                    html.setFile(player, "data/html/villagemaster/SubClass_Add.htm");
                    final StringBuilder content1 = StringUtil.startAppend(200);
                    Set<ClassId> subsAvailable = getAvailableSubClasses(player);

                    if (subsAvailable != null && !subsAvailable.isEmpty()) {
                        subsAvailable.forEach(subClassId -> {
                            ClassInfo subClass = ClassListData.getInstance().getClass(subClassId);
                            StringUtil.append(content1, "<a action=\"bypass -h npc_%objectId%_Subclass 4 ",
                                    String.valueOf(subClass.getClassId().getId()), "\" msg=\"1268;", formatClassForDisplay(subClass),
                                    "\">", formatClassForDisplay(subClass), "</a><br>");
                        });

                    } else {
                        // TODO: Retail message
                        player.sendMessage("There are no sub classes available at this time.");
                        return;
                    }
                    html.replace("%list%", content1.toString());
                    break;
                case 2: // Change Class - Initial


                    content += "Change Subclass:<br>";

                    final int baseClassId = player.getBaseClass();

                    if (player.getSubClasses().isEmpty()) {
                        content += "You can't change sub classes when you don't have a sub class to begin with.<br>";
                    } else {
                        content += "Which class would you like to switch to?<br>";

                        if (baseClassId == player.getActiveClass()) {
                            content += ClassListData.getInstance().getClassNameById(baseClassId) +
                                    "&nbsp;<font color=\"LEVEL\">(Base Class)</font><br><br>";
                        } else {
                            content += "<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 5 0\">" +
                                    ClassListData.getInstance().getClassNameById(baseClassId) + "</a>&nbsp;" +
                                    "<font color=\"LEVEL\">(Base Class)</font><br><br>";
                        }

                        for (Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext(); ) {
                            SubClass subClass = subList.next();
                            int subClassId = subClass.getClassId();

                            if (subClassId == player.getActiveClass()) {
                                content += ClassListData.getInstance().getClassNameById(subClassId) + "<br>";
                            } else {
                                content += "<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 5 " +
                                        subClass.getClassIndex() + "\">" +
                                        ClassListData.getInstance().getClassNameById(subClassId) + "</a><br>";
                            }
                        }
                    }
                    break;
                case 3: // Change/Cancel Subclass - Initial
                    if (player.getSubClasses() == null || player.getSubClasses().isEmpty()) {
                        html.setFile(player, "data/html/villagemaster/SubClass_ModifyEmpty.htm");
                        break;
                    }

                    // custom value
                    if (player.getTotalSubClasses() > 3) {
                        html.setFile(player, "data/html/villagemaster/SubClass_ModifyCustom.htm");
                        final StringBuilder content3 = StringUtil.startAppend(200);
                        int classIndex = 1;

                        for (Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext(); ) {
                            SubClass subClass = subList.next();

                            StringUtil.append(content3, "Sub-class ", String.valueOf(classIndex++), "<br>",
                                    "<a action=\"bypass -h npc_%objectId%_Subclass 6 ",
                                    String.valueOf(subClass.getClassIndex()), "\">",
                                    ClassListData.getInstance().getClassNameById(subClass.getClassId()), "</a><br>");
                        }
                        html.replace("%list%", content3.toString());
                    } else {
                        // retail html contain only 3 subclasses
                        html.setFile(player, "data/html/villagemaster/SubClass_Modify.htm");
                        if (player.getSubClasses().containsKey(1)) {
                            html.replace("%sub1%", ClassListData.getInstance()
                                    .getClassNameById(player.getSubClasses().get(1).getClassId()));
                        } else {
                            html.replace(
                                    "Sub-class 1<br>\n<Button ALIGN=LEFT ICON=\"NORMAL\" action=\"bypass -h npc_%objectId%_Subclass 6 1\">%sub1%</Button>",
                                    "");
                        }

                        if (player.getSubClasses().containsKey(2)) {
                            html.replace("%sub2%", ClassListData.getInstance()
                                    .getClassNameById(player.getSubClasses().get(2).getClassId()));
                        } else {
                            html.replace(
                                    "Sub-class 2<br>\n<Button ALIGN=LEFT ICON=\"NORMAL\" action=\"bypass -h npc_%objectId%_Subclass 6 2\">%sub2%</Button>",
                                    "");
                        }

                        if (player.getSubClasses().containsKey(3)) {
                            html.replace("%sub3%", ClassListData.getInstance()
                                    .getClassNameById(player.getSubClasses().get(3).getClassId()));
                        } else {
                            html.replace(
                                    "Sub-class 3<br>\n<Button ALIGN=LEFT ICON=\"NORMAL\" action=\"bypass -h npc_%objectId%_Subclass 6 3\">%sub3%</Button>",
                                    "");
                        }
                    }
                    break;
                case 4: // Add Subclass - Action (Subclass 4 x[x])
                    /*
                     * If the character is less than level 75 on any of their previously chosen
                     * classes then disallow them to change to their most recently added sub-class choice.
                     */

                    if (!player.getFloodProtectors().getSubclass().tryPerformAction("add subclass")) {
                        LOGGER.warning("Player " + player.getName() + " has performed a subclass change too fast");
                        return;
                    }

                    boolean allowAddition = true;

                    if (player.getTotalSubClasses() >= maxSubs) {
                        allowAddition = false;
                    }

                    if (player.getLevel() < 75 && !(player.getRace() != Race.ERTHEIA || player.getLevel() >= 85)) {
                        allowAddition = false;
                    }

                    if (allowAddition) {
                        if (!player.getSubClasses().isEmpty()) {
                            for (Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext(); ) {
                                SubClass subClass = subList.next();

                                if (subClass.getLevel() < 75) {
                                    allowAddition = false;
                                    break;
                                }
                            }
                        }
                    }

                    if (allowAddition && isValidNewSubClass(player, paramOne)) {
                        if (!player.addSubClass(paramTwo, player.getTotalSubClasses() + 1, false)) {
                            return;
                        }

                        player.setActiveClass(player.getTotalSubClasses());


                        player.giveAvailableSkills(true, true, true);
                        player.sendSkillList();


                        html.setFile(player, "data/html/villagemaster/SubClass_AddOk.htm");

                        player.sendPacket(SystemMessageId.THE_NEW_SUBCLASS_S1_HAS_BEEN_ADDED_CONGRATS); // Subclass added.
                        player.sendPacket(new ExSubjobInfo(player, SubclassInfoType.NEW_SLOT_USED));
                    } else {
                        html.setFile(player, "data/html/villagemaster/SubClass_Fail.htm");
                    }
                    break;
                case 5: // Change Class - Action
                    /*
                     * If the character is less than level 75 on any of their previously chosen
                     * classes then disallow them to change to their most recently added sub-class choice.
                     *
                     * Note: paramOne = classIndex
                     */

                    if (!player.getFloodProtectors().getSubclass().tryPerformAction("add subclass")) {
                        LOGGER.warning("Player " + player.getName() + " has performed a subclass change too fast");
                        return;
                    }


                    player.setActiveClass(paramOne);

                    content += "Change Subclass:<br>Your active sub class is now a <font color=\"LEVEL\">" +
                            ClassListData.getInstance().getClassNameById(player.getActiveClass()) + "</font>.";

                    player.sendPacket(new ExSubjobInfo(player, SubclassInfoType.CLASS_CHANGED));
                    break;
                case 6: // Change/Cancel Subclass - Choice
                    // validity check
                    if (paramOne < 1 || paramOne > maxSubs) {
                        return;
                    }


                    subsAvailable = getAvailableSubClasses(player);

                    // another validity check
                    if (subsAvailable == null || subsAvailable.isEmpty()) {
                        // TODO: Retail message
                        player.sendMessage("There are no sub classes available at this time.");
                        return;
                    }

                    final StringBuilder content6 = StringUtil.startAppend(200);

                    int finalParamOne = paramOne;
                    subsAvailable.forEach(subClassId -> {
                        ClassInfo subClass = ClassListData.getInstance().getClass(subClassId);
                        StringUtil.append(content6, "<a action=\"bypass -h npc_%objectId%_Subclass 7 ",
                                String.valueOf(finalParamOne), " ", String.valueOf(subClass.getClassId().getId()), "\" msg=\"1445;",
                                "\">", formatClassForDisplay(subClass), "</a><br>");
                    });


                    switch (paramOne) {
                        case 1:
                            html.setFile(player, "data/html/villagemaster/SubClass_ModifyChoice1.htm");
                            break;
                        case 2:
                            html.setFile(player, "data/html/villagemaster/SubClass_ModifyChoice2.htm");
                            break;
                        case 3:
                            html.setFile(player, "data/html/villagemaster/SubClass_ModifyChoice3.htm");
                            break;
                        default:
                            html.setFile(player, "data/html/villagemaster/SubClass_ModifyChoice.htm");
                    }
                    html.replace("%list%", content6.toString());
                    break;
                case 7: // Change Subclass - Action
                    /*
                     * Warning: the information about this subclass will be removed from the
                     * subclass list even if false!
                     */

                    if (!player.getFloodProtectors().getSubclass().tryPerformAction("add subclass")) {
                        LOGGER.warning("Player " + player.getName() + " has performed a subclass change too fast");
                        return;
                    }

                    if (player.modifySubClass(paramOne, paramTwo, false)) {
                        player.stopAllEffects(); // all effects from old subclass stopped!
                        player.setActiveClass(paramOne);

                        content += "Change Subclass:<br>Your sub class has been changed to <font color=\"LEVEL\">" +
                                ClassListData.getInstance().getClassNameById(paramTwo) + "</font>.";


                        player.giveAvailableSkills(true, true, true);
                        player.sendSkillList();

                        player.sendPacket(SystemMessageId.THE_NEW_SUBCLASS_S1_HAS_BEEN_ADDED_CONGRATS); // Subclass added.
                        player.sendPacket(new ExSubjobInfo(player, SubclassInfoType.CLASS_CHANGED));
                    } else {
                        /*
                         * This isn't good! modifySubClass() removed subclass from memory
                         * we must update _classIndex! Else IndexOutOfBoundsException can turn
                         * up some place down the line along with other seemingly unrelated
                         * problems.
                         */
                        player.setActiveClass(0); // Also updates _classIndex plus switching _classid to baseclass.

                        player.sendMessage(
                                "The sub class could not be added, you have been reverted to your base class.");
                        return;
                    }
                    break;
                case 8: // Make Dual Class - Initial
                    if (player.getSubClasses() == null || player.getSubClasses().isEmpty()) {
                        player.sendMessage("You don't have any subclass!");
                        return;
                    }


                    // retail html contain only 3 subclasses
                    html.setFile(player, "data/html/villagemaster/SubClass_MakeDual.htm");
                    if (player.getSubClasses().containsKey(1)) {
                        html.replace("%sub1%", ClassListData.getInstance()
                                .getClassNameById(player.getSubClasses().get(1).getClassId()));
                    } else {
                        html.replace("<a action=\"bypass -h npc_%objectId%_Subclass 9 1\">%sub1%</a><br>", "");
                    }

                    if (player.getSubClasses().containsKey(2)) {
                        html.replace("%sub2%", ClassListData.getInstance()
                                .getClassNameById(player.getSubClasses().get(2).getClassId()));
                    } else {
                        html.replace("<a action=\"bypass -h npc_%objectId%_Subclass 9 2\">%sub2%</a><br>", "");
                    }

                    if (player.getSubClasses().containsKey(3)) {
                        html.replace("%sub3%", ClassListData.getInstance()
                                .getClassNameById(player.getSubClasses().get(3).getClassId()));
                    } else {
                        html.replace("<a action=\"bypass -h npc_%objectId%_Subclass 9 3\">%sub3%</a><br>", "");
                    }
                    break;
                case 9: // Make Dual Class - Action
                    if (paramOne < 1 || paramOne > maxSubs) {
                        return;
                    }

                    SubClass subClass = player.getSubClasses().get(paramOne);
                    if (subClass == null) {
                        return;
                    }

                    if (subClass.getClassDefinition().getId() == 136) {
                        player.sendMessage("You cannot make Judicator be your dual class!");
                        return;
                    }

                    if (subClass.getLevel() < 80) {
                        player.sendMessage("This subclass is not at level 80!");
                        return;
                    }


                    boolean hasDual = false;
                    for (SubClass sub : player.getSubClasses().values()) {
                        if (sub.isDualClass()) {
                            hasDual = true;
                            break;
                        }
                    }

                    if (hasDual) {
                        player.sendMessage("You already have a dual class!");
                        return;
                    }

                    subClass.setDualClassActive(true);
                    if (Config.STARTING_LEVEL > subClass.getLevel()) {
                        byte level = Config.STARTING_LEVEL;
                        if (level > subClass.getMaxLevel()) {
                            level = subClass.getMaxLevel();
                        }

                        subClass.setLevel(level);
                        subClass.setExp(ExperienceData.getInstance().getExpForLevel(level));
                        player.broadcastUserInfo();
                    }

                    player.sendPacket(new ExSubjobInfo(player, SubclassInfoType.NEW_SLOT_USED));

                    content += "Make Dual Class:<br>Your subclass is now a <font color=\"LEVEL\">dual class</font>.";

                    player.sendMessage("Dual class created!"); // Subclass added.
                    break;
            }

            content += "</body></html>";

            // If the content is greater than for a basic blank page,
            // then assume no external HTML file was assigned.
            if (content.length() > 26) {
                html.setHtml(content);
            }

            html.replace("%objectId%", String.valueOf(getObjectId()));
            player.sendPacket(html);
        } else if (command.startsWith("changerace")) {
            int templateId = Integer.parseInt(cmdParams);
            if (templateId < 0) {
                if (player.getRaceAppearance() < 0) {
                    player.sendMessage("Special Services: You don't have any race appearance to restore!");
                    return;
                }

                player.setRaceAppearance(templateId);
                player.broadcastUserInfo();
                player.sendMessage("Special Services: Your race appearance has been restored.");
                return;
            }

            if (player.getRaceAppearance() == templateId) {
                player.sendMessage("Special Services: You already have this race appearance!");
                return;
            }

            if (player.getRace() == Race.KAMAEL && ClassListData.getInstance().getClass(player.getClassId()).getClassId().level() < 3 ||
                    player.getRace() == Race.ERTHEIA) {
                player.sendMessage("Special Services: Sorry, but I can't change your race appearance!");
                return;
            }

            PlayerTemplate temp = PlayerTemplateData.getInstance().getTemplate(templateId);
            if (temp == null || temp.getRace() == Race.DWARF && temp.getClassId().isMage() ||
                    temp.getRace() == Race.ERTHEIA && !(player.getAppearance().getSexType() == Sex.FEMALE)) {
                player.sendMessage("Special Services: Sorry, but I can't change your race appearance!");
                return;
            }

            if (!player
                    .destroyItemByItemId("SpecialServices", 57, 10, player,
                            true)) {
                player.sendMessage("Special Services: You don't have enough coins!");
                return;
            }

            player.setRaceAppearance(templateId);
            player.broadcastUserInfo();

            player.sendMessage("Special Services: You changed your race appearance successfully!");
        } else if (command.equals("RemoveList")) {
        } else if (command.startsWith("Remove ")) {
            int slot = Integer.parseInt(command.substring(7));
            player.removeHenna(slot);
        } else if (actualCommand.equalsIgnoreCase("create_clan")) {
            if (cmdParams.isEmpty()) {
                return;
            }

            ClanTable.getInstance().createClan(player, cmdParams);
        } else if (actualCommand.equalsIgnoreCase("create_academy")) {
            if (cmdParams.isEmpty()) {
                return;
            }

        } else if (actualCommand.equalsIgnoreCase("rename_pledge")) {
            if (cmdParams.isEmpty() || cmdParams2.isEmpty()) {
                return;
            }

        } else if (actualCommand.equalsIgnoreCase("create_royal")) {
            if (cmdParams.isEmpty()) {
                return;
            }

        } else if (actualCommand.equalsIgnoreCase("create_knight")) {
            if (cmdParams.isEmpty()) {
                return;
            }

        } else if (actualCommand.equalsIgnoreCase("assign_subpl_leader")) {
            if (cmdParams.isEmpty()) {
                return;
            }

        } else if (actualCommand.equalsIgnoreCase("create_ally")) {
            if (cmdParams.isEmpty()) {
                return;
            }

            if (!player.isClanLeader()) {
                player.sendPacket(SystemMessageId.ONLY_CLAN_LEADERS_MAY_CREATE_ALLIANCES);
                return;
            }
            player.getClan().createAlly(player, cmdParams);
        } else if (actualCommand.equalsIgnoreCase("dissolve_ally")) {
            if (!player.isClanLeader()) {
                player.sendPacket(SystemMessageId.THIS_FEATURE_IS_ONLY_AVAILABLE_TO_ALLIANCE_LEADERS);
                return;
            }
            player.getClan().dissolveAlly(player);
        } else if (actualCommand.equalsIgnoreCase("dissolve_clan")) {
        } else if (actualCommand.equalsIgnoreCase("change_clan_leader")) {
            if (cmdParams.isEmpty()) {
                return;
            }

        } else if (actualCommand.equalsIgnoreCase("recover_clan")) {
        } else if (actualCommand.equalsIgnoreCase("increase_clan_level")) {
            if (!player.isClanLeader()) {
                player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
                return;
            }
            player.getClan().levelUpClan(player);
        } else if (actualCommand.equalsIgnoreCase("learn_clan_skills")) {
        } else if (command.startsWith("Subclass")) {
            if (EventsManager.getInstance().isPlayerParticipant(player.getObjectId())) {
                player.sendMessage("You can't change sub classes if you are joined in an event.");
                return;
            }

            int cmdChoice = Integer.parseInt(command.substring(9, 10).trim());

            // Subclasses may not be changed while a skill is in use.
            if (player.isCastingNow() || player.isAllSkillsDisabled()) {
                player.sendPacket(SystemMessageId.SUBCLASSES_MAY_NOT_BE_CREATED_OR_CHANGED_WHILE_A_SKILL_IS_IN_USE);
                return;
            }

            String content = "<html><body>";
            NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());

            int paramOne = 0;
            int paramTwo = 0;

            try {
                int endIndex = command.indexOf(' ', 11);
                if (endIndex == -1) {
                    endIndex = command.length();
                }

                paramOne = Integer.parseInt(command.substring(11, endIndex).trim());
                if (command.length() > endIndex) {
                    paramTwo = Integer.parseInt(command.substring(endIndex).trim());
                }
            } catch (Exception ignored) {
            }

            int maxSubs = Config.MAX_SUBCLASS;
            if (player.getRace() == Race.ERTHEIA) {
                maxSubs = 1;
            }
            switch (cmdChoice) {
                case 0: // Subclass change menu
                    html.setFile(player, "villagemaster/SubClass.htm");
                    break;
                case 1: // Add Subclass - Initial

                case 2: // Change Class - Initial
                    content += "Change Subclass:<br>";

                    final int baseClassId = player.getBaseClass();

                    if (player.getSubClasses().isEmpty()) {
                        content += "You can't change sub classes when you don't have a sub class to begin with.<br>";
                    } else {
                        content += "Which class would you like to switch to?<br>";

                        if (baseClassId == player.getActiveClass()) {
                            content += ClassListData.getInstance().getClass(baseClassId).getClassName() +
                                    "&nbsp;<font color=\"LEVEL\">(Base Class)</font><br><br>";
                        } else {
                            content += "<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 5 0\">" +
                                    ClassListData.getInstance().getClass(baseClassId).getClassName() + "</a>&nbsp;" +
                                    "<font color=\"LEVEL\">(Base Class)</font><br><br>";
                        }

                        for (Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext(); ) {
                            SubClass subClass = subList.next();
                            int subClassId = subClass.getClassId();

                            if (subClassId == player.getActiveClass()) {
                                content += ClassListData.getInstance().getClass(baseClassId).getClassName() + "<br>";
                            } else {
                                content += "<a action=\"bypass -h npc_" + getObjectId() + "_Subclass 5 " +
                                        subClass.getClassIndex() + "\">" +
                                        ClassListData.getInstance().getClass(baseClassId).getClassName() + "</a><br>";
                            }
                        }
                    }
                    break;
                case 3: // Change/Cancel Subclass - Initial
                    if (player.getSubClasses() == null || player.getSubClasses().isEmpty()) {
                        html.setFile(player, "villagemaster/SubClass_ModifyEmpty.htm");
                        break;
                    }

                    // custom value
                    if (player.getTotalSubClasses() > 3) {
                        html.setFile(player, "villagemaster/SubClass_ModifyCustom.htm");
                        final StringBuilder content3 = StringUtil.startAppend(200);
                        int classIndex = 1;

                        for (Iterator<SubClass> subList = iterSubClasses(player); subList.hasNext(); ) {
                            SubClass subClass = subList.next();

                            StringUtil.append(content3, "Sub-class ", String.valueOf(classIndex++), "<br>",
                                    "<a action=\"bypass -h npc_%objectId%_Subclass 6 ",
                                    String.valueOf(subClass.getClassIndex()), "\">",
                                    ClassListData.getInstance().getClass(subClass.getClassId()).getClassName(), "</a><br>");
                        }
                        html.replace("%list%", content3.toString());
                    } else {
                        // retail html contain only 3 subclasses
                        html.setFile(player, "villagemaster/SubClass_Modify.htm");
                        if (player.getSubClasses().containsKey(1)) {
                            html.replace("%sub1%", ClassListData.getInstance()
                                    .getClass(player.getSubClasses().get(1).getClassId()).getClassName());
                        } else {
                            html.replace(
                                    "Sub-class 1<br>\n<Button ALIGN=LEFT ICON=\"NORMAL\" action=\"bypass -h npc_%objectId%_Subclass 6 1\">%sub1%</Button>",
                                    "");
                        }

                        if (player.getSubClasses().containsKey(2)) {
                            html.replace("%sub2%", ClassListData.getInstance()
                                    .getClass(player.getSubClasses().get(2).getClassId()).getClassName());
                        } else {
                            html.replace(
                                    "Sub-class 2<br>\n<Button ALIGN=LEFT ICON=\"NORMAL\" action=\"bypass -h npc_%objectId%_Subclass 6 2\">%sub2%</Button>",
                                    "");
                        }

                        if (player.getSubClasses().containsKey(3)) {
                            html.replace("%sub3%", ClassListData.getInstance()
                                    .getClass(player.getSubClasses().get(3).getClassId()).getClassName());
                        } else {
                            html.replace(
                                    "Sub-class 3<br>\n<Button ALIGN=LEFT ICON=\"NORMAL\" action=\"bypass -h npc_%objectId%_Subclass 6 3\">%sub3%</Button>",
                                    "");
                        }
                    }
                    break;
                case 4: // Add Subclass - Action (Subclass 4 x[x])

                case 5: // Change Class - Action
                    /*
                     * If the character is less than level 75 on any of their previously chosen
                     * classes then disallow them to change to their most recently added sub-class choice.
                     *
                     * Note: paramOne = classIndex
                     */

                    if (!player.getFloodProtectors().getSubclass().tryPerformAction("add subclass")) {
                        LOGGER.warning("Player " + player.getName() + " has performed a subclass change too fast");
                        return;
                    }

                    player.setActiveClass(paramOne);

                    content += "Change Subclass:<br>Your active sub class is now a <font color=\"LEVEL\">" +
                            ClassListData.getInstance().getClass(player.getActiveClass()).getClassName() + "</font>.";

                    player.sendPacket(new ExSubjobInfo(player, SubclassInfoType.CLASS_CHANGED));
                    break;
                case 6: // Change/Cancel Subclass - Choice

                case 7: // Change Subclass - Action
                    /*
                     * Warning: the information about this subclass will be removed from the
                     * subclass list even if false!
                     */

                    if (!player.getFloodProtectors().getSubclass().tryPerformAction("add subclass")) {
                        LOGGER.warning("Player " + player.getName() + " has performed a subclass change too fast");
                        return;
                    }


                    if (player.modifySubClass(paramOne, paramTwo, false)) {
                        player.stopAllEffects(); // all effects from old subclass stopped!
                        player.setActiveClass(paramOne);

                        content += "Change Subclass:<br>Your sub class has been changed to <font color=\"LEVEL\">" +
                                ClassListData.getInstance().getClass(paramTwo).getClassName() + "</font>.";


                        player.giveAvailableSkills(true, true, true);
                        player.sendSkillList();

                        player.sendPacket(SystemMessageId.THE_NEW_SUBCLASS_S1_HAS_BEEN_ADDED_CONGRATS); // Subclass added.
                        player.sendPacket(new ExSubjobInfo(player, SubclassInfoType.NEW_SLOT_USED));
                    } else {
                        /*
                         * This isn't good! modifySubClass() removed subclass from memory
                         * we must update _classIndex! Else IndexOutOfBoundsException can turn
                         * up some place down the line along with other seemingly unrelated
                         * problems.
                         */
                        player.setActiveClass(0); // Also updates _classIndex plus switching _classid to baseclass.

                        player.sendMessage(
                                "The sub class could not be added, you have been reverted to your base class.");
                        return;
                    }
                    break;
                case 8: // Make Dual Class - Initial
                    if (player.getSubClasses() == null || player.getSubClasses().isEmpty()) {
                        player.sendMessage("You don't have any subclass!");
                        return;
                    }


                    // retail html contain only 3 subclasses
                    html.setFile(player, "villagemaster/SubClass_MakeDual.htm");
                    if (player.getSubClasses().containsKey(1)) {
                        html.replace("%sub1%", ClassListData.getInstance()
                                .getClass(player.getSubClasses().get(1).getClassId()).getClassName());
                    } else {
                        html.replace("<a action=\"bypass -h npc_%objectId%_Subclass 9 1\">%sub1%</a><br>", "");
                    }

                    if (player.getSubClasses().containsKey(2)) {
                        html.replace("%sub2%", ClassListData.getInstance()
                                .getClass(player.getSubClasses().get(2).getClassId()).getClassName());
                    } else {
                        html.replace("<a action=\"bypass -h npc_%objectId%_Subclass 9 2\">%sub2%</a><br>", "");
                    }

                    if (player.getSubClasses().containsKey(3)) {
                        html.replace("%sub3%", ClassListData.getInstance()
                                .getClass(player.getSubClasses().get(3).getClassId()).getClassName());
                    } else {
                        html.replace("<a action=\"bypass -h npc_%objectId%_Subclass 9 3\">%sub3%</a><br>", "");
                    }
                    break;
                case 9: // Make Dual Class - Action
                    if (paramOne < 1 || paramOne > maxSubs) {
                        return;
                    }

                    SubClass subClass = player.getSubClasses().get(paramOne);
                    if (subClass == null) {
                        return;
                    }

                    if (subClass.getClassDefinition().getId() == 136) {
                        player.sendMessage("You cannot make Judicator be your dual class!");
                        return;
                    }

                    if (subClass.getLevel() < 80) {
                        player.sendMessage("This subclass is not at level 80!");
                        return;
                    }


                    boolean hasDual = false;
                    for (SubClass sub : player.getSubClasses().values()) {
                        if (sub.isDualClass()) {
                            hasDual = true;
                            break;
                        }
                    }

                    if (hasDual) {
                        player.sendMessage("You already have a dual class!");
                        return;
                    }

                    subClass.setDualClassActive(true);
                    if (Config.STARTING_LEVEL > subClass.getLevel()) {
                        byte level = Config.STARTING_LEVEL;
                        if (level > subClass.getMaxLevel()) {
                            level = subClass.getMaxLevel();
                        }

                        subClass.setLevel(level);
                        subClass.setExp(ExperienceData.getInstance().getExpForLevel(level));
                        player.broadcastUserInfo();
                    }

                    player.sendPacket(new ExSubjobInfo(player, SubclassInfoType.CLASS_CHANGED));

                    content += "Make Dual Class:<br>Your subclass is now a <font color=\"LEVEL\">dual class</font>.";

                    player.sendMessage("Dual class created!"); // Subclass added.
                    break;
            }

            content += "</body></html>";

            // If the content is greater than for a basic blank page,
            // then assume no external HTML file was assigned.
            if (content.length() > 26) {
                html.setHtml(content);
            }

            html.replace("%objectId%", String.valueOf(getObjectId()));
            player.sendPacket(html);
        } else if (command.startsWith("WithdrawP")) {
            if (true) {
                String htmFile = "mods/WhSortedP.htm";
                String htmContent = HtmCache.getInstance().getHtm(player, htmFile);
                if (htmContent != null) {
                    NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
                    npcHtmlMessage.setHtml(htmContent);
                    npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
                    player.sendPacket(npcHtmlMessage);
                } else {
                    LOGGER.warning("Missing htm: " + htmFile + " !");
                }
            } else {
            }
        } else if (command.equals("DepositP")) {
        } else if (command.startsWith("WithdrawC")) {
            if (true) {
                String htmFile = "mods/WhSortedC.htm";
                String htmContent = HtmCache.getInstance().getHtm(player, htmFile);
                if (htmContent != null) {
                    NpcHtmlMessage npcHtmlMessage = new NpcHtmlMessage(getObjectId());
                    npcHtmlMessage.setHtml(htmContent);
                    npcHtmlMessage.replace("%objectId%", String.valueOf(getObjectId()));
                    player.sendPacket(npcHtmlMessage);
                } else {
                    LOGGER.warning("Missing htm: " + htmFile + " !");
                }
            } else {
            }
        } else if (command.equals("DepositC")) {
        } else if (command.startsWith("FishSkillList")) {
        } else if (command.startsWith("AbandonCastle")) {
            Clan clan = player.getClan();
            if (clan == null) {
                player.sendMessage("You don't have a clan!");
                return;
            }

            if (!player.isClanLeader()) {
                player.sendMessage("You are not the clan leader from your clan!");
                return;
            }

            Castle castle = CastleManager.getInstance().getCastleByOwner(clan);
            if (castle == null) {
                player.sendMessage("Your clan doesn't have a castle!");
                return;
            }

            Siege siege = CastleManager.getInstance().getCastleByOwner(clan).getSiege();
            if (siege != null && siege.isInProgress()) {
                player.sendMessage("This function can't be used while in siege!");
                return;
            }

            castle.removeOwner(clan);
            Broadcast.toAllOnlinePlayers(clan.getName() + " has abandoned " + castle.getName() + " castle!");
            player.sendMessage("The castle has been abandoned!");
        } else {
            super.onBypassFeedback(player, command);
        }
    }

    private static void showHtmlMenu(PlayerInstance player, int objectId, int level) {
        NpcHtmlMessage html = new NpcHtmlMessage(objectId);
        if (player.getRace() == Race.ERTHEIA && level == 1) {
            level = 2;
        }

        final ClassInfo currentClass = ClassListData.getInstance().getClass(player.getClassId());
        if (currentClass.getClassId().level() >= level) {
            html.setFile(player, "data/html/classmaster/nomore.htm");
        } else {
            final int minLevel = getMinLevel(currentClass.level());
            if (player.getLevel() >= minLevel) {
                final StringBuilder menu = new StringBuilder(100);
                for (ClassInfo cid : ClassListData.getInstance().getClassList().values()) {
                    if (cid.getClassId().getRace() == null) {
                        continue;
                    } else if (cid.getClassId().getRace() == Race.ERTHEIA && player.getLevel() < 40) {
                        continue;
                    }

                    if (validateClass(currentClass.getClassId(), cid.getClassId()) && cid.level() == level) {
                        if (cid.getClassId().getId() != 135) // 135 = Inspector (male + female) - prohibiting Judicator as main class
                        {
                            StringUtil.append(menu, "<a action=\"bypass -h npc_%objectId%_change_class ",
                                    String.valueOf(cid.getClassId().getId()), "\">",
                                    ClassListData.getInstance().getClassNameById(cid.getClassId().getId()), "</a><br>");
                        }
                    }
                }

                if (menu.length() > 0) {
                    html.setFile(player, "data/html/classmaster/template.htm");
                    html.replace("%name%", ClassListData.getInstance().getClassNameById(currentClass.getClassId().getId()));
                    html.replace("%menu%", menu.toString());
                } else {
                    html.setFile(player, "data/html/classmaster/comebacklater.htm");
                    html.replace("%level%", String.valueOf(getMinLevel(level - 1)));
                }
            } else {
                if (minLevel < Integer.MAX_VALUE) {
                    html.setFile(player, "data/html/classmaster/comebacklater.htm");
                    html.replace("%level%", String.valueOf(minLevel));
                } else {
                    html.setFile(player, "data/html/classmaster/nomore.htm");
                }
            }
        }

        html.replace("%objectId%", String.valueOf(objectId));
        player.sendPacket(html);
    }

    private static final Map<CategoryType, Integer> classCloak = new EnumMap<>(CategoryType.class);

    static {
        classCloak.put(CategoryType.SIXTH_SIGEL_GROUP, 30310); // Abelius Cloak
        classCloak.put(CategoryType.SIXTH_TIR_GROUP, 30311); // Sapyros Cloak Grade
        classCloak.put(CategoryType.SIXTH_OTHEL_GROUP, 30312); // Ashagen Cloak Grade
        classCloak.put(CategoryType.SIXTH_YR_GROUP, 30313); // Cranigg Cloak Grade
        classCloak.put(CategoryType.SIXTH_FEOH_GROUP, 30314); // Soltkreig Cloak Grade
        classCloak.put(CategoryType.SIXTH_WYNN_GROUP, 30315); // Naviarope Cloak Grade
        classCloak.put(CategoryType.SIXTH_IS_GROUP, 30316); // Leister Cloak Grade
        classCloak.put(CategoryType.SIXTH_EOLH_GROUP, 30317); // Laksis Cloak Grade
    }


    private static int getCloakId(PlayerInstance player) {
        return classCloak.entrySet().stream().filter(e -> player.isInCategory(e.getKey())).mapToInt(Map.Entry::getValue).findFirst().orElse(0);
    }

    private static boolean checkAndChangeClass(PlayerInstance player, int val) {
        final ClassId currentClassId = player.getClassId();
        if (getMinLevel(currentClassId.level()) > player.getLevel()) {
            return false;
        }

        if (!validateClass(currentClassId, val)) {
            return false;
        }

        player.setClassId(val);

        if (player.isSubClassActive()) {
            player.getSubClasses().get(player.getClassIndex()).setClassId(player.getActiveClass());
        } else {
            player.setBaseClass(player.getActiveClass());
            SkillTreeData.getInstance().cleanSkillUponChangeClass(player, false);
            if (player.isInCategory(CategoryType.SIXTH_CLASS_GROUP)) {
                for (SkillLearn skill : SkillTreeData.getInstance().getRaceSkillTree(player.getRace())) {
                    player.addSkill(SkillData.getInstance().getSkill(skill.getSkillId(), skill.getSkillLevel()), true);
                }
                // 4th change
                if (player.isDualClassActive()) {
                    player.addItem("Class transfer", 37375, 2, null, true);

                } else {
                    player.addItem("Class transfer", 37374, 2, null, true);
                    player.addItem("Class CHange", getCloakId(player), 1, null, true);
                }

            }
            if (Config.AUTO_LEARN_SKILLS) {
                player.giveAvailableSkills(Config.AUTO_LEARN_FS_SKILLS, Config.AUTO_LEARN_FP_SKILLS, true);
            }
            player.store(false); // Save player cause if server crashes before this char is saved, he will lose class.
            player.broadcastUserInfo();
            player.sendSkillList();
            player.sendPacket(new PlaySound("ItemSound.quest_fanfare_2"));
        }
        player.broadcastUserInfo();

        return true;
    }


    private boolean isValidNewSubClass(PlayerInstance player, int classId) {
        final ClassId cid = ClassId.getClassId(classId);
        ClassId subClassId;
        for (SubClass subList : player.getSubClasses().values()) {
            subClassId = ClassId.getClassId(subList.getClassId());
            if (subClassId.equalsOrChildOf(cid)) {
                return false;
            }
        }

        // get player base class
        final int currentBaseId = player.getBaseClass();
        final ClassId baseCID = ClassId.getClassId(currentBaseId);

        // we need 2nd occupation ID
        final int baseClassId = (CategoryData.getInstance().isInCategory(CategoryType.FOURTH_CLASS_GROUP, baseCID.getId()) || CategoryData.getInstance().isInCategory(CategoryType.FIFTH_CLASS_GROUP, baseCID.getId()) || CategoryData.getInstance().isInCategory(CategoryType.SIXTH_CLASS_GROUP, baseCID.getId())) ? baseCID.getParent().getId() : currentBaseId;
        final Set<ClassId> availSubs = getSubclasses(player, baseClassId);
        if ((availSubs == null) || availSubs.isEmpty()) {
            return false;
        }

        boolean found = false;
        for (ClassId pclass : availSubs) {
            if (pclass.getId() == classId) {
                found = true;
                break;
            }
        }
        return found;
    }

    /**
     * Returns minimum player level required for next class transfer
     *
     * @param level - current skillId level (0 - start, 1 - first, etc)
     */
    private static int getMinLevel(int level) {
        switch (level) {
            case 0:
                return 20;
            case 1:
                return 40;
            case 2:
                return 76;
            case 3:
                return 85;
            default:
                return Integer.MAX_VALUE;
        }
    }

    /**
     * Returns true if class change is possible
     *
     * @param oldC current player ClassId
     * @param val  new class index
     * @return
     */
    private static boolean validateClass(ClassId oldC, int val) {
        try {
            return validateClass(oldC, ClassListData.getInstance().getClass(val).getClassId());
        } catch (Exception e) {
            // possible ArrayOutOfBoundsException
        }
        return false;
    }


    Set<ClassId> getAvailableSubClasses(PlayerInstance player) {
        final int currentBaseId = player.getBaseClass();
        final ClassId baseCID = ClassId.getClassId(currentBaseId);
        final int baseClassId = (CategoryData.getInstance().isInCategory(CategoryType.FOURTH_CLASS_GROUP, baseCID.getId()) || CategoryData.getInstance().isInCategory(CategoryType.FIFTH_CLASS_GROUP, baseCID.getId()) || CategoryData.getInstance().isInCategory(CategoryType.SIXTH_CLASS_GROUP, baseCID.getId())) ? baseCID.getParent().getId() : currentBaseId;
        final Set<ClassId> availSubs = getSubclasses(player, baseClassId);
        if ((availSubs != null) && !availSubs.isEmpty()) {
            for (ClassId pclass : availSubs) {
                // scan for already used subclasses
                final int availClassId = pclass.getId();
                final ClassId cid = ClassId.getClassId(availClassId);
                for (SubClass subList : player.getSubClasses().values()) {
                    final ClassId subId = ClassId.getClassId(subList.getClassId());
                    if (subId.equalsOrChildOf(cid)) {
                        availSubs.remove(cid);
                        break;
                    }
                }
            }
        }
        return availSubs;
    }

    private static final Set<ClassId> mainSubclassSet;
    private static final Set<ClassId> neverSubclassed = EnumSet.of(ClassId.OVERLORD, ClassId.WARSMITH);
    private static final Set<ClassId> subclasseSet1 = EnumSet.of(ClassId.DARK_AVENGER, ClassId.PALADIN, ClassId.TEMPLE_KNIGHT, ClassId.SHILLIEN_KNIGHT);
    private static final Set<ClassId> subclasseSet2 = EnumSet.of(ClassId.TREASURE_HUNTER, ClassId.ABYSS_WALKER, ClassId.PLAINS_WALKER);
    private static final Set<ClassId> subclasseSet3 = EnumSet.of(ClassId.HAWKEYE, ClassId.SILVER_RANGER, ClassId.PHANTOM_RANGER);
    private static final Set<ClassId> subclasseSet4 = EnumSet.of(ClassId.WARLOCK, ClassId.ELEMENTAL_SUMMONER, ClassId.PHANTOM_SUMMONER);
    private static final Set<ClassId> subclasseSet5 = EnumSet.of(ClassId.SORCERER, ClassId.SPELLSINGER, ClassId.SPELLHOWLER);
    private static final EnumMap<ClassId, Set<ClassId>> subclassSetMap = new EnumMap<>(ClassId.class);

    static {
        final Set<ClassId> subclasses = CategoryData.getInstance().getCategoryByType(CategoryType.THIRD_CLASS_GROUP).stream().map(ClassId::getClassId).collect(Collectors.toSet());
        subclasses.removeAll(neverSubclassed);
        mainSubclassSet = subclasses;
        subclassSetMap.put(ClassId.DARK_AVENGER, subclasseSet1);
        subclassSetMap.put(ClassId.PALADIN, subclasseSet1);
        subclassSetMap.put(ClassId.TEMPLE_KNIGHT, subclasseSet1);
        subclassSetMap.put(ClassId.SHILLIEN_KNIGHT, subclasseSet1);
        subclassSetMap.put(ClassId.TREASURE_HUNTER, subclasseSet2);
        subclassSetMap.put(ClassId.ABYSS_WALKER, subclasseSet2);
        subclassSetMap.put(ClassId.PLAINS_WALKER, subclasseSet2);
        subclassSetMap.put(ClassId.HAWKEYE, subclasseSet3);
        subclassSetMap.put(ClassId.SILVER_RANGER, subclasseSet3);
        subclassSetMap.put(ClassId.PHANTOM_RANGER, subclasseSet3);
        subclassSetMap.put(ClassId.WARLOCK, subclasseSet4);
        subclassSetMap.put(ClassId.ELEMENTAL_SUMMONER, subclasseSet4);
        subclassSetMap.put(ClassId.PHANTOM_SUMMONER, subclasseSet4);
        subclassSetMap.put(ClassId.SORCERER, subclasseSet5);
        subclassSetMap.put(ClassId.SPELLSINGER, subclasseSet5);
        subclassSetMap.put(ClassId.SPELLHOWLER, subclasseSet5);
    }


    public final Set<ClassId> getSubclasses(PlayerInstance player, int classId) {
        Set<ClassId> subclasses = null;
        final ClassId pClass = ClassId.getClassId(classId);
        if (CategoryData.getInstance().isInCategory(CategoryType.THIRD_CLASS_GROUP, classId) || (CategoryData.getInstance().isInCategory(CategoryType.FOURTH_CLASS_GROUP, classId))) {
            subclasses = EnumSet.copyOf(mainSubclassSet);
            subclasses.remove(pClass);

            // Ertheia classes cannot be subclassed and only Kamael can take Kamael classes as subclasses.
            for (ClassId cid : ClassId.values()) {
                if ((cid.getRace() == Race.ERTHEIA) || ((cid.getRace() == Race.KAMAEL) && (player.getRace() != Race.KAMAEL))) {
                    subclasses.remove(cid);
                }
            }

            if (player.getRace() == Race.KAMAEL) {
                if (player.getAppearance().isFemale()) {
                    subclasses.remove(ClassId.MALE_SOULBREAKER);
                } else {
                    subclasses.remove(ClassId.FEMALE_SOULBREAKER);
                }

                if (!player.getSubClasses().containsKey(2) || (player.getSubClasses().get(2).getLevel() < 75)) {
                    subclasses.remove(ClassId.INSPECTOR);
                }
            }

            final Set<ClassId> unavailableClasses = subclassSetMap.get(pClass);
            if (unavailableClasses != null) {
                subclasses.removeAll(unavailableClasses);
            }
        }

        if (subclasses != null) {
            final ClassId currClassId = player.getClassId();
            for (ClassId tempClass : subclasses) {
                if (currClassId.equalsOrChildOf(tempClass)) {
                    subclasses.remove(tempClass);
                }
            }
        }
        return subclasses;
    }

    /**
     * Returns true if class change is possible
     *
     * @param oldC current player ClassId
     * @param newC new ClassId
     * @return true if class change is possible
     */
    private static boolean validateClass(ClassId oldC, ClassId newC) {
        if (newC == null) {
            return false;
        }

        if (oldC.equals(newC.getParent()) || (oldC.getId() == 133 && newC.getId() == 170)) {
            return true;
        }

        return newC.childOf(oldC);
    }

    private Iterator<SubClass> iterSubClasses(PlayerInstance player) {
        return player.getSubClasses().values().iterator();
    }/*
     * Returns list of available subclasses
     * Base class and already used subclasses removed
     */


    private static String formatClassForDisplay(ClassInfo className) {
        String classNameStr = className.getClassName();
        char[] charArray = classNameStr.toCharArray();

        for (int i = 1; i < charArray.length; i++) {
            if (Character.isUpperCase(charArray[i])) {
                classNameStr = classNameStr.substring(0, i) + " " + classNameStr.substring(i);
            }
        }

        return classNameStr;
    }


}
