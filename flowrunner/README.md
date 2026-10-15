# Flow Runner (v0.1)

A client-side Fabric mod for Minecraft Java 26.3: wall slide, wall kick, momentum carry,
a perfect-timing window and an alternating-wall combo.

## Get the jar (no setup)
1. Make a free GitHub repo and upload everything in this folder (keep the `.github` folder).
2. Open the repo's **Actions** tab, click the latest "build" run, and download **flowrunner-jar** from Artifacts.
3. Unzip it and put `flowrunner-0.1.0.jar` in `.minecraft/mods`.

## You also need
- Minecraft Java 26.3 with Fabric Loader (0.19.5 or newer)
- Fabric API for 26.3
- Java 25 (the launcher bundles it)

## Controls
- Jump while touching a wall in the air = wall kick.
- Hold into a wall while falling = wall slide.
- Jump just as you touch the wall (or a couple ticks before) = perfect kick: more speed, higher pitch chime.
- Alternate between walls to build a combo. Spamming one wall loses speed.

## Tuning
Edit `.minecraft/config/flowrunner.properties` (created on first launch) and restart.

## Local build (optional)
Install JDK 25 and Gradle, then run `gradle build`. The jar appears in `build/libs/`.
