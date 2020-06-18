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

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.impl.NpcData;
import org.l2jmobius.gameserver.data.xml.impl.SkillData;
import org.l2jmobius.gameserver.enums.InstanceType;
import org.l2jmobius.gameserver.events.instanced.types.LuckyChests;
import org.l2jmobius.gameserver.model.actor.Creature;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.model.skills.Skill;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;

/**
 * This class manages all chest.
 * @author Julian
 */
public class ChestInstance extends MonsterInstance
{
	private volatile boolean _specialDrop;
	
	public ChestInstance(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.ChestInstance);
		setRandomWalking(false);
		_specialDrop = false;
	}

	@Override
	public boolean doDie(Creature killer) {
		int id = getTemplate().getId();

		if (id == 900911 && killer instanceof PlayerInstance && ((PlayerInstance) killer).getEvent() instanceof LuckyChests) {
			int prize = Rnd.get(100);
			PlayerInstance player = (PlayerInstance) killer;
			LuckyChests event = (LuckyChests) player.getEvent();
			MagicSkillUse MSU;
			Skill skill;
			if (prize == 0) {
				MSU = new MagicSkillUse(player, player, 2025, 1, 1, 0);
				skill = SkillData.getInstance().getSkill(2025, 1);
				player.sendPacket(MSU);
				player.broadcastPacket(MSU);
				player.useMagic(skill, null, false, false);
				event.chestPoints(player, 20);
			} else if (prize < 5) {
				MSU = new MagicSkillUse(player, player, 2024, 1, 1, 0);
				skill = SkillData.getInstance().getSkill(2024, 1);
				player.sendPacket(MSU);
				player.broadcastPacket(MSU);
				player.useMagic(skill, null,false, false);
				event.chestPoints(player, 5);
			} else if (prize < 25) {
				MSU = new MagicSkillUse(player, player, 2023, 1, 1, 0);
				skill = SkillData.getInstance().getSkill(2023, 1);
				player.sendPacket(MSU);
				player.broadcastPacket(MSU);
				player.useMagic(skill, null,false, false);
				event.chestPoints(player, 1);
			} else if (prize < 35) {
				player.stopAllEffects();
				player.reduceCurrentHp(player.getMaxHp() + player.getMaxCp() + 1, this, null);
				player.sendMessage("The chest contained the death!!!");
			} else if (prize < 45) {
				SkillData.getInstance().getSkill(1069, 1).applyEffects(this, player);
				player.sendMessage("The chest was full of sleeping spores!");
			} else if (prize < 55) {
				SkillData.getInstance().getSkill(92, 1).applyEffects(this, player);
				player.sendMessage("You have been stunned by the flash that this chest did when opened!");
			} else if (prize < 65) {
				SkillData.getInstance().getSkill(495, 10).applyEffects(this, player);
				player.sendMessage("The chest shot a lot of knives!");
			} else if (prize < 75) {
				SkillData.getInstance().getSkill(736, 1).applyEffects(this, player);
				player.sendMessage("The chest was full of poison spores!");
			} else {
				player.sendMessage("The chest is empty..");
			}
		}
		return super.doDie(killer);
	}

	@Override
	public void onSpawn()
	{
		super.onSpawn();
		_specialDrop = false;
		setMustRewardExpSp(true);
	}
	
	public synchronized void setSpecialDrop()
	{
		_specialDrop = true;
	}
	
	@Override
	public void doItemDrop(NpcTemplate npcTemplate, Creature lastAttacker)
	{
		int id = getTemplate().getId();
		if (!_specialDrop)
		{
			if ((id >= 18265) && (id <= 18286))
			{
				id += 3536;
			}
			else if ((id == 18287) || (id == 18288))
			{
				id = 21671;
			}
			else if ((id == 18289) || (id == 18290))
			{
				id = 21694;
			}
			else if ((id == 18291) || (id == 18292))
			{
				id = 21717;
			}
			else if ((id == 18293) || (id == 18294))
			{
				id = 21740;
			}
			else if ((id == 18295) || (id == 18296))
			{
				id = 21763;
			}
			else if ((id == 18297) || (id == 18298))
			{
				id = 21786;
			}
		}
		super.doItemDrop(NpcData.getInstance().getTemplate(id), lastAttacker);
	}
	
	@Override
	public boolean isMovementDisabled()
	{
		return true;
	}
	
	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}
}
