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
package scripts.handlers.voicedcommandhandlers;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.instancemanager.PremiumManager;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.stats.Stat;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

public class MyHiddenStats implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"myhiddenstats"
	};
	
	@Override
	public boolean useVoicedCommand(String command, PlayerInstance player, String target)
	{
		if (command.startsWith("myhiddenstats"))
		{
			final NpcHtmlMessage msg = new NpcHtmlMessage(5);

			String html = "<html>" + "<body>" +
					"<center><table><tr><td><img src=icon.etc_alphabet_h_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_i_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_d_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_d_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_e_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_n_i00 width=32 height=32></td></tr></table></center>" +
					"<center><table><tr><td><img src=icon.etc_alphabet_s_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_t_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_a_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_t_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_s_i00 width=32 height=32></td></tr></table></center><br><br>" +
					"<center><table width=300><tr><td FIXWIDTH=150>Stat</td><td FIXWIDTH=150>Value</td</tr></table><img src=\"L2UI.Squaregray\" width=300 height=1>";

			html += getStatHtm("Shield Def", player.getShldDef(), 0, false, "");
			html += getStatHtm("Dmg Absorbed", player.getStat().getValue(Stat.ABSORB_DAMAGE_PERCENT, 0), 0, false, "%");
			html += getStatHtm("Heal Effect", player.getStat().getValue(Stat.HEAL_EFFECT, 100) - 100, 0, true,
					"%");
			html += getStatHtm("Heals Effect Add", player.getStat().getValue(Stat.HEAL_EFFECT_ADD, 100) - 100, 0, true, "%");
			html += getStatHtm("Momentum Max", player.getStat().getValue(Stat.MAX_MOMENTUM, 100) - 100, 0, true,
					"%");
			html += getStatHtm("Max Targets", player.getStat().getValue(Stat.ATTACK_COUNT_MAX, 1) - 1, 0, true, "");
			html += getStatHtm("P. Skill Power", player.getStat().getValue(Stat.PHYSICAL_SKILL_POWER, 100) - 100, 0,
					true, "%");
			html += getStatHtm("P. Crit Dmg", player.getStat().getValue(Stat.CRITICAL_DAMAGE, 100) - 100, 0, true, "%");
			html += getStatHtm("P. Crit Dmg Recvd", player.getStat().getValue(Stat.DEFENCE_CRITICAL_DAMAGE, 100) - 100, 0, true, "%");
			html += getStatHtm("Skill Crit", player.getStat().getValue(Stat.SKILL_CRITICAL, 100) - 100, 0, true,
					"%");
			html += getStatHtm("Skill Crit Probability", player.getStat().getValue(Stat.SKILL_CRITICAL_PROBABILITY, 100) - 100, 0, true,
					"%");
			html += getStatHtm("M. Skill Power", player.getStat().getValue(Stat.MAGICAL_SKILL_POWER, 100) - 100, 0, true,
					"%");
			html += getStatHtm("M. Crit Dmg", player.getStat().getValue(Stat.MAGIC_CRITICAL_DAMAGE, 100) - 100, 0, true, "%");
			html += getStatHtm("M. Crit Dmg Recvd", player.getStat().getValue(Stat.DEFENCE_MAGIC_CRITICAL_DAMAGE, 100) - 100, 0, true,
					"%");
			html += getStatHtm("Fixed P. Crit Dmg", player.getStat().getValue(Stat.CRITICAL_DAMAGE_ADD, 0), 0, true, "");
			html += getStatHtm("Reflected Dmg", player.getStat().getValue(Stat.REFLECT_DAMAGE_PERCENT, 0), 0, true, "%");
			html += getStatHtm("Reflection Resistance", player.getStat().getValue(Stat.REFLECT_DAMAGE_PERCENT_DEFENSE, 0), 0, true, "%");

			html += getStatHtm("PvP P. Dmg", player.getStat().getValue(Stat.PVP_PHYSICAL_ATTACK_DAMAGE, 100) - 100, 0, true, "%");
			html += getStatHtm("PvP P. Skill Dmg", player.getStat().getValue(Stat.PVP_PHYSICAL_SKILL_DAMAGE, 100) - 100, 0,
					true, "%");
			html += getStatHtm("PvP M. Dmg", player.getStat().getValue(Stat.PVP_MAGICAL_SKILL_DAMAGE, 100) - 100, 0, true, "%");
			html += getStatHtm("PvP P. Dmg Res", player.getStat().getValue(Stat.PVP_PHYSICAL_ATTACK_DEFENCE, 100) - 100, 0, true,
					"%");
			html += getStatHtm("PvP P. Skill Dmg Res", player.getStat().getValue(Stat.PVP_PHYSICAL_SKILL_DEFENCE, 100) - 100, 0,
					true, "%");
			html += getStatHtm("PvP M. Dmg Res", player.getStat().getValue(Stat.PVP_MAGICAL_SKILL_DEFENCE, 100) - 100, 0, true,
					"%");

			html += getStatHtm("PvE P. Dmg", player.getStat().getValue(Stat.PVE_PHYSICAL_ATTACK_DAMAGE, 100) - 100, 0, true, "%");
			html += getStatHtm("PvE P. Skill Dmg", player.getStat().getValue(Stat.PVE_PHYSICAL_SKILL_DAMAGE, 100) - 100, 0,
					true, "%");
			html += getStatHtm("PvE M. Dmg", player.getStat().getValue(Stat.PVE_MAGICAL_SKILL_DAMAGE, 100) - 100, 0, true, "%");
			html += getStatHtm("PvE P. Dmg Res", player.getStat().getValue(Stat.PVE_PHYSICAL_ATTACK_DEFENCE, 100) - 100, 0, true,
					"%");
			html += getStatHtm("PvE P. Skill Dmg Res", player.getStat().getValue(Stat.PVE_PHYSICAL_SKILL_DEFENCE, 100) - 100, 0,
					true, "%");
			html += getStatHtm("PvE M. Dmg Res", player.getStat().getValue(Stat.PVE_MAGICAL_SKILL_DEFENCE, 100) - 100, 0, true,
					"%");
			html += getStatHtm("Resist abnormal debuff", player.getStat().getValue(Stat.RESIST_ABNORMAL_DEBUFF, 100) - 100, 0, true, "%");
			html += getStatHtm("Physical abnormal Res", player.getStat().getValue(Stat.ABNORMAL_RESIST_PHYSICAL, 100) - 100, 0,
					true, "%");
			html += getStatHtm("Magical abnormal Res", player.getStat().getValue(Stat.ABNORMAL_RESIST_MAGICAL, 100) - 100, 0,
					true, "%");
			html += getStatHtm("Immobile Res", player.getStat().getValue(Stat.IMMOBILE_DAMAGE_RESIST, 100) - 100, 0, true, "%");
			html += getStatHtm("Dispell Res", player.getStat().getValue(Stat.RESIST_DISPEL_BUFF, 100) - 100, 0, true, "%");
			html += getStatHtm("absorbDamChance", player.getStat().getValue(Stat.ABSORB_DAMAGE_CHANCE, 100) - 100, 0, true, "%");
			html += getStatHtm("FixedDamageResist", player.getStat().getValue(Stat.REAL_DAMAGE_RESIST, 100) - 100, 0, true, "%");




			html += "</table><br>" + "</body></html>";
			player.sendPacket(new NpcHtmlMessage(0, html));
			
			msg.setHtml(html);
			player.sendPacket(msg);
		}
		else
		{
			return false;
		}
		return true;
	}

	private String getStatHtm(String statName, double statVal, double statDefault, boolean plusIfPositive, String suffix)
	{
		if (statVal == statDefault)
		{
			return "";
		}

		return "<table width=300 border=0><tr><td FIXWIDTH=150>" + statName + ":</td><td FIXWIDTH=150>" +
				(plusIfPositive && statVal >= 0 ? "+" : "") + new DecimalFormat("#0.##").format(statVal) + suffix +
				"</td></tr></table><img src=\"L2UI.Squaregray\" width=300 height=1>";
	}


	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}