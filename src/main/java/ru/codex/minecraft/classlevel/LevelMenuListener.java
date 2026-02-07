package ru.codex.minecraft.classlevel;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class LevelMenuListener implements Listener {
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!LevelMenuUtil.LEVEL_MENU_TITLE.equals(event.getView().getTitle())) {
            return;
        }

        event.setCancelled(true);
    }
}
