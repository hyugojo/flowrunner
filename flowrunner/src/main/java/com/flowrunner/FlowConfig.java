package com.flowrunner;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Tunable numbers. Edit config/flowrunner.properties and restart the game.
 * All speeds are blocks per tick (20 ticks = 1 second).
 */
public final class FlowConfig {
    private static FlowConfig instance = new FlowConfig();

    public boolean enabled = true;
    public boolean effects = true;
    public double volume = 0.8;

    // Forgiveness
    public int jumpBufferTicks = 3;     // jump pressed this many ticks before touching a wall still counts
    public int coyoteTicks = 4;         // can still kick this many ticks after leaving a wall
    public int cooldownTicks = 4;       // minimum ticks between kicks
    public double wallReach = 0.18;     // how far from the hitbox a wall is detected

    // Wall slide
    public boolean slideEnabled = true;
    public double slideFallSpeed = 0.12; // max downward speed while pressed against a wall

    // Kick
    public double kickSpeed = 0.45;          // base horizontal speed after a kick
    public double kickUp = 0.52;             // upward speed after a kick (vanilla jump is ~0.42)
    public double awayBias = 0.7;            // 0 = leave along your look direction, higher = more straight away from the wall
    public double momentumCarry = 1.0;       // 1.0 = keep all your incoming horizontal speed
    public double maxSpeed = 1.0;            // horizontal speed cap

    // Timing and chaining
    public int perfectWindowTicks = 2;       // jump within this many ticks of touching the wall = perfect kick
    public double perfectMultiplier = 1.3;   // speed multiplier on a perfect kick
    public double perfectUpMultiplier = 1.1;
    public int maxCombo = 8;
    public double comboSpeedBonus = 0.06;    // extra speed per combo level
    public double sameWallPenalty = 0.75;    // spamming the same wall loses speed
    public int comboResetGroundTicks = 10;   // ticks on the ground before the combo resets

    public static FlowConfig get() {
        return instance;
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("flowrunner.properties");
        FlowConfig c = new FlowConfig();
        if (Files.exists(path)) {
            Properties p = new Properties();
            try (Reader r = Files.newBufferedReader(path)) {
                p.load(r);
            } catch (IOException e) {
                System.err.println("[FlowRunner] Could not read config, using defaults: " + e);
            }
            c.enabled = b(p, "enabled", c.enabled);
            c.effects = b(p, "effects", c.effects);
            c.volume = d(p, "volume", c.volume);
            c.jumpBufferTicks = i(p, "jumpBufferTicks", c.jumpBufferTicks);
            c.coyoteTicks = i(p, "coyoteTicks", c.coyoteTicks);
            c.cooldownTicks = i(p, "cooldownTicks", c.cooldownTicks);
            c.wallReach = d(p, "wallReach", c.wallReach);
            c.slideEnabled = b(p, "slideEnabled", c.slideEnabled);
            c.slideFallSpeed = d(p, "slideFallSpeed", c.slideFallSpeed);
            c.kickSpeed = d(p, "kickSpeed", c.kickSpeed);
            c.kickUp = d(p, "kickUp", c.kickUp);
            c.awayBias = d(p, "awayBias", c.awayBias);
            c.momentumCarry = d(p, "momentumCarry", c.momentumCarry);
            c.maxSpeed = d(p, "maxSpeed", c.maxSpeed);
            c.perfectWindowTicks = i(p, "perfectWindowTicks", c.perfectWindowTicks);
            c.perfectMultiplier = d(p, "perfectMultiplier", c.perfectMultiplier);
            c.perfectUpMultiplier = d(p, "perfectUpMultiplier", c.perfectUpMultiplier);
            c.maxCombo = i(p, "maxCombo", c.maxCombo);
            c.comboSpeedBonus = d(p, "comboSpeedBonus", c.comboSpeedBonus);
            c.sameWallPenalty = d(p, "sameWallPenalty", c.sameWallPenalty);
            c.comboResetGroundTicks = i(p, "comboResetGroundTicks", c.comboResetGroundTicks);
        }
        instance = c;
        write(path, c);
    }

    private static void write(Path path, FlowConfig c) {
        StringBuilder s = new StringBuilder();
        s.append("# Flow Runner config. Restart the game after editing.\n");
        s.append("# Speeds are blocks per tick (20 ticks = 1 second).\n");
        line(s, "enabled", c.enabled);
        line(s, "effects", c.effects);
        line(s, "volume", c.volume);
        line(s, "jumpBufferTicks", c.jumpBufferTicks);
        line(s, "coyoteTicks", c.coyoteTicks);
        line(s, "cooldownTicks", c.cooldownTicks);
        line(s, "wallReach", c.wallReach);
        line(s, "slideEnabled", c.slideEnabled);
        line(s, "slideFallSpeed", c.slideFallSpeed);
        line(s, "kickSpeed", c.kickSpeed);
        line(s, "kickUp", c.kickUp);
        line(s, "awayBias", c.awayBias);
        line(s, "momentumCarry", c.momentumCarry);
        line(s, "maxSpeed", c.maxSpeed);
        line(s, "perfectWindowTicks", c.perfectWindowTicks);
        line(s, "perfectMultiplier", c.perfectMultiplier);
        line(s, "perfectUpMultiplier", c.perfectUpMultiplier);
        line(s, "maxCombo", c.maxCombo);
        line(s, "comboSpeedBonus", c.comboSpeedBonus);
        line(s, "sameWallPenalty", c.sameWallPenalty);
        line(s, "comboResetGroundTicks", c.comboResetGroundTicks);
        try {
            Files.writeString(path, s.toString());
        } catch (IOException e) {
            System.err.println("[FlowRunner] Could not write config: " + e);
        }
    }

    private static void line(StringBuilder s, String k, Object v) {
        s.append(k).append('=').append(v).append('\n');
    }

    private static boolean b(Properties p, String k, boolean def) {
        String v = p.getProperty(k);
        return v == null ? def : Boolean.parseBoolean(v.trim());
    }

    private static int i(Properties p, String k, int def) {
        try {
            String v = p.getProperty(k);
            return v == null ? def : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static double d(Properties p, String k, double def) {
        try {
            String v = p.getProperty(k);
            return v == null ? def : Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
