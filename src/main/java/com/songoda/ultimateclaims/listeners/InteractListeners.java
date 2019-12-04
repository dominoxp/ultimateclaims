package com.songoda.ultimateclaims.listeners;

import com.songoda.ultimateclaims.UltimateClaims;
import com.songoda.ultimateclaims.claim.Claim;
import com.songoda.ultimateclaims.claim.ClaimManager;
import com.songoda.ultimateclaims.member.ClaimMember;
import com.songoda.ultimateclaims.member.ClaimPerm;
import com.songoda.ultimateclaims.member.ClaimRole;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class InteractListeners implements Listener {

    private UltimateClaims plugin;

    public InteractListeners(UltimateClaims plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        ClaimManager claimManager = UltimateClaims.getInstance().getClaimManager();

        Chunk chunk = event.getClickedBlock().getChunk();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || !claimManager.hasClaim(chunk)) return;

        Claim claim = claimManager.getClaim(chunk);

        boolean canRedstone = isRedstone(event.getClickedBlock()) && claim.playerHasPerms(event.getPlayer(), ClaimPerm.REDSTONE);
        boolean canDoors = isDoor(event.getClickedBlock()) && claim.playerHasPerms(event.getPlayer(), ClaimPerm.DOORS);

        if (canRedstone || canDoors) {
            return;
        } else if (isRedstone(event.getClickedBlock()) && !claim.playerHasPerms(event.getPlayer(), ClaimPerm.REDSTONE)
                || isDoor(event.getClickedBlock()) && !claim.playerHasPerms(event.getPlayer(), ClaimPerm.DOORS)) {
            plugin.getLocale().getMessage("event.general.nopermission").sendPrefixedMessage(event.getPlayer());
            event.setCancelled(true);
            return;
        }

        if (!claim.playerHasPerms(event.getPlayer(), ClaimPerm.INTERACT)) {
            plugin.getLocale().getMessage("event.general.nopermission").sendPrefixedMessage(event.getPlayer());
            event.setCancelled(true);
            return;
        }

        ClaimMember member = claim.getMember(event.getPlayer());

        if (claim.getPowerCell().hasLocation()
                && claim.getPowerCell().getLocation().equals(event.getClickedBlock().getLocation())
                && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if ((member != null && member.getRole() == ClaimRole.OWNER) || event.getPlayer().hasPermission("ultimateclaims.bypass")) {
                plugin.getGuiManager().showGUI(event.getPlayer(), claim.getPowerCell().getGui());
            } else {
                plugin.getLocale().getMessage("event.powercell.failopen").sendPrefixedMessage(event.getPlayer());
            }
            event.setCancelled(true);
        }
    }

    private boolean isDoor(Block block) {
        if (block == null) return false;
        switch (block.getType().name()) {
            case "DARK_OAK_DOOR":
            case "ACACIA_DOOR":
            case "BIRCH_DOOR":
            case "JUNGLE_DOOR":
            case "OAK_DOOR":
            case "SPRUCE_DOOR":
            case "ACACIA_TRAPDOOR":
            case "BIRCH_TRAPDOOR":
            case "DARK_OAK_TRAPDOOR":
            case "IRON_TRAPDOOR":
            case "JUNGLE_TRAPDOOR":
            case "OAK_TRAPDOOR":
            case "SPRUCE_TRAPDOOR":
            case "OAK_FENCE_GATE":
            case "ACACIA_FENCE_GATE":
            case "BIRCH_FENCE_GATE":
            case "DARK_OAK_FENCE_GATE":
            case "JUNGLE_FENCE_GATE":
            case "SPRUCE_FENCE_GATE":
            case "WOODEN_DOOR":
            case "WOOD_DOOR":
            case "TRAP_DOOR":
            case "FENCE_GATE":
                return true;
            default:
                return false;
        }
    }

    private boolean isRedstone(Block block) {
        if (block == null) return false;
        switch (block.getType().name()) {
            case "LEVER":
            case "BIRCH_BUTTON":
            case "ACACIA_BUTTON":
            case "DARK_OAK_BUTTON":
            case "JUNGLE_BUTTON":
            case "OAK_BUTTON":
            case "SPRUCE_BUTTON":
            case "STONE_BUTTON":
            case "WOOD_BUTTON":
                return true;
            default:
                return false;
        }
    }

}