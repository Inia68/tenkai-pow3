package scripts.handlers.itemhandlers;

import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.instancemanager.GrandBossManager;
import org.l2jmobius.gameserver.instancemanager.InstanceManager;
import org.l2jmobius.gameserver.instancemanager.ZoneManager;
import org.l2jmobius.gameserver.model.actor.Playable;
import org.l2jmobius.gameserver.model.actor.instance.PlayerInstance;
import org.l2jmobius.gameserver.model.items.instance.ItemInstance;
import org.l2jmobius.gameserver.model.zone.ZoneId;
import org.l2jmobius.gameserver.taskmanager.AttackStanceTaskManager;

public class MagicGem implements IItemHandler {

    @Override
    public boolean useItem(Playable playable, ItemInstance item, boolean forceUse) {
        if (!(playable instanceof PlayerInstance)) {
            return false;
        }

        PlayerInstance player = (PlayerInstance) playable;

        if (!player.getFloodProtectors().getMagicGem().tryPerformAction("Magic Gem")) {
            return false;
        }

        if (player.getInstanceId() == 0 &&
                !player.isInsideZone(ZoneId.PVP) &&
                (!player.isInsideZone(ZoneId.NO_SUMMON_FRIEND) ||
                        player.getEvent() == null &&
                !player.isInOlympiadMode() && !AttackStanceTaskManager.getInstance().hasAttackStanceTask(player) &&
                InstanceManager.getInstance().getInstance(player.getObjectId()) == null && player.getPvpFlag() == 0)){
            player.spawnServitors();
            player.sendMessage("You use a Magic Gem.");
        } else {
            player.sendMessage("You cannot use a Magic Gem right now.");
        }
        return true;
    }
}
