package com.tenjava.entries.EvilCodes.t2.listeners;

import com.tenjava.entries.EvilCodes.t2.TenJava;
import com.tenjava.entries.EvilCodes.t2.handlers.DatabaseHandler;
import com.tenjava.entries.EvilCodes.t2.handlers.FilesHandler;
import com.tenjava.entries.EvilCodes.t2.handlers.MessagesHandler;
import com.tenjava.entries.EvilCodes.t2.utils.ChestInventory;
import com.tenjava.entries.EvilCodes.t2.utils.Enchantments;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PlayerListener implements Listener {

    //Players with a energylevel lower than 0.1 will be listed here - So they do less damage (1/3)
    public static final ArrayList<Player> minusStr2 = new ArrayList<Player>();
    //Players with a energylevel lower than 0.3 will be listed here - So they do less damage (1/2)
    public static final ArrayList<Player> minusStr1 = new ArrayList<Player>();

    //Listed chestinventories with chest that have been opened but are not completely empty
    public static final ArrayList<ChestInventory> chestinventories = new ArrayList<ChestInventory>();

    //Items you can find in the chests
    private static final Material[] findableItems = { Material.WOOD_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLD_SWORD, //SWORDS
            Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,  //LEATHER
            Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS, //IRON
            Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS, //CHAIN
            Material.GOLD_HELMET, Material.GOLD_CHESTPLATE, Material.GOLD_LEGGINGS, Material.GOLD_BOOTS, //GOLD
            Material.APPLE, Material.PUMPKIN_PIE, Material.PUMPKIN, Material.MELON, Material.MELON_BLOCK, Material.BREAD, Material.MUSHROOM_SOUP, //FOOD
            Material.RED_MUSHROOM, Material.BROWN_MUSHROOM, Material.POTATO, Material.BAKED_POTATO, Material.COOKED_BEEF, Material.COOKED_CHICKEN,  //FOOD
            Material.COOKED_FISH, Material.COOKIE, Material.CAKE, Material.CARROT_ITEM, Material.GOLDEN_APPLE, //FOOD
            Material.WOOD_AXE, Material.STONE_AXE, Material.IRON_AXE, //AXES
            Material.FISHING_ROD, Material.BOW, Material.ARROW }; //MISC

    //Spawn / respawn location from the config file
    public static Location getSpawnLocation() {
        return new Location(Bukkit.getWorld(FilesHandler.getConfig().getString("spawn.world")), FilesHandler.getConfig().getInt("spawn.x"), FilesHandler.getConfig().getInt("spawn.y"), FilesHandler.getConfig().getInt("spawn.z"));
    }

    /*
        EVENTS
     */

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent e) {
        final Player player = e.getPlayer();
        if (!DatabaseHandler.userExists(player)) {
            DatabaseHandler.insert(player);
        }
        player.setExp(0.99f);
        player.setGameMode(GameMode.SURVIVAL);
        DatabaseHandler.getConnection().execute("UPDATE `" + DatabaseHandler.getPrefix() + "player` SET `lastlogin` = CURRENT_TIMESTAMP WHERE `uuid` = '" + e.getPlayer().getUniqueId().toString() + "';");
        player.teleport(getSpawnLocation());
        //Add starter items (from config file)
        for (final int item : FilesHandler.getConfig().getIntegerList("startitems")) {
            player.getInventory().addItem(new ItemStack(item, 1));
        }
        player.sendMessage(MessagesHandler.convert("welcome"));
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
        //Deny a levelup
        e.setAmount(0);
    }

    @EventHandler
    public void onPlayerDeath(final PlayerDeathEvent e) {
        final Player player = e.getEntity();
        DatabaseHandler.increaseValue("deaths", player);
        player.sendMessage(MessagesHandler.convert("death"));
    }

    /**
     * Checks whether the opened chest (at loc x,y,z) is already opened (from chestinventories arraylist)
     * @param x Coordinate
     * @param y Coordinate
     * @param z Coordinate
     * @return true if it is listed in chestinventories arraylist
     */
    private static boolean containsChest(final int x, final int y, final int z) {
        boolean contains = false;
        for (final ChestInventory inv : chestinventories) {
            if (inv.getX() == x && inv.getY() == y && inv.getZ() == z) {
                contains = true;
                break;
            }
        }
        return contains;
    }

    /**
     * To get the inventory of a chest which has been already opened (check boolean above)
     * @param x Coordinate
     * @param y Coordinate
     * @param z Coordinate
     * @return the inventory of the chest
     */
    private static Inventory getInventory(final int x, final int y, final int z) {
        Inventory inventory = null;
        for (final ChestInventory inv : chestinventories) {
            if (inv.getX() == x && inv.getY() == y && inv.getZ() == z) {
                inventory = inv.getInventory();
                break;
            }
        }
        return inventory;
    }

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent e) {
        if (e.getClickedBlock() != null) {
            if (e.getClickedBlock().getType().equals(Material.CHEST)) {
                if (containsChest(e.getClickedBlock().getX(), e.getClickedBlock().getY(), e.getClickedBlock().getZ())) {
                    //Open a inventory of a chest which was already opened (see method above)
                    e.setCancelled(true);
                    e.getPlayer().openInventory(getInventory(e.getClickedBlock().getX(), e.getClickedBlock().getY(), e.getClickedBlock().getZ()));
                } else {
                    //Generate a new chestinventory
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
                    chestinventories.add(new ChestInventory(e.getPlayer(), e.getClickedBlock().getWorld().getName(), e.getClickedBlock().getX(), e.getClickedBlock().getY(), e.getClickedBlock().getZ(), inv));
                }
            } else if (e.getClickedBlock().getType().equals(Material.ENCHANTMENT_TABLE)) {
                //Direct enchant players item (in hand)
                e.setCancelled(true);
                if (e.getPlayer().getLevel() >= 30) {
                    Enchantments.enchant(e.getPlayer(), e.getPlayer().getItemInHand());
                    final int newmobkills = DatabaseHandler.getValue("mobkills", e.getPlayer()) - 30;
                    DatabaseHandler.getConnection().execute("UPDATE `" + DatabaseHandler.getPrefix() + "player` SET `mobkills` = '" + newmobkills + "' WHERE `uuid` = '" + e.getPlayer().getUniqueId().toString() + "';");
                    e.getPlayer().setLevel(e.getPlayer().getLevel() - 30);
                } else {
                    e.getPlayer().sendMessage("nolvl30");
                }
            }
        }
    }

    /**
     * Close listener so that the inventory of the chest will be saved in chestinventories arraylist
     */
    @EventHandler
    public void onPlayerCloseInventory(final InventoryCloseEvent e) {
        if (e.getInventory().getName().equalsIgnoreCase("rewardchest")) {
            for (final ChestInventory inv : chestinventories) {
                if (inv.getPlayer() == e.getPlayer()) {
                    final Inventory inventory = e.getInventory();
                    final Location location = inv.getLocation();
                    chestinventories.remove(inv);
                    chestinventories.add(new ChestInventory(null, location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), inventory));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerLevelUp(final PlayerLevelChangeEvent e) {
    //Deny levelup
        if (e.getOldLevel() < e.getNewLevel()) {
            e.getPlayer().setLevel(DatabaseHandler.getValue("mobkills", e.getPlayer()));
        }
    }

    @EventHandler
    public void onCreatureSpawn(final CreatureSpawnEvent e) {
        //Only let mobs spawn
        final Entity entity = e.getEntity();
        if (entity.getType() == EntityType.PIG || entity.getType() == EntityType.COW || entity.getType() == EntityType.CHICKEN || entity.getType() == EntityType.SHEEP) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onHungerLevelChange(final FoodLevelChangeEvent e) {
        //Player wont loose any hunger
        e.setCancelled(true);
        e.setFoodLevel(20);
    }

    @EventHandler
    public void onEntityDamageByEnity(final EntityDamageByEntityEvent e) {
        if (e.getEntity().getType() == EntityType.PLAYER) {
            //Check for pvp
            if (e.getDamager().getType() == EntityType.PLAYER) {
                if (!FilesHandler.getConfig().getBoolean("pvp")) {
                    final Player damager = (Player) e.getDamager();
                    damager.sendMessage(MessagesHandler.convert("nopvp"));
                    e.setCancelled(true);
                }
            }
        } else {
            //Dont do so much dmg when player has low energy
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
        //Same as on join
        e.setRespawnLocation(getSpawnLocation());
        e.getPlayer().setExp(0.9f);
        Bukkit.getScheduler().scheduleSyncDelayedTask(TenJava.instance, new Runnable() {
            @Override
            public void run() {
                e.getPlayer().setExp(0.9f);
            }
        }, 60L);
        for (final int item : FilesHandler.getConfig().getIntegerList("startitems")) {
            e.getPlayer().getInventory().addItem(new ItemStack(item, 1));
        }
    }

    @EventHandler
    public void onEntityExplore(final EntityExplodeEvent e) {
        //Cancel creeper / tnt explosions
        e.blockList().clear();
    }

    /*
    Only allow building for OPs
     */

    @EventHandler
    public void onPlayerBreakBlock(final BlockBreakEvent e) {
        if (!e.getPlayer().isOp())
            e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerPlaceBlock(final BlockPlaceEvent e) {
        if (!e.getPlayer().isOp())
            e.setCancelled(true);
    }
}
