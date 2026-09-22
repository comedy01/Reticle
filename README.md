# Reticle

A fighter-jet style heads-up display for elytra flight. Speed, altitude, and vertical speed readouts, a heading tape, a flight-path marker that shows where you're actually moving (not just where you're looking), a "pull up" warning before you eat fall damage, and line-of-sight target boxes around nearby entities, color-coded hostile / passive / player.

Reticle is a small client-side mod for Minecraft on Fabric, compatible with Mod Menu.

Supported versions: 26.1 to 26.3.

## Features

- **Flight instruments** - horizontal speed, vertical speed, height above ground, and Y level.
- **Flight-path marker** - a separate marker from the crosshair showing your actual velocity direction, since diving or firework boosts often send you somewhere other than where you're looking.
- **Heading tape** - your compass heading across the top of the screen.
- **Pull-up warning** - flashes before your descent rate and altitude would put you into fall damage.
- **Target boxes** - boxes nearby entities in your direct line of sight (no wallhacks - anything blocked by terrain is skipped), color-coded by hostile / passive / player.
- **Recolorable** - the primary HUD color is fully adjustable; classic HUD green by default.
- **Client-side only** - there is nothing to install on a server. The HUD only appears while you're gliding.

## Install

**Fabric**

1. Install [Fabric Loader](https://fabricmc.net/use/) for your version of Minecraft.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) and the Fabric Reticle jar for your version in your `mods` folder.
3. Start the game.

Optional: add [Mod Menu](https://modrinth.com/mod/modmenu) to get a settings screen (*Mods > Reticle > Configure*).

## Status

Early development. Core instruments, the flight-path marker, and target boxes are implemented and unit-tested; in-game verification on a real client launch is still pending before a release build goes out.
