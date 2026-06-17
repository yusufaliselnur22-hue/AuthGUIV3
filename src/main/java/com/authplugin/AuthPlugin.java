package com.authplugin;

import com.authplugin.command.AuthCommand;
import com.authplugin.listener.AuthListener;
import com.authplugin.listener.SignInputListener;
import com.authplugin.manager.AuthManager;
import com.authplugin.manager.SessionManager;
import org.bukkit.plugin.java.JavaPlugin;

public class AuthPlugin extends JavaPlugin {

    private static AuthPlugin instance;

    private AuthManager authManager;
    private SessionManager sessionManager;
    private AuthListener authListener;
    private SignInputListener signInputListener;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.authManager = new AuthManager(this);
        this.sessionManager = new SessionManager(this);
        this.authListener = new AuthListener(this);
        this.signInputListener = new SignInputListener(this);

        getServer().getPluginManager().registerEvents(authListener, this);
        getServer().getPluginManager().registerEvents(signInputListener, this);

        AuthCommand authCommand = new AuthCommand(this);
        getCommand("changepassword").setExecutor(authCommand);
        getCommand("authreload").setExecutor(authCommand);
        getCommand("resetpassword").setExecutor(authCommand);

        getLogger().info("AuthPlugin v" + getDescription().getVersion() + " etkinleştirildi.");
        getLogger().info("Oturum süresi: " + getConfig().getInt("session-duration-hours", 24) + " saat");
    }

    @Override
    public void onDisable() {
        if (sessionManager != null) {
            sessionManager.saveAll();
        }
        getLogger().info("AuthPlugin devre dışı bırakıldı.");
    }

    public static AuthPlugin getInstance() {
        return instance;
    }

    public AuthManager getAuthManager() {
        return authManager;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public AuthListener getAuthListener() {
        return authListener;
    }

    public SignInputListener getSignInputListener() {
        return signInputListener;
    }
}
