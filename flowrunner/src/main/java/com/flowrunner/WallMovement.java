package com.flowrunner;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side wall slide + wall kick with buffering, coyote time,
 * a "perfect" timing window and a combo that rewards alternating walls.
 * Runs once per client tick (after the vanilla player tick).
 */
public final class WallMovement {
    private static final int NONE = 999;

    private static boolean prevJump;
    private static int jumpBuffer;
    private static int cooldown;
    private static int groundTicks;

    private static int ticksOnWall;      // consecutive ticks touching a wall (1 on first contact tick)
    private static int ticksSinceWall = NONE; // 0 while touching, counts up after leaving
    private static int contactLen = NONE;     // contact length of the most recent wall contact
    private static double wallNx, wallNz;     // normal of the most recent wall (points away from it)

    private static boolean hasLastKick;
    private static double kickNx, kickNz;
    private static int combo;

    private WallMovement() {}

    public static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        FlowConfig c = FlowConfig.get();
           if (p == null || mc.level == null || !c.enabled) {
            prevJump = false;
            resetWallState();
            return;
        }

        boolean jumpDown = mc.options.keyJump.isDown();
        boolean jumpPressed = jumpDown && !prevJump;
        prevJump = jumpDown;
        if (jumpPressed) jumpBuffer = c.jumpBufferTicks + 1;
        if (cooldown > 0) cooldown--;

        if (!eligible(p)) {
            resetWallState();
            endTick();
            return;
        }

        if (p.onGround()) {
            groundTicks++;
            resetWallState();
            if (groundTicks > c.comboResetGroundTicks) {
                combo = 0;
                hasLastKick = false;
            }
            endTick();
            return;
        }
        groundTicks = 0;

        // --- wall detection ---
        double[] n = findWall(p, c.wallReach);
        if (n != null) {
            ticksOnWall++;
            contactLen = ticksOnWall;
            ticksSinceWall = 0;
            wallNx = n[0];
            wallNz = n[1];
        } else {
            ticksOnWall = 0;
            if (ticksSinceWall < NONE) ticksSinceWall++;
        }

        // --- kick ---
        if (jumpBuffer > 0 && cooldown == 0 && ticksSinceWall <= c.coyoteTicks) {
            kick(p, c);
            endTick();
            return;
        }

        // --- slide ---
        if (c.slideEnabled && n != null && p.horizontalCollision) {
            Vec3 v = p.getDeltaMovement();
            if (v.y < -c.slideFallSpeed) {
                p.setDeltaMovement(v.x, -c.slideFallSpeed, v.z);
                p.resetFallDistance();
                if (c.effects && p.tickCount % 3 == 0) {
                    spawnWallParticles(p, n[0], n[1], 1, false);
                }
            }
        }
        endTick();
    }

    private static void endTick() {
        if (jumpBuffer > 0) jumpBuffer--;
    }

    private static void resetWallState() {
        ticksOnWall = 0;
        ticksSinceWall = NONE;
        contactLen = NONE;
    }

    private static boolean eligible(LocalPlayer p) {
        return !p.isSpectator()
                && !p.getAbilities().flying
                && !p.isFallFlying()
                && !p.isInWater()
                && !p.isInLava()
                && !p.onClimbable()
                && !p.isPassenger();
    }

    /** Returns the wall normal {x, z} (pointing away from the wall) or null if no wall is touching. */
    private static double[] findWall(LocalPlayer p, double reach) {
        Level level = p.level();
        AABB box = p.getBoundingBox();
        Vec3 v = p.getDeltaMovement();
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        double bestScore = -Double.MAX_VALUE;
        double[] best = null;
        for (int[] d : dirs) {
            AABB test = box.move(d[0] * reach, 0, d[1] * reach);
            if (!level.noCollision(p, test)) {
                double score = v.x * d[0] + v.z * d[1]; // prefer the wall we are moving into
                if (score > bestScore) {
                    bestScore = score;
                    best = new double[]{-d[0], -d[1]};
                }
            }
        }
        return best;
    }

    private static void kick(LocalPlayer p, FlowConfig c) {
        double nx = wallNx, nz = wallNz;
        Vec3 v = p.getDeltaMovement();

        boolean perfect = contactLen <= c.perfectWindowTicks;
        boolean sameWall = hasLastKick && (nx * kickNx + nz * kickNz) > 0.9;

        if (sameWall) {
            combo = 0;
        } else if (perfect) {
            combo = Math.min(combo + 1, c.maxCombo);
        }
        double chain = 1.0 + combo * c.comboSpeedBonus;
        if (sameWall) chain *= c.sameWallPenalty;

        // speed: keep incoming momentum, but never less than the base kick
        double hs = Math.hypot(v.x, v.z);
        double base = c.kickSpeed * (perfect ? c.perfectMultiplier : 1.0) * chain;
        double speed = Math.min(Math.max(base, hs * c.momentumCarry), c.maxSpeed);

        // direction: steer with look direction, but always leave the wall
        Vec3 look = p.getLookAngle();
        double lx = look.x, lz = look.z;
        double len = Math.hypot(lx, lz);
        if (len < 1.0e-4) {
            lx = nx;
            lz = nz;
        } else {
            lx /= len;
            lz /= len;
        }
        double into = lx * nx + lz * nz;
        if (into < 0) { // remove the part of the look vector that points into the wall
            lx -= into * nx;
            lz -= into * nz;
        }
        double dx = lx + nx * c.awayBias;
        double dz = lz + nz * c.awayBias;
        double dlen = Math.hypot(dx, dz);
        if (dlen < 1.0e-4) {
            dx = nx;
            dz = nz;
        } else {
            dx /= dlen;
            dz /= dlen;
        }

        double up = c.kickUp * (perfect ? c.perfectUpMultiplier : 1.0);
        p.setDeltaMovement(dx * speed, up, dz * speed);
        p.resetFallDistance();

        // feedback
        if (c.effects) {
            float pitch = (float) Math.min(2.0, 0.8 + combo * 0.1);
            SoundEvent sound = perfect ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.AMETHYST_BLOCK_HIT;
            p.level().playLocalSound(p.getX(), p.getY(), p.getZ(), sound, SoundSource.PLAYERS,
                    (float) c.volume, pitch, false);
            spawnWallParticles(p, nx, nz, perfect ? 10 : 5, perfect);
        }

        // bookkeeping
        hasLastKick = true;
        kickNx = nx;
        kickNz = nz;
        cooldown = c.cooldownTicks;
        jumpBuffer = 0;
        ticksSinceWall = NONE;
        ticksOnWall = 0;
        contactLen = NONE;
    }

    private static void spawnWallParticles(LocalPlayer p, double nx, double nz, int count, boolean perfect) {
        Level level = p.level();
        double x = p.getX() - nx * 0.35;
        double z = p.getZ() - nz * 0.35;
        for (int i = 0; i < count; i++) {
            double y = p.getY() + 0.2 + level.getRandom().nextDouble() * 1.4;
            double ox = (level.getRandom().nextDouble() - 0.5) * 0.2;
            double oz = (level.getRandom().nextDouble() - 0.5) * 0.2;
            level.addParticle(perfect ? ParticleTypes.CRIT : ParticleTypes.CLOUD,
                    x + ox, y, z + oz, nx * 0.05, 0.02, nz * 0.05);
        }
    }
}
