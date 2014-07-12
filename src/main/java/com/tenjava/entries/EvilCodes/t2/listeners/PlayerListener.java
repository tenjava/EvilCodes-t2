package com.tenjava.entries.EvilCodes.t2.listeners;

import com.tenjava.entries.EvilCodes.t2.handlers.DatabaseHandler;
import com.tenjava.entries.EvilCodes.t2.handlers.FilesHandler;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PlayerListener implements Listener {

    public static final ArrayList<Player> minusStr2 = new ArrayList<Player>();
    public static final ArrayList<Player> minusStr1 = new ArrayList<Player>();

    private static final Material[] findableItems = { Material.WOOD_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLD_SWORD, //SWORDS
            Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,  //LEATHER
            Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS, //IRON
            Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS, //CHAIN
            Material.GOLD_HELMET, Material.GOLD_CHESTPLATE, Material.GOLD_LEGGINGS, Material.GOLD_BOOTS, //GOLD
            Material.APPLE, Material.PUMPKIN_PIE, Material.PUMPKIN, Material.MELON, Material.MELON_BLOCK, Material.BREAD, Material.MUSHROOM_SOUP, //FOOD
            Material.RED_MUSHROOM, Material.BROWN_MUSHROOM, Material.POTATO, Material.BAKED_POTATO, Material.COOKED_BEEF, Material.COOKED_CHICKEN,  //FOOD
            Material.COOKED_FISH, Material.COOKIE, Material.CAKE, Material.CARROT_ITEM, Material.GOLDEN_APPLE, //FOOD
            Material.WOOD_AXE, Material.STONE_AXE, Material.IRON_AXE, //AXES
            Material.FISHING_ROD, Material.EXP_BOTTLE, Material.BOW, Material.ARROW }; //MISC



    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent e) {
        final Player player = e.getPlayer();
        if (!DatabaseHandler.userExists(player)) {
            DatabaseHandler.insert(player);
            player.setExp(0.99f);
        }
        player.setGameMode(GameMode.SURVIVAL);
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent e) {
        final Player player = e.getPlayer();
        DatabaseHandler.getConnection().execute("UPDATE `" + DatabaseHandler.getPrefix() + "player` SET `energy` = '" + player.getExp() + "' WHERE `uuid` = '" + player.getUniqueId().toString() + "';");
    }

    @EventHandler
    public void onEntityDeath(final EntityDeathEvent e) {
        if (e.getEntityType() != EntityType.PLAYER) {
            if (e.getEntity().getKiller() != null) {
                if (e.getEntity().getKiller().getType() == EntityType.PLAYER) {
                    final Player killer = e.getEntity().getKiller();
                    final LivingEntity entity = e.getEntity();
                    if (entity.getType() == EntityType.ZOMBIE || entity.getType() == EntityType.CREEPER || entity.getType() == EntityType.SKELETON || entity.getType() == EntityType.SPIDER || entity.getType() == EntityType.ENDERMAN) {
                        DatabaseHandler.increaseValue("mobkills", killer);
                        killer.setExp(killer.getExp() + 0.05f);
                    }
                    killer.setLevel(DatabaseHandler.getValue("mobkills", killer));
                }
            }
        }
        e.setDroppedExp(0);
    }

    @EventHandler
    public void onExpChange(final PlayerExpChangeEvent e) {
        e.setAmount(0);
    }

    @EventHandler
    public void onPlayerDeath(final PlayerDeathEvent e) {
        final Player player = e.getEntity();
        DatabaseHandler.increaseValue("deaths", player);
    }

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent e) {
        if (e.getClickedBlock() != null) {
            if (e.getClickedBlock().getType().equals(Material.CHEST)) {
                e.setCancelled(true);
                //Generate new chest
                final Inventory inv = Bukkit.createInventory(null, 3 * 9, "RewardChest");
                final List<ItemStack> inchestitems = new ArrayList<ItemStack>();
                final Random r = new Random();
                final int num = r.nextInt(10);
                for (int i = 0; i < num; i++) {
                    final Material itemmat = findableItems[r.nextInt(findableItems.length)];
                    final ItemStack item = new ItemStack(itemmat, 1);
                    inchestitems.add(item);
                }
                for (final ItemStack item : inchestitems) {
                    inv.setItem(r.nextInt(inv.getSize() - 1), item);
                }
                e.getPlayer().openInventory(inv);

            }
        }
    }

    @EventHandler
    public void onPlayerLevelUp(final PlayerLevelChangeEvent e) {
        e.getPlayer().setLevel(DatabaseHandler.getValue("mobkills", e.getPlayer()));
    }

    @EventHandler
    public void onCreatureSpawn(final CreatureSpawnEvent e) {
        final Entity entity = e.getEntity();
        if (entity.getType() == EntityType.PIG || entity.getType() == EntityType.COW || entity.getType() == EntityType.CHICKEN || entity.getType() == EntityType.SHEEP) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onHungerLevelChange(final FoodLevelChangeEvent e) {
        e.setCancelled(true);
        e.setFoodLevel(20);
    }

    @EventHandler
    public void onEntityDamageByEnity(final EntityDamageByEntityEvent e) {
        if (e.getEntity().getType() == EntityType.PLAYER) {
            if (e.getDamager().getType() == EntityType.PLAYER) {
                if (!FilesHandler.getConfig().getBoolean("pvp"))
                    e.setCancelled(true);
            }
        } else {
            if (e.getDamager().getType() == EntityType.PLAYER) {
                final Player damager = (Player) e.getDamager();
                if (minusStr2.contains(damager))
                    e.setDamage(e.getDamage() / 3);
                else if (minusStr1.contains(damager))
                    e.setDamage(e.getDamage() / 2);
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(final PlayerRespawnEvent e) {
        e.getPlayer().setExp(0.9f);
    }
}
