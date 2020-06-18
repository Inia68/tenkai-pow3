package org.l2jmobius.gameserver.model.actor.instance;

import org.l2jmobius.gameserver.ai.CtrlIntention;
import org.l2jmobius.gameserver.datatables.SpawnTable;
import org.l2jmobius.gameserver.enums.InstanceType;
import org.l2jmobius.gameserver.events.instanced.EventInstance;
import org.l2jmobius.gameserver.events.instanced.EventInstance.EventType;
import org.l2jmobius.gameserver.events.instanced.EventTeam;
import org.l2jmobius.gameserver.events.instanced.types.CaptureTheFlag;
import org.l2jmobius.gameserver.model.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.network.serverpackets.*;


public class EventFlagInstance extends NpcInstance {
	private boolean toDelete = false;
	private EventInstance event = null;
	private EventTeam team = null;
	private EventType type;
	
	public EventFlagInstance(NpcTemplate template) {
		super(template);
		setInstanceType(InstanceType.EventFlagInstance);
	}
	
	public void setEvent(EventInstance event) {
		this.event = event;
		if (event.isType(EventType.CaptureTheFlag)) {
			type = EventType.CaptureTheFlag;
		} else {
			type = EventType.FieldDomination;
		}
	}
	
	@Override
	public void onAction(PlayerInstance player, boolean interact) {
		if (toDelete || !event.isType(type)) {
			deleteMe();
			SpawnTable.getInstance().deleteSpawn(getSpawn(), true);
		} else if (this != player.getTarget()) {
			player.setTarget(this);
			// Send a Server->Client packet ValidateLocation to correct the NpcInstance position and heading on the client
			player.sendPacket(new ValidateLocation(this));
		} else {
			player.sendPacket(new ValidateLocation(this));
			if (!canInteract(player)) {
				// Notify the Player AI with AI_INTENTION_INTERACT
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this);
			} else {
				if (player.getEvent() != null && (event.isType(EventType.CaptureTheFlag) || event.isType(EventType.FieldDomination))) {
					if (event.isType(EventType.CaptureTheFlag) && player.getEvent() == event) {
						((CaptureTheFlag) player.getEvent()).onFlagTouched(player, getEventTeam());
					} else if (event.isType(EventType.FieldDomination) && !player.isCastingNow() && player.getEvent() != null &&
							player.getEvent().getParticipantTeam(player.getObjectId()).getFlagId() != getId()) {
						player.stopMove(null);
						
						int castingMillis = player.isMageClass() ? 5000 : 7000;
						player.broadcastPacket(new MagicSkillUse(player, 1050, 1, castingMillis, 0));
						player.sendPacket(new SetupGauge(player.getObjectId(), 0, castingMillis));
						player.sendMessage("Converting flag...");
						
						// player.setLastSkillCast(SkillData.getInstance().getSkill(1050, 1));
						FlagCastFinalizer fcf = new FlagCastFinalizer(player);
						// player.setSkillCast(ThreadPoolManager.getInstance().scheduleEffect(fcf, castingMillis));
						// player.forceIsCasting(TimeController.getGameTicks() + castingMillis / TimeController.MILLIS_IN_TICK);
					}
				}
			}
		}
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	public EventTeam getEventTeam() {
		return team;
	}
	
	public void setTeam(EventTeam team) {
		this.team = team;
	}
	
	class FlagCastFinalizer implements Runnable {
		private PlayerInstance player;
		
		FlagCastFinalizer(PlayerInstance player) {
			this.player = player;
		}
		
		@Override
		public void run() {
			if (player.isCastingNow()) {
				player.sendPacket(new MagicSkillLaunched(player, 2046, 1));
				player.abortAllSkillCasters();

				/*
				if (player.getEvent() != null && player.getEvent() instanceof FieldDomination && !isToDelete()) {
					if (getTeam() == null) {
						((FieldDomination) player.getEvent()).convertFlag(EventFlagInstance.this,
								player.getEvent().getParticipantTeam(player.getObjectId()),
								player);
					} else {
						((FieldDomination) player.getEvent()).convertFlag(EventFlagInstance.this, null, player);
					}
				}
				*/
			}
		}
	}
	
	public void shouldBeDeleted() {
		toDelete = true;
	}
	
	public boolean isToDelete() {
		return toDelete;
	}
}
