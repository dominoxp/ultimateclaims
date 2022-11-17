package com.songoda.ultimateclaims.listeners;

import static com.songoda.ultimateclaims.listeners.DominoHelper.isModeratedWorld;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.compatibility.ServerVersion;
import com.songoda.ultimateclaims.UltimateClaims;
import com.songoda.ultimateclaims.claim.Claim;
import com.songoda.ultimateclaims.claim.ClaimManager;
import com.songoda.ultimateclaims.claim.ClaimSetting;
import com.songoda.ultimateclaims.claim.PowerCell;
import com.songoda.ultimateclaims.member.ClaimMember;
import com.songoda.ultimateclaims.member.ClaimPerm;
import com.songoda.ultimateclaims.member.ClaimRole;
import com.songoda.ultimateclaims.settings.Settings;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class BlockListeners implements Listener {

    private final UltimateClaims plugin;

    public BlockListeners(UltimateClaims plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ClaimManager claimManager = plugin.getClaimManager();

        Block block = event.getBlock();

        Chunk chunk = block.getChunk();

        if (!claimManager.hasClaim(chunk)) {
            if (isModeratedWorld(event.getBlock()) && !event.getPlayer().hasPermission("ultimateclaims.world_protection.place")) {
                event.setCancelled(true);
            }
            return;
        }

        Claim claim = claimManager.getClaim(chunk);

        PowerCell powerCell = claim.getPowerCell();

        if (powerCell.getLocation() != null) {
            Block blockPowerCell = powerCell.getLocation().getBlock();

            if (block.getType() == CompatibleMaterial.CHEST.getMaterial() && (block.getRelative(BlockFace.NORTH).equals(blockPowerCell)
                    || block.getRelative(BlockFace.SOUTH).equals(blockPowerCell)
                    || block.getRelative(BlockFace.EAST).equals(blockPowerCell)
                    || block.getRelative(BlockFace.WEST).equals(blockPowerCell))) {
                event.setCancelled(true);
                return;
            }
        }

        if (!claim.playerHasPerms(event.getPlayer(), ClaimPerm.PLACE)) {
            plugin.getLocale().getMessage("event.general.nopermission").sendPrefixedMessage(event.getPlayer());
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        ClaimManager claimManager = plugin.getClaimManager();

        Block block = event.getBlock();

        Chunk chunk = event.getBlock().getChunk();

        // Prevent outside claim modification to world, if this world is protected
        if (!claimManager.hasClaim(chunk)) {
            if (isModeratedWorld(event.getBlock()) && !event.getPlayer().hasPermission("ultimateclaims.world_protection.break")) {
                event.setCancelled(true);
            }
            return;
        }

        Claim claim = claimManager.getClaim(chunk);
        PowerCell powerCell = claim.getPowerCell();

        if (!claim.playerHasPerms(event.getPlayer(), ClaimPerm.BREAK)) {
            plugin.getLocale().getMessage("event.general.nopermission")
                .sendPrefixedMessage(event.getPlayer());
            event.setCancelled(true);
            return;
        }

        if (powerCell.hasLocation() && powerCell.getLocation().equals(block.getLocation())) {
            ClaimMember member = claim.getMember(event.getPlayer());
            if ((member != null && member.getRole() == ClaimRole.OWNER) || event.getPlayer().hasPermission("ultimateclaims.admin.removeclaim")) {
                powerCell.destroy();
            } else {
                plugin.getLocale().getMessage("event.general.nopermission").sendPrefixedMessage(event.getPlayer());
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void ignite(BlockIgniteEvent event) {
        ClaimManager claimManager = plugin.getClaimManager();

        Claim claim = claimManager.getClaim(event.getBlock().getChunk());
        if (isModeratedWorld(event.getBlock()) || (claim != null && !claim.getClaimSettings().isEnabled(ClaimSetting.FIRE_SPREAD))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void ignite(BlockBurnEvent event) {
        ClaimManager claimManager = plugin.getClaimManager();

        Claim claim = claimManager.getClaim(event.getBlock().getChunk());
        if (isModeratedWorld(event.getBlock()) || (claim != null && !claim.getClaimSettings().isEnabled(ClaimSetting.FIRE_SPREAD))) {
            if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_11)) {
                event.getIgnitingBlock().setType(CompatibleMaterial.AIR.getMaterial());
            } else {
                for (BlockFace bf : new BlockFace[] {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
                    Block b = event.getBlock().getRelative(bf);
                    if (b != null && b.getType() == CompatibleMaterial.FIRE.getMaterial()) {
                        b.setType(CompatibleMaterial.AIR.getMaterial());
                    }
                }
            }
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void decay(LeavesDecayEvent event) {
        ClaimManager claimManager = plugin.getClaimManager();

        Claim claim = claimManager.getClaim(event.getBlock().getChunk());
        if (isModeratedWorld(event.getBlock()) || (claim != null && !claim.getClaimSettings().isEnabled(ClaimSetting.LEAF_DECAY))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHopper(InventoryMoveItemEvent event) {
        ClaimManager claimManager = plugin.getClaimManager();
        ItemStack item = event.getItem();

        if (!(event.getDestination().getHolder() instanceof Chest)) return;

        Chest chest = (Chest) event.getDestination().getHolder();
        Chunk chunk = chest.getLocation().getChunk();

        if (!claimManager.hasClaim(chunk)) return;

        Claim claim = claimManager.getClaim(chunk);
        // hopper in a claim, are we trying to push into a powercell?
        PowerCell powerCell = claim.getPowerCell();

        if (powerCell == null || !powerCell.hasLocation() || !powerCell.getLocation().equals(chest.getLocation())) {
            return;
        }

        // To prevent issues with items getting lost, let's prevent
        // items from getting moved if someone is opening the inventory.
        if (powerCell.isInventoryOpen()) {
            event.setCancelled(true);
            return;
        }

        if (!Settings.ENABLE_HOPPERS.getBoolean()) {
            final Location target = chest.getLocation();
            // Powercells have a different inventory than the chest
            // To help out players a bit, we're just going to not let hoppers do their thing
            if (powerCell.hasLocation() && powerCell.getLocation().equals(target)) {
                // yep, let's not do that
                event.setCancelled(true);
            }
            return;
        }

        boolean isFull = !powerCell.addItem(item);
        if (isFull) {
            event.setCancelled(true);
            return;
        }
        powerCell.rejectUnusable();
        powerCell.stackItems();

        event.getDestination().remove(item);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockFromToEventMonitor(BlockFromToEvent event) {
        // prevent water/lava/egg griefs
        ClaimManager claimManager = plugin.getClaimManager();
        Claim fromClaim = claimManager.getClaim(event.getBlock().getChunk());
        Claim toClaim = claimManager.getClaim(event.getToBlock().getChunk());
        // if we're moving across a claim boundary, cancel the event
        if (fromClaim != null && toClaim != null) {
            if (!fromClaim.equals(toClaim)) {
                event.setCancelled(true);
            }
        } else if (toClaim != null || fromClaim != null) {
            // moving from unclaimed to a claim
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        // don't push into a protected region
        pistonCheck(event, event.getBlocks());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        // don't pull from a protected region
        pistonCheck(event, event.getBlocks());
    }

    void pistonCheck(BlockPistonEvent event, List<Block> blocks) {
        ClaimManager claimManager = plugin.getClaimManager();
        Block piston = event.getBlock();
        final BlockFace dir = event.getDirection();
        final int chunkX = piston.getX() >> 4, chunkZ = piston.getZ() >> 4;
        Claim fromClaim = claimManager.getClaim(piston.getChunk());
        for (Block block : blocks) {
            // only check if this block is in a different chunk, or going into another chunk
            if (block.getX() >> 4 != chunkX || block.getZ() >> 4 != chunkZ) {
                Claim toClaim = claimManager.getClaim(block.getChunk());
                // if we're moving across a claim boundary, cancel the event
                if (fromClaim != null && toClaim != null) {
                    if (!fromClaim.equals(toClaim)) {
                        // different claims!
                        event.setCancelled(true);
                        return;
                    }
                } else if (toClaim != null || fromClaim != null) {
                    // trying to alter another claim
                    event.setCancelled(true);
                    return;
                }
            } else if ((block.getX() + dir.getModX()) >> 4 != chunkX || (block.getZ() + dir.getModZ()) >> 4 != chunkZ) {
                Claim toClaim = claimManager.getClaim(block.getRelative(dir).getChunk());
                // if we're moving across a claim boundary, cancel the event
                if (fromClaim != null && toClaim != null) {
                    if (!fromClaim.equals(toClaim)) {
                        // different claims!
                        event.setCancelled(true);
                        return;
                    }
                } else if (toClaim != null || fromClaim != null) {
                    // trying to alter another claim
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
}
