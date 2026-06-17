package com.authplugin.command;

import com.authplugin.AuthPlugin;
import com.authplugin.listener.SignInputListener;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class AuthCommand implements CommandExecutor {

    private final AuthPlugin plugin;

    public AuthCommand(AuthPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        String prefix = colorize(plugin.getConfig().getString("messages.prefix", "&8[&6Auth&8] &r"));

        switch (command.getName().toLowerCase()) {
            case "changepassword" -> {
                return handleChangePassword(sender, args, prefix);
            }
            case "authreload" -> {
                return handleReload(sender, prefix);
            }
            case "resetpassword" -> {
                return handleResetPassword(sender, args, prefix);
            }
        }
        return false;
    }

    private boolean handleChangePassword(CommandSender sender, String[] args, String prefix) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix + "&cBu komut sadece oyuncular tarafından kullanılabilir.");
            return true;
        }

        if (!plugin.getAuthListener().isAuthenticated(player)) {
            player.sendMessage(prefix + colorize(plugin.getConfig().getString(
                "messages.change-password-not-auth",
                "&cÖnce giriş yapman gerekiyor!")));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(prefix + colorize(plugin.getConfig().getString(
                "messages.change-password-usage",
                "&cKullanım: /changepassword <yeni_sifre>")));
            return true;
        }

        String newPassword = args[0];
        if (newPassword.length() < 4) {
            player.sendMessage(prefix + colorize(plugin.getConfig().getString(
                "messages.change-password-fail-short",
                "&cYeni şifre en az 4 karakter olmalı!")));
            return true;
        }

        boolean success = plugin.getAuthManager().changePassword(player.getUniqueId(), newPassword);
        if (success) {
            player.sendMessage(prefix + colorize(plugin.getConfig().getString(
                "messages.change-password-success",
                "&aŞifreni başarıyla değiştirdin!")));
        } else {
            player.sendMessage(prefix + "&cŞifre değiştirilemedi. Tekrar dene.");
        }
        return true;
    }

    private boolean handleReload(CommandSender sender, String prefix) {
        if (!sender.hasPermission("authplugin.admin")) {
            sender.sendMessage(prefix + colorize(plugin.getConfig().getString(
                "messages.no-permission", "&cBu komutu kullanma izniniz yok!")));
            return true;
        }
        plugin.reloadConfig();
        plugin.getSessionManager().loadSessions();
        sender.sendMessage(prefix + colorize(plugin.getConfig().getString(
            "messages.reload-success", "&aAuthPlugin config yenilendi!")));
        return true;
    }

    private boolean handleResetPassword(CommandSender sender, String[] args, String prefix) {
        if (!sender.hasPermission("authplugin.admin")) {
            sender.sendMessage(prefix + colorize(plugin.getConfig().getString(
                "messages.no-permission", "&cBu komutu kullanma izniniz yok!")));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(prefix + colorize(plugin.getConfig().getString(
                "messages.reset-usage", "&cKullanım: /resetpassword <oyuncu>")));
            return true;
        }

        String targetName = args[0];
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(prefix + colorize(plugin.getConfig().getString(
                "messages.reset-not-found", "&c%player% adlı oyuncu bulunamadı!")
                .replace("%player%", targetName)));
            return true;
        }

        UUID uuid = target.getUniqueId();
        plugin.getAuthManager().deletePlayer(uuid);
        plugin.getSessionManager().invalidate(uuid);

        if (target.isOnline() && target.getPlayer() != null) {
            plugin.getAuthListener().setAuthenticated(uuid, false);
            Player onlineTarget = target.getPlayer();
            onlineTarget.sendMessage(prefix + "&eŞifren sıfırlandı. Lütfen yeniden kayıt ol.");
            plugin.getSignInputListener().openSignFor(onlineTarget, SignInputListener.SignAction.REGISTER);
        }

        sender.sendMessage(prefix + colorize(plugin.getConfig().getString(
            "messages.reset-success", "&a%player% adlı oyuncunun şifresi sıfırlandı.")
            .replace("%player%", targetName)));
        return true;
    }

    private String colorize(String msg) {
        return msg.replace("&", "\u00a7");
    }
}
