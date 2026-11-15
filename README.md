# Backseat Riders

Backseat Riders is a server-authoritative NeoForge mod for Minecraft 1.21.1. This starter implementation targets NeoForge 21.1.249 and Cobblemon 1.8.4.

## Implemented in this source branch

- Owner-only `/rideables trust <player>`, `/rideables revoke <player>`, and `/rideables list` commands while mounted on a Pok�mon the player owns.
- Permissions are stored by owner UUID and trusted player UUID in overworld saved data, so they apply to every sent-out Pok�mon owned by that trainer. Names are only used for command display.
- Cobblemon's native rideability checks remain in place except the explicit owner/passenger allowlist gate. Trusted UUIDs work across the owner's sent-out Pok�mon. A trusted onboard player's selected driver UUID is synchronized to clients when Cobblemon has no controller.
- Tamed vanilla horses can carry a second player, positioned behind the first. The horse's existing first passenger remains the controller.
- The GitHub Actions workflow builds the JAR on Java 21 and retains the build artifact.

## Not implemented or verified yet

- This initial slice does not add seats or change seat counts. It relies on the selected Pok�mon already having an available seat in Cobblemon's own riding settings. Species and form seat profiles, attachment locators, a visual editor, and animated-bone tuning are deferred.
- Additional generic vanilla mounts, including cows, sheep, and armadillos, are deferred.
- Item use and Pok� Ball compatibility, a manual steering handoff command, disconnect/recall/battle/portal race handling, and runtime multiplayer testing are deferred.
- This source has not been locally compiled: the development PC checkout has no Java/Gradle installation, and direct GitHub clone traffic is blocked. A successful GitHub Actions run is required before this branch can be treated as a distributable build. Compilation is not in-game testing.

## Build and install

The CI workflow runs `gradle --no-daemon clean build` with Java 21. The output is `build/libs/backseat-riders-0.1.0.jar`. Install the JAR on both server and clients alongside Cobblemon 1.8.4. The project does not modify worlds or install itself into any game instance.

## Multiplayer verification checklist

Before calling this playable, verify on a disposable server and world: two players share a tamed horse; owner boards a Pok�mon; trusted non-owner boards with a free native Pok�mon seat; untrusted player is denied even if another passenger is aboard; trusted rider drives when owner is absent and still drives on a full mount; owner remains Cobblemon's selected driver when present; revocation blocks future boarding; all riders can safely dismount. Also confirm Cobblemon still rejects out-of-range riders, platform-mounted or pastured Pok�mon, empty-seat profiles, owners in battle, below-minimum scale, beam mode 3, and behaviours that disallow riding. Then check disconnect, recall, battle, portals, resource reload, and custom species/scale. No such in-game verification has been performed for this source branch.

