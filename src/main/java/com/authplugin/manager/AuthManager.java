package com.authplugin.manager;

import com.authplugin.AuthPlugin;
import com.authplugin.util.PasswordUtil;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class AuthManager {

    private final AuthPlugin plugin;
    private final File dataFolder;

    public AuthManager(AuthPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "players");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public boolean isRegistered(UUID uuid) {
        File file = getPlayerFile(uuid);
        if (!file.exists()) return false;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        return cfg.contains("password") && cfg.contains("salt");
    }

    public boolean register(UUID uuid, String password) {
        if (password == null || password.trim().isEmpty()) return false;
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(password, salt);

        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("uuid", uuid.toString());
        cfg.set("password", hash);
        cfg.set("salt", salt);

        try {
            cfg.save(getPlayerFile(uuid));
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Oyuncu verisi kaydedilemedi: " + uuid);
            e.printStackTrace();
            return false;
        }
    }

    public boolean verifyPassword(UUID uuid, String password) {
        if (!isRegistered(uuid)) return false;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(getPlayerFile(uuid));
        String storedHash = cfg.getString("password", "");
        String salt = cfg.getString("salt", "");
        return PasswordUtil.verifyPassword(password, storedHash, salt);
    }

    public boolean changePassword(UUID uuid, String newPassword) {
        if (!isRegistered(uuid)) return false;
        if (newPassword == null || newPassword.trim().isEmpty()) return false;

        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(newPassword, salt);

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(getPlayerFile(uuid));
        cfg.set("password", hash);
        cfg.set("salt", salt);

        try {
            cfg.save(getPlayerFile(uuid));
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Şifre değiştirilemedi: " + uuid);
            e.printStackTrace();
            return false;
        }
    }

    public boolean deletePlayer(UUID uuid) {
        File file = getPlayerFile(uuid);
        return file.exists() && file.delete();
    }

    private File getPlayerFile(UUID uuid) {
        return new File(dataFolder, uuid.toString() + ".yml");
    }
}
