package com.authplugin.manager;

import com.authplugin.AuthPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SessionManager {

    private final AuthPlugin plugin;
    private final File sessionsFile;

    private final Map<UUID, String> sessionIps = new HashMap<>();
    private final Map<UUID, Long> sessionTimestamps = new HashMap<>();

    public SessionManager(AuthPlugin plugin) {
        this.plugin = plugin;
        this.sessionsFile = new File(plugin.getDataFolder(), "sessions.yml");
        loadSessions();
    }

    public boolean isAuthenticated(UUID uuid, String currentIp) {
        if (!sessionIps.containsKey(uuid) || !sessionTimestamps.containsKey(uuid)) {
            return false;
        }
        String storedIp = sessionIps.get(uuid);
        long timestamp = sessionTimestamps.get(uuid);
        long sessionDurationMs = getSessionDurationMs();
        long now = System.currentTimeMillis();

        boolean ipMatches = storedIp.equals(currentIp);
        boolean notExpired = (now - timestamp) < sessionDurationMs;

        return ipMatches && notExpired;
    }

    public void authenticate(UUID uuid, String ip) {
        sessionIps.put(uuid, ip);
        sessionTimestamps.put(uuid, System.currentTimeMillis());
        saveSessions();
    }

    public void invalidate(UUID uuid) {
        sessionIps.remove(uuid);
        sessionTimestamps.remove(uuid);
        saveSessions();
    }

    public boolean hasSessionForDifferentIp(UUID uuid, String currentIp) {
        if (!sessionIps.containsKey(uuid)) return false;
        String storedIp = sessionIps.get(uuid);
        return !storedIp.equals(currentIp);
    }

    private long getSessionDurationMs() {
        int hours = plugin.getConfig().getInt("session-duration-hours", 24);
        return (long) hours * 60 * 60 * 1000;
    }

    public void loadSessions() {
        if (!sessionsFile.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(sessionsFile);
        sessionIps.clear();
        sessionTimestamps.clear();

        if (cfg.contains("sessions")) {
            for (String key : cfg.getConfigurationSection("sessions").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    String ip = cfg.getString("sessions." + key + ".ip", "");
                    long timestamp = cfg.getLong("sessions." + key + ".timestamp", 0L);
                    if (!ip.isEmpty() && timestamp > 0) {
                        sessionIps.put(uuid, ip);
                        sessionTimestamps.put(uuid, timestamp);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    public void saveSessions() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, String> entry : sessionIps.entrySet()) {
            UUID uuid = entry.getKey();
            String key = "sessions." + uuid.toString();
            cfg.set(key + ".ip", entry.getValue());
            cfg.set(key + ".timestamp", sessionTimestamps.getOrDefault(uuid, 0L));
        }
        try {
            cfg.save(sessionsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Oturumlar kaydedilemedi: " + e.getMessage());
        }
    }

    public void saveAll() {
        saveSessions();
    }
}
