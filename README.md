# Backseat Riders

Backseat Riders is a server-authoritative NeoForge mod for Minecraft 1.21.1. This starter implementation targets NeoForge 21.1.249 and Cobblemon 1.8.4.

## Implemented in this source branch

- Owner-only `/rideables trust <player>`, `/rideables revoke <player>`, and `/rideables list` commands while mounted on a Pokemon the player owns.
- Permissions are stored by owner UUID and trusted player UUID in overworld saved data, so they apply to every sent-out Pokemon owned by that trainer. Names are only used for command display.
- Cobblemon's native rideability checks remain in place except the explicit owner/passenger allowlist gate. Trusted UUIDs work across the owner's sent-out Pokemon. A trusted onboard player's selected driver UUID is synchronized to clients when Cobblemon has no controller.
- Tamed vanilla horses can carry a second player, positioned behind the first. The horse's existing first passenger remains the controller.
- The GitHub Actions workflow builds the JAR on Java 21 and retains the build artifact. A separate workflow starts a disposable NeoForge 21.1.249 dedicated server with the exact Cobblemon 1.8.4 dependency to check mod loading and mixin application.

## Not implemented or verified yet

- This slice still does not add seats or change seat counts, so it does not meet the requested two-seat-per-Pokemon default. It relies on the selected Pokemon already having an available seat in Cobblemon's own riding settings. The installed 1.8.4 data confirms native profiles use named model locators (seat_1, seat_2, etc.) and current counts vary (for example Charizard 1, Dewgong 2, Camerupt 6). A safe generic fallback needs a client-side path when a model lacks a locator as well as a matching server passenger offset; that hook has not yet been verified against the runtime API. Existing native seat profiles remain untouched.
- Additional generic vanilla mounts, including cows, sheep, and armadillos, are deferred.
- Item use and Pokeball compatibility, a manual steering handoff command, disconnect/recall/battle/portal race handling, and multiplayer interaction testing are deferred.
- CI compilation/unit tests passed for an earlier prototype commit. The disposable startup workflow has been added but its result is pending. Neither compilation nor server startup is in-game or multiplayer testing.

## Build and install

The CI workflow runs `gradle --no-daemon clean build` with Java 21. The output is `build/libs/backseat-riders-0.1.0.jar`. Install the JAR on both server and clients alongside Cobblemon 1.8.4. The project does not modify worlds or install itself into any game instance.

## Multiplayer verification checklist

Before calling this playable, verify on a disposable server and world: two players share a tamed horse; owner boards a Pokemon; trusted non-owner boards with a free native Pokemon seat; untrusted player is denied even if another passenger is aboard; trusted rider drives when owner is absent and still drives on a full mount; owner remains Cobblemon's selected driver when present; revocation blocks future boarding; all riders can safely dismount. Also confirm Cobblemon still rejects out-of-range riders, platform-mounted or pastured Pokemon, empty-seat profiles, owners in battle, below-minimum scale, beam mode 3, and behaviours that disallow riding. Then check disconnect, recall, battle, portals, resource reload, and custom species/scale. No multiplayer verification has been performed for this source branch.

