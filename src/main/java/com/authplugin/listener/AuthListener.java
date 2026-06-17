package com.authplugin.listener;

import com.authplugin.AuthPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AuthListener implements Listener {

    private final AuthPlugin plugin;
    private final Set<UUID> authenticatedPlayers = new HashSet<>();

    private static final Set<String> ALLOWED_COMMANDS = Set.of(
        "/authreload", "/resetpassword"
    );

    public AuthListener(AuthPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String playerIp = getPlayerIp(player);
        String prefix = colorize(plugin.getConfig().getString("messages.prefix", "&8[&6Auth&8] &r"));

        authenticatedPlayers.remove(uuid);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                boolean registered = plugin.getAuthManager().isRegistered(uuid);

                if (!registered) {
                    authenticatedPlayers.remove(uuid);
                    player.sendMessage(prefix + colorize(plugin.getConfig().getString(
                        "messages.register-welcome",
                        "&eHoş geldin! &6Kayıt olmak için &e&lkayıt tabelasına &6şifreni yaz.")));
                    plugin.getSignInputListener().openSignFor(player, SignInputListener.SignAction.REGISTER);
                    return;
                }

                boolean sessionValid = plugin.getSessionManager().isAuthenticated(uuid, playerIp);

                if (sessionValid) {
                    authenticatedPlayers.add(uuid);
                    player.sendMessage(prefix + colorize(plugin.getConfig().getString(
                        "messages.login-session",
                        "&aOturum geçerli, otomatik giriş yapıldı!")));
                    return;
                }

                boolean ipChanged = plugin.getSessionManager().hasSessionForDifferentIp(uuid, playerIp);
                String welcomeMsg = ipChanged
                    ? plugin.getConfig().getString("messages.ip-changed", "&cFarklı bir IP'den bağlandın. Lütfen giriş yap.")
                    : plugin.getConfig().getString("messages.login-welcome", "&eGeri döndün! &6Giriş için şifreni yaz.");

                player.sendMessage(prefix + colorize(welcomeMsg));
                plugin.getSignInputListener().openSignFor(player, SignInputListener.SignAction.LOGIN);
            }
        }.runTaskLater(plugin, 5L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        authenticatedPlayers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (isAuthenticated(player)) return;
        if (plugin.getSignInputListener().hasPendingAction(player.getUniqueId())) return;

        double fromX = event.getFrom().getX();
        double fromZ = event.getFrom().getZ();
        double toX = event.getTo().getX();
        double toZ = event.getTo().getZ();

        if (fromX != toX || fromZ != toZ) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    @SuppressWarnings("deprecation")
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        blockIfNotAuth(event.getPlayer(), event,
            plugin.getConfig().getString("messages.blocked-action", "&cGiriş yapman gerekiyor!"));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (isAuthenticated(player)) return;

        String cmd = event.getMessage().toLowerCase().split(" ")[0];
        if (ALLOWED_COMMANDS.contains(cmd)) return;

        event.setCancelled(true);
        player.sendMessage(colorize(plugin.getConfig().getString("messages.prefix", "&8[&6Auth&8] &r")
            + plugin.getConfig().getString("messages.blocked-action", "&cGiriş yapman gerekiyor!")));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        blockIfNotAuth(event.getPlayer(), event,
            plugin.getConfig().getString("messages.blocked-action", "&cGiriş yapman gerekiyor!"));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        blockIfNotAuth(event.getPlayer(), event,
            plugin.getConfig().getString("messages.blocked-action", "&cGiriş yapman gerekiyor!"));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        blockIfNotAuth(event.getPlayer(), event,
            plugin.getConfig().getString("messages.blocked-action", "&cGiriş yapman gerekiyor!"));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        blockIfNotAuth(event.getPlayer(), event,
            plugin.getConfig().getString("messages.blocked-action", "&cGiriş yapman gerekiyor!"));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemPickup(PlayerPickupItemEvent event) {
        blockIfNotAuth(event.getPlayer(), event,
            plugin.getConfig().getString("messages.blocked-action", "&cGiriş yapman gerekiyor!"));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        blockIfNotAuth(player, event,
            plugin.getConfig().getString("messages.blocked-action", "&cGiriş yapman gerekiyor!"));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        blockIfNotAuth(player, event,
            plugin.getConfig().getString("messages.blocked-action", "&cGiriş yapman gerekiyor!"));
    }

    private void blockIfNotAuth(Player player, Cancellable event, String message) {
        if (isAuthenticated(player)) return;
        event.setCancelled(true);
        player.sendMessage(colorize(plugin.getConfig().getString("messages.prefix", "&8[&6Auth&8] &r") + message));
    }

    public boolean isAuthenticated(Player player) {
        return authenticatedPlayers.contains(player.getUniqueId());
    }

    public void setAuthenticated(UUID uuid, boolean value) {
        if (value) {
            authenticatedPlayers.add(uuid);
        } else {
            authenticatedPlayers.remove(uuid);
        }
    }

    private String getPlayerIp(Player player) {
        if (player.getAddress() != null) {
            return player.getAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }

    private String colorize(String msg) {
        return msg.replace("&", "\u00a7");
    }
}
