package com.authplugin.listener;

import com.authplugin.AuthPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SignInputListener implements Listener {

    public enum SignAction {
        REGISTER, LOGIN, CHANGE_PASSWORD
    }

    private final AuthPlugin plugin;
    private final Map<UUID, SignAction> pendingActions = new HashMap<>();
    private final Map<UUID, Location> tempSignLocations = new HashMap<>();
    private final Map<UUID, BlockState> originalStates = new HashMap<>();

    public SignInputListener(AuthPlugin plugin) {
        this.plugin = plugin;
    }

    public void openSignFor(Player player, SignAction action) {
        if (pendingActions.containsKey(player.getUniqueId())) {
            restoreTempSign(player.getUniqueId());
        }

        Location signLoc = findSignLocation(player);
        if (signLoc == null) {
            plugin.getLogger().warning("İmzalanacak yer bulunamadı: " + player.getName());
            return;
        }

        Block block = signLoc.getBlock();
        originalStates.put(player.getUniqueId(), block.getState());
        tempSignLocations.put(player.getUniqueId(), signLoc);
        pendingActions.put(player.getUniqueId(), action);

        BlockFace playerFacing = getPlayerFacing(player);
        block.setType(Material.OAK_WALL_SIGN);

        org.bukkit.block.data.type.WallSign wallSignData =
            (org.bukkit.block.data.type.WallSign) block.getBlockData();
        wallSignData.setFacing(playerFacing);
        block.setBlockData(wallSignData);

        Sign sign = (Sign) block.getState();
        setSignText(sign, action);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    restoreTempSign(player.getUniqueId());
                    return;
                }
                Block freshBlock = signLoc.getBlock();
                if (freshBlock.getState() instanceof Sign freshSign) {
                    player.openSign(freshSign, Side.FRONT);
                }
            }
        }.runTaskLater(plugin, 2L);
    }

    private void setSignText(Sign sign, SignAction action) {
        boolean glow = plugin.getConfig().getBoolean("register-sign.glow", true);
        String colorStr = action == SignAction.REGISTER
            ? plugin.getConfig().getString("register-sign.color", "ORANGE")
            : plugin.getConfig().getString("login-sign.color", "ORANGE");

        TextColor textColor = resolveColor(colorStr);

        String cfgBase = (action == SignAction.REGISTER) ? "register-sign" : "login-sign";

        String l0 = plugin.getConfig().getString(cfgBase + ".line0", "");
        String l1 = plugin.getConfig().getString(cfgBase + ".line1", action == SignAction.REGISTER ? "--- KAYIT ---" : "--- GIRIS ---");
        String l2 = plugin.getConfig().getString(cfgBase + ".line2", "Sifreni yukari yaz");
        String l3 = plugin.getConfig().getString(cfgBase + ".line3", "Bitti'ye bas");

        if (action == SignAction.CHANGE_PASSWORD) {
            l1 = "SIFRE DEGISTIR";
            l2 = "Yeni sifreni";
            l3 = "yukari yaz";
        }

        org.bukkit.block.sign.SignSide side = sign.getSide(Side.FRONT);
        side.setGlowingText(glow);

        side.line(0, Component.text(l0).color(textColor).decoration(TextDecoration.ITALIC, false));
        side.line(1, Component.text(l1).color(textColor).decoration(TextDecoration.ITALIC, false).decorate(TextDecoration.BOLD));
        side.line(2, Component.text(l2).color(textColor).decoration(TextDecoration.ITALIC, false));
        side.line(3, Component.text(l3).color(textColor).decoration(TextDecoration.ITALIC, false));

        sign.update(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!pendingActions.containsKey(uuid)) return;

        event.setCancelled(true);

        String password = "";
        Component line0 = event.line(0);
        if (line0 != null) {
            password = PlainTextComponentSerializer.plainText().serialize(line0).trim();
        }

        SignAction action = pendingActions.remove(uuid);
        restoreTempSign(uuid);

        final String finalPassword = password;

        new BukkitRunnable() {
            @Override
            public void run() {
                processSignInput(player, action, finalPassword);
            }
        }.runTaskLater(plugin, 1L);
    }

    private void processSignInput(Player player, SignAction action, String password) {
        String prefix = colorize(plugin.getConfig().getString("messages.prefix", "&8[&6Auth&8] &r"));

        if (password.isEmpty()) {
            player.sendMessage(prefix + colorize(plugin.getConfig().getString("messages.register-fail-empty", "&cŞifre boş olamaz!")));
            new BukkitRunnable() {
                @Override
                public void run() {
                    openSignFor(player, action);
                }
            }.runTaskLater(plugin, 10L);
            return;
        }

        if (password.length() < 4) {
            player.sendMessage(prefix + colorize(plugin.getConfig().getString("messages.register-fail-short", "&cŞifre en az 4 karakter olmalı!")));
            new BukkitRunnable() {
                @Override
                public void run() {
                    openSignFor(player, action);
                }
            }.runTaskLater(plugin, 10L);
            return;
        }

        switch (action) {
            case REGISTER -> handleRegister(player, password, prefix);
            case LOGIN -> handleLogin(player, password, prefix);
            case CHANGE_PASSWORD -> handleChangePassword(player, password, prefix);
        }
    }

    private void handleRegister(Player player, String password, String prefix) {
        if (plugin.getAuthManager().isRegistered(player.getUniqueId())) {
            player.sendMessage(prefix + colorize(plugin.getConfig().getString("messages.register-already", "&cZaten kayıtlısın!")));
            return;
        }

        boolean success = plugin.getAuthManager().register(player.getUniqueId(), password);
        if (success) {
            String playerIp = getPlayerIp(player);
            plugin.getSessionManager().authenticate(player.getUniqueId(), playerIp);
            plugin.getAuthListener().setAuthenticated(player.getUniqueId(), true);
            player.sendMessage(prefix + colorize(plugin.getConfig().getString("messages.register-success", "&aKayıt başarılı! İyi oyunlar!")));
            plugin.getLogger().info(player.getName() + " kayıt oldu.");
        } else {
            player.sendMessage(prefix + "&cKayıt sırasında bir hata oluştu. Lütfen tekrar dene.");
        }
    }

    private void handleLogin(Player player, String password, String prefix) {
        if (!plugin.getAuthManager().isRegistered(player.getUniqueId())) {
            player.sendMessage(prefix + "&cÖnce kayıt olman gerekiyor!");
            new BukkitRunnable() {
                @Override
                public void run() {
                    openSignFor(player, SignAction.REGISTER);
                }
            }.runTaskLater(plugin, 10L);
            return;
        }

        boolean correct = plugin.getAuthManager().verifyPassword(player.getUniqueId(), password);
        if (correct) {
            String playerIp = getPlayerIp(player);
            plugin.getSessionManager().authenticate(player.getUniqueId(), playerIp);
            plugin.getAuthListener().setAuthenticated(player.getUniqueId(), true);
            player.sendMessage(prefix + colorize(plugin.getConfig().getString("messages.login-success", "&aGiriş başarılı! İyi oyunlar!")));
            plugin.getLogger().info(player.getName() + " giriş yaptı.");
        } else {
            player.sendMessage(prefix + colorize(plugin.getConfig().getString("messages.login-fail", "&cYanlış şifre! Tekrar dene.")));
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) openSignFor(player, SignAction.LOGIN);
                }
            }.runTaskLater(plugin, 20L);
        }
    }

    private void handleChangePassword(Player player, String newPassword, String prefix) {
        boolean success = plugin.getAuthManager().changePassword(player.getUniqueId(), newPassword);
        if (success) {
            player.sendMessage(prefix + colorize(plugin.getConfig().getString("messages.change-password-success", "&aŞifreni başarıyla değiştirdin!")));
        } else {
            player.sendMessage(prefix + "&cŞifre değiştirilemedi. Tekrar dene.");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        pendingActions.remove(uuid);
        restoreTempSign(uuid);
    }

    private void restoreTempSign(UUID uuid) {
        Location loc = tempSignLocations.remove(uuid);
        BlockState original = originalStates.remove(uuid);
        if (loc != null && original != null) {
            Block block = loc.getBlock();
            block.setType(original.getType());
            if (block.getType() == original.getType()) {
                original.update(true);
            }
        } else if (loc != null) {
            loc.getBlock().setType(Material.AIR);
        }
    }

    private Location findSignLocation(Player player) {
        Location eyeLoc = player.getEyeLocation();
        double yaw = Math.toRadians(eyeLoc.getYaw());
        double dx = -Math.sin(yaw);
        double dz = Math.cos(yaw);

        for (int dist = 1; dist <= 3; dist++) {
            Location candidate = player.getLocation().clone().add(dx * dist, 0.5, dz * dist);
            Block block = candidate.getBlock();
            if (block.getType() == Material.AIR || !block.getType().isSolid()) {
                candidate.setX(Math.floor(candidate.getX()) + 0.5);
                candidate.setY(Math.floor(candidate.getY()));
                candidate.setZ(Math.floor(candidate.getZ()) + 0.5);
                return candidate;
            }
        }

        Location fallback = player.getLocation().clone().add(0, 1, 0);
        fallback.setX(Math.floor(fallback.getX()) + 0.5);
        fallback.setY(Math.floor(fallback.getY()));
        fallback.setZ(Math.floor(fallback.getZ()) + 0.5);
        return fallback;
    }

    private BlockFace getPlayerFacing(Player player) {
        float yaw = player.getLocation().getYaw();
        yaw = ((yaw % 360) + 360) % 360;
        if (yaw >= 315 || yaw < 45) return BlockFace.NORTH;
        if (yaw < 135) return BlockFace.EAST;
        if (yaw < 225) return BlockFace.SOUTH;
        return BlockFace.WEST;
    }

    private String getPlayerIp(Player player) {
        if (player.getAddress() != null) {
            return player.getAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }

    private TextColor resolveColor(String colorName) {
        return switch (colorName.toUpperCase()) {
            case "ORANGE" -> TextColor.color(0xFF6A00);
            case "GOLD" -> NamedTextColor.GOLD;
            case "RED" -> NamedTextColor.RED;
            case "GREEN" -> NamedTextColor.GREEN;
            case "BLUE" -> NamedTextColor.BLUE;
            case "YELLOW" -> NamedTextColor.YELLOW;
            case "AQUA" -> NamedTextColor.AQUA;
            case "WHITE" -> NamedTextColor.WHITE;
            case "LIGHT_PURPLE" -> NamedTextColor.LIGHT_PURPLE;
            default -> TextColor.color(0xFF6A00);
        };
    }

    private String colorize(String msg) {
        return msg.replace("&", "\u00a7");
    }

    public boolean hasPendingAction(UUID uuid) {
        return pendingActions.containsKey(uuid);
    }
}
