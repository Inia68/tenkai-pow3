/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.l2jmobius.gameserver.instancemanager;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.data.xml.impl.SkillData;
import org.l2jmobius.gameserver.data.xml.impl.SkillTreeData;
import org.l2jmobius.gameserver.enums.InstanceType;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.Summon;
import org.l2jmobius.gameserver.model.actor.instance.NpcInstance;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.entity.Castle;
import org.l2jmobius.gameserver.model.itemcontainer.Inventory;
import org.l2jmobius.gameserver.model.items.Item;
import org.l2jmobius.gameserver.model.items.PlayerItemTemplate;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.model.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanager.AttackStanceTaskManager;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class NpcBufferInstance extends NpcInstance {
	private static final int[] buffs = {14791, 14792, 14793, 14794, 30814, 14788, 14789, 14790, 17175, 17176, 17177};
	
	public NpcBufferInstance(int objectId, NpcTemplate template) {
		super(template);
		
		setInstanceType(InstanceType.L2NpcBufferInstance);
	}
	
	@Override
	public void onBypassFeedback(PlayerInstance player, String command) {
		if (player == null) {
			return;
		}

		if (player.getEvent() != null) {
			player.sendMessage("I can not help you if you are registered for an event");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(player)) {
			player.sendMessage("I can not help you while you are fighting");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (command.startsWith("BuffMe")) {
			if (player.isInCombat() || player.isDead() || player.isInOlympiadMode() || player.getPvpFlag() > 0 ||
					OlympiadManager.getInstance().isRegisteredInComp(player) || player.isPlayingEvent()) {
				player.sendMessage("You can't use this option now!");
				return;
			}
			
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			int skillId = Integer.valueOf(st.nextToken());
			
			if (skillId < 4) {
				int[] buffs = {17293, 17292, 17291, 9277, 9276, 9274, 9273};
				for (int id : buffs) {
					giveBuff(player, id);
				}
				

				if (skillId == 1) //Mage
				{
					giveBuff(player, 17296);
				} else if (skillId == 2) //Knight
				{
					giveBuff(player, 17294);
				} else if (skillId == 3) //Warrior
				{
					giveBuff(player, 17295);
				}
			} else {
				giveBuff(player, skillId);
			}
			
			showChatWindow(player, 9);
		} else if (command.startsWith("Buff")) {
			StringTokenizer st = new StringTokenizer(command.substring(5), " ");
			int buffId = Integer.parseInt(st.nextToken());
			int chatPage = Integer.parseInt(st.nextToken());
			int buffLevel = SkillData.getInstance().getMaxLevel(buffId);
			
			Skill skill = SkillData.getInstance().getSkill(buffId, buffLevel);
			skill.applyEffects(player, player);
			if (skill != null) {
				player.setCurrentMp(player.getMaxMp());
			}
			
			showChatWindow(player, chatPage);
		} else if (command.startsWith("Heal")) {
			if ((player.isInCombat() || player.getPvpFlag() > 0) && !player.isInsideZone(ZoneId.PEACE)) {
				player.sendMessage("You cannot be healed while engaged in combat.");
				return;
			}
			
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());
			player.setCurrentCp(player.getMaxCp());
			
			for (Summon summon : player.getServitorsAndPets()) {
				summon.setCurrentHp(summon.getMaxHp());
				summon.setCurrentMp(summon.getMaxMp());
				summon.setCurrentCp(summon.getMaxCp());
			}
			
			showChatWindow(player);
		} else if (command.startsWith("RemoveBuffs")) {
			player.stopAllEffectsExceptThoseThatLastThroughDeath();
			showChatWindow(player, 0);
		} else if (command.startsWith("Pet") && player.getPet() == null && player.getServitorsAndPets().isEmpty()) {
		} else if (command.startsWith("PetBuff")) {
			StringTokenizer st = new StringTokenizer(command.substring(8), " ");
			int buffId = Integer.parseInt(st.nextToken());
			int chatPage = Integer.parseInt(st.nextToken());
			int buffLevel = SkillData.getInstance().getMaxLevel(buffId);
			
			Skill skill = SkillData.getInstance().getSkill(buffId, buffLevel);
			if (skill != null) {
				if (player.getPet() != null) {
					player.setCurrentMp(player.getMaxMp());
				}
				for (Summon summon : player.getServitorsAndPets()) {
					player.setCurrentMp(player.getMaxMp());
					skill.applyEffects(summon, summon);
				}
			}
			
			showChatWindow(player, chatPage);
		} else if (command.startsWith("PetHeal")) {
			if (player.getPet() != null) {
				player.getPet().setCurrentHp(player.getPet().getMaxHp());
				player.getPet().setCurrentMp(player.getPet().getMaxMp());
				player.getPet().setCurrentCp(player.getPet().getMaxCp());
			}
			for (Summon summon : player.getServitorsAndPets()) {
				summon.setCurrentHp(summon.getMaxHp());
				summon.setCurrentMp(summon.getMaxMp());
				summon.setCurrentCp(summon.getMaxCp());
			}
			showChatWindow(player, 10);
		} else if (command.startsWith("PetRemoveBuffs")) {
			player.getPet().stopAllEffects();
			showChatWindow(player, 0);
		} else if (command.startsWith("Chat")) {
			showChatWindow(player, Integer.valueOf(command.substring(5)));
		} else {
			super.onBypassFeedback(player, command);
		}
	}
	
	private static void giveBuff(PlayerInstance player, int skillId) {
		if (player == null) {
			return;
		}
		
		boolean buffSelf = true;
		boolean buffSummon = player.getTarget() != player;
		
		if (buffSummon) {
			if (player.getPet() != null) {
				SkillData.getInstance().getSkill(skillId, 1).applyEffects(player.getPet(), player.getPet());
				player.getPet().setCurrentHpMp(player.getPet().getMaxHp(), player.getPet().getMaxMp());
				if (player.getTarget() == player.getPet()) {
					buffSelf = false;
				}
			}
			
			if (player.getServitorsAndPets() != null) {
				for (Summon summon : player.getServitorsAndPets()) {
					if (summon == null) {
						continue;
					}
					
					SkillData.getInstance().getSkill(skillId, 1).applyEffects(summon, summon);
					summon.setCurrentHpMp(summon.getMaxHp(), summon.getMaxMp());
					if (player.getTarget() == summon) {
						buffSelf = false;
					}
				}
			}
		}
		
		if (buffSelf) {
			SkillData.getInstance().getSkill(skillId, 1).applyEffects(player, player);
			player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
		}
	}
	
	public static void buff(Playable character) {
		int type = 2;
		if (character instanceof PlayerInstance) {
			PlayerInstance player = (PlayerInstance) character;
			if (!player.isMageClass()) {
				ItemInstance shield = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
				if (shield != null && shield.getItem().getType1() == Item.TYPE1_SHIELD_ARMOR) {
					type = 0;
				} else {
					type = 1;
				}
			}
		} else {
			type = 1;
		}
		
		for (int buff : buffs) {
			if (buff == 17296 && type != 0 || buff == 17294 && type != 1 || buff == 17295 && type != 2) {
				continue;
			}
			
			SkillData.getInstance().getSkill(buff, 1).applyEffects(character, character);
			character.setCurrentMp(character.getMaxMp());
		}
	}
	
	@Override
	public void showChatWindow(PlayerInstance player, int val) {
		if (val >= 10 && player.getPet() == null && player.getServitorsAndPets().isEmpty()) {
			val = 0;
		}
		// Send a Server->Client NpcHtmlMessage containing the text of the NpcInstance to the Player
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		String filename = getHtmlPath(getId(), val, player);
		html.setFile(player, filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		player.sendPacket(html);
		
		// Send a Server->Client ActionFailed to the Player in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}

}
