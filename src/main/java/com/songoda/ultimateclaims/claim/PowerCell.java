package com.songoda.ultimateclaims.claim;

import static com.songoda.core.utils.NumberUtils.formatWithSuffix;
import static com.songoda.ultimateclaims.claim.CostEquation.LINEAR;
import static com.songoda.ultimateclaims.settings.Settings.COST_EQUATION;
import static com.songoda.ultimateclaims.settings.Settings.ECONOMY_VALUE;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.hooks.EconomyManager;
import com.songoda.core.hooks.HologramManager;
import com.songoda.core.utils.NumberUtils;
import com.songoda.ultimateclaims.UltimateClaims;
import com.songoda.ultimateclaims.gui.PowerCellGui;
import com.songoda.ultimateclaims.settings.Settings;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PowerCell {

    // This is the unique identifier for this power cell.
    // It is reset on every plugin load.
    // Used for holograms.
    private final UUID uniqueId = UUID.randomUUID();

    protected final Claim claim;
    protected final UltimateClaims plugin = UltimateClaims.getInstance();

    protected Location location = null;

    protected List<ItemStack> items = new ArrayList<>();

    protected int currentPower = Settings.STARTING_POWER.getInt();

    protected double economyBalance = 0;
    protected PowerCellGui opened = null;

    public PowerCell(Claim claim) {
        this.claim = claim;
    }

    public int tick() {
        boolean loaded = false;
        if (location != null && location.getWorld() != null) {
            int x = location.getBlockX() >> 4;
            int z = location.getBlockZ() >> 4;

            loaded = location.getWorld().isChunkLoaded(x, z);
        }

        if (location != null) {
            updateItemsFromGui();

            int oldPower = currentPower;
            ListIterator<ItemStack> iterator = items.listIterator();
            while (iterator.hasNext()) {
                ItemStack itemStack = iterator.next();
                double itemValue = plugin.getItemManager().getItemValue(itemStack) * itemStack.getAmount();

                iterator.remove();
                this.currentPower += itemValue;

                if (loaded && Settings.POWERCELL_HOLOGRAMS.getBoolean())
                    updateHologram();
            }

            //Update database if value has changed
            if(currentPower != oldPower){
                plugin.getDataManager().updateClaim(claim);
            }
            return this.currentPower;
        }
        stackItems();
        return this.currentPower;
    }

    public void rejectUnusable() {
        if (location == null)
            return;
        // list of items in the inventory that are worthless and removed from our inventory
        List<ItemStack> rejects = new ArrayList<>();
        for (int i = items.size() - 1; i >= 0; i--) {
            final ItemStack item = items.get(i);
            if (item != null && plugin.getItemManager().getItems().stream().noneMatch(powerCellItem -> powerCellItem.isSimilar(item)))
                rejects.add(items.remove(i));
        }

        if (!rejects.isEmpty()) {
            // YEET
            updateGuiInventory();
            rejects.stream().filter(item -> item.getType() != CompatibleMaterial.AIR.getMaterial())
                    .forEach(item -> location.getWorld().dropItemNaturally(location, item));
        }
    }

    public void updateGuiInventory() {
        if (opened != null)
            opened.updateGuiInventory(items);
    }

    public void updateItemsFromGui() {
        updateItemsFromGui(false);
    }

    public void updateItemsFromGui(boolean force) {
        if (!isInventoryOpen()
                && !force) return;
        List<ItemStack> items = new ArrayList<>();
        for (int i = 10; i < 44; i++) {
            if (i == 17
                    || i == 18
                    || i == 26
                    || i == 27
                    || i == 35
                    || i == 36) continue;
            ItemStack item = opened.getItem(i);
            if (item != null && item.getType() != CompatibleMaterial.AIR.getMaterial())
                items.add(item);
        }
        setItems(items);
    }

    public boolean isInventoryOpen() {
        return opened != null
                && opened.getInventory() != null
                && !opened.getInventory().getViewers().isEmpty();
    }

    public void createHologram() {
        if (location == null) {
            return;
        }

        if (!HologramManager.isHologramLoaded(getHologramId())) {
            HologramManager.createHologram(getHologramId(), location, getTimeRemaining());
        }
    }

    public void updateHologram() {
        if (location == null) {
            return;
        }

        if (HologramManager.isHologramLoaded(getHologramId())) {
            HologramManager.updateHologram(getHologramId(), getTimeRemaining());
        }
    }

    public String getTimeRemaining() {
        return plugin.getLocale().getMessage("general.claim.powercell")
            .processPlaceholder("power", formatWithSuffix(getTotalPower()))
            .getMessage();
    }

    public void removeHologram() {
        if (HologramManager.isHologramLoaded(getHologramId())) {
            HologramManager.removeHologram(getHologramId());
        }
    }

    public int getCurrentPower() {
        return currentPower;
    }

    public void setCurrentPower(int currentPower) {
        this.currentPower = currentPower;
        tick();

        if (this.plugin.getDynmapManager() != null) {
            this.plugin.getDynmapManager().refreshDescription(this.claim);
        }
    }

    public void setEconomyBalance(double economyBalance) {
        this.economyBalance = economyBalance;
    }

    public long getTotalPower() {
        return getItemPower() + (long) getEconomyPower() + currentPower;
    }

    public long getItemPower() {
        updateItemsFromGui();
        double total = 0;
        for (ItemStack itemStack : items) {
            double itemValue = itemStack.getAmount() * plugin.getItemManager().getItemValue(itemStack);

            switch (getCostEquation()) {
                case DEFAULT:
                    total += itemValue / claim.getClaimSize();
                    break;
                case LINEAR:
                    total += itemValue / (claim.getClaimSize() * getLinearValue());
                    break;
                default:
                    total += itemValue;
            }
        }
        return (int) total;
    }

    // Must not be ran if this inventory is open.
    public void stackItems() {
        List<Integer> removed = new ArrayList<>();
        List<ItemStack> newItems = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            ItemStack item = items.get(i);
            CompatibleMaterial material = CompatibleMaterial.getMaterial(item);

            if (removed.contains(i))
                continue;

            ItemStack newItem = item.clone();
            newItems.add(newItem);
            removed.add(i);

            if (item.getAmount() >= item.getMaxStackSize())
                continue;

            for (int j = 0; j < items.size(); j++) {
                ItemStack second = items.get(j);

                if (newItem.getAmount() > newItem.getMaxStackSize())
                    break;

                if (item.getAmount() >= second.getMaxStackSize()
                        || removed.contains(j)
                        || CompatibleMaterial.getMaterial(second) != material
                        || !second.isSimilar(item))
                    continue;

                if (item.getAmount() + second.getAmount() > item.getMaxStackSize()) {
                    second.setAmount(newItem.getAmount() + second.getAmount() - newItem.getMaxStackSize());
                    newItem.setAmount(newItem.getMaxStackSize());
                } else {
                    removed.add(j);
                    newItem.setAmount(newItem.getAmount() + second.getAmount());
                }
            }
        }
        items = newItems;
    }

    public double getEconomyBalance() {
        return this.economyBalance;
    }

    public double getEconomyPower() {
        return economyBalance / getEconomyValue();
    }

    public double getEconomyValue() {
        double value = ECONOMY_VALUE.getDouble();

        switch (getCostEquation()) {
            case DEFAULT:
                return value * claim.getClaimSize();
            case LINEAR:
                return value * (claim.getClaimSize() * getLinearValue());
            default:
                return value;
        }
    }

    private CostEquation getCostEquation() {
        if (COST_EQUATION.getString().startsWith("LINEAR")) return LINEAR;
        else return CostEquation.valueOf(COST_EQUATION.getString());
    }

    private double getLinearValue() {
        if (getCostEquation() != LINEAR) return 1.0d;
        String[] equationSplit = COST_EQUATION.getString().split(" ");
        return Double.parseDouble(equationSplit[1]);
    }

    public List<ItemStack> getItems() {
        return new ArrayList<>(this.items);
    }

    public void setItems(List<ItemStack> items) {
        this.items = items;
    }

    public boolean addItem(ItemStack item) {
        if (items.size() >= 28) return false;
        this.items.add(item);
        return true;
    }

    public void addEconomy(double amount) {
        this.economyBalance += amount;
    }

    public void removeEconomy(double amount) {
        this.economyBalance -= amount;
    }

    public void clearItems() {
        this.items.clear();
    }

    public Location getLocation() {
        return location == null ? null : location.clone();
    }

    public boolean hasLocation() {
        return location != null;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public PowerCellGui getGui(Player player) {
        if (opened != null && opened.isOpen()) {
            opened.close();
            updateItemsFromGui(true);
            stackItems();
        }

        return opened = new PowerCellGui(UltimateClaims.getInstance(), this.claim, player);
    }

    public void destroy() {
        if (location != null && location.getWorld() != null) {
            getItems().stream().filter(Objects::nonNull)
                    .forEach(item -> location.getWorld().dropItemNaturally(location, item));
            removeHologram();

            OfflinePlayer owner = claim.getOwner().getPlayer();
            EconomyManager.deposit(owner, economyBalance);
            if (owner.isOnline())
                owner.getPlayer().sendMessage(plugin.getLocale().getMessage("event.powercell.destroyed")
                        .processPlaceholder("balance", economyBalance).getPrefixedMessage());
        }
        this.economyBalance = 0;
        this.items.clear();
        if (opened != null)
            opened.exit();
        this.opened = null;
        this.clearItems();
        this.location = null;
    }

    public String getHologramId() {
        return "UltimateClaims-" + uniqueId;
    }

    public void removePower(int power) {
        setCurrentPower(currentPower - power);
    }

    public void addCurrentPower(int power) {
        setCurrentPower(currentPower + power);
    }
}
