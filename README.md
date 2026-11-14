# Backseat Riders

Backseat Riders is a server-authoritative NeoForge mod for Minecraft 1.21.1. This starter implementation targets NeoForge 21.1.249 and Cobblemon 1.8.4.

## Implemented in this source branch

- Owner-only `/rideables trust <player>`, `/rideables revoke <player>`, and `/rideables list` commands while mounted on a Pok�mon the player owns.
- Permissions are stored by Pok�mon entity UUID and player UUID in overworld saved data. Names are only used for command display.
- Trusted players pass Cobblemon's `PokemonEntity.isRideableFor` check without replacing its normal checks. A trusted onboard player can be selected to drive when Cobblemon has no controller.
- The GitHub Actions workflow builds the JAR on Java 21 and retains the build artifact.

## Not implemented or verified yet

- This initial slice does not add seats or change seat counts. It relies on the selected Pok�mon already having an available seat in Cobblemon's own riding settings. Species and form seat profiles, attachment locators, a visual editor, and animated-bone tuning are deferred.
- Generic vanilla mounts, including a two-player horse, are deferred.
- Item use and Pok� Ball compatibility, steering handoff controls, disconnect/recall/battle/portal race handling, and runtime multiplayer testing are deferred.
- This source has not been locally compiled: the development PC checkout has no Java/Gradle installation, and direct GitHub clone traffic is blocked. A successful GitHub Actions run is required before this branch can be treated as a distributable build. Compilation is not in-game testing.

## Build and install

The CI workflow runs `gradle --no-daemon clean build` with Java 21. The output is `build/libs/backseat-riders-0.1.0.jar`. Install the JAR on both server and clients alongside Cobblemon 1.8.4. The project does not modify worlds or install itself into any game instance.

## Multiplayer verification checklist

Before calling this playable, verify on a disposable server and world: owner boards; trusted non-owner boards with a free native Pok�mon seat; untrusted player is denied; trusted rider drives when owner is absent; owner remains Cobblemon's selected driver when present; revocation blocks future boarding; all riders can safely dismount; then check seat contention, disconnect, recall, battle, portals, resource reload, and custom species/scale. No such in-game verification has been performed for this source branch.

