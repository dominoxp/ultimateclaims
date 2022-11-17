package com.songoda.ultimateclaims.listeners;

import com.songoda.ultimateclaims.settings.Settings;
import org.bukkit.block.Block;

public class DominoHelper {
    public static boolean isModeratedWorld(String worldName){
        return !Settings.DISABLED_WORLDS.getStringList()
            .contains(worldName);
    }

    public static boolean isModeratedWorld(Block block){
        return isModeratedWorld(block.getWorld().getName());
    }
}
