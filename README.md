# Backseat Riders

Backseat Riders is a server-authoritative NeoForge mod for Minecraft 1.21.1, targeting NeoForge 21.1.249 and Cobblemon 1.8.4.

## Implemented in this source branch

- Owner-wide `/rideables trust <player>`, `/rideables revoke <player>`, and `/rideables list` commands. Trust and revoke accept cached names or UUIDs, so players can manage their party without mounting a Pokemon and can revoke offline UUIDs.
- Permissions are stored by owner UUID and trusted player UUID in overworld saved data and apply across the owner's sent-out Pokemon.
- Cobblemon's native rideability checks remain in place except the explicit owner/passenger allowlist gate. Trusted UUIDs can board when the owner is absent. A trusted onboard player's selected driver UUID is synchronized to clients when Cobblemon has no controller.
- Tamed vanilla horses accept their owner or a trusted party member as a second rider with an empty hand. The extra rider is placed behind the first, and the first passenger remains the vanilla controller.
- GitHub Actions builds the JAR on Java 21. A separate workflow attempts a disposable NeoForge 21.1.249 dedicated-server launch with Cobblemon 1.8.4 to exercise mod loading and mixin application.

## Not implemented or verified yet

- This source does not add seats or change Pok�mon seat counts, so it does not meet the requested two-seat-per-Pok�mon default. It relies on native Cobblemon seat profiles having a free seat. Installed 1.8.4 species data uses named model locators (`seat_1`, `seat_2`, etc.) and has different existing counts (Charizard 1, Dewgong 2, Camerupt 6). A safe fallback requires a client-side body-position path for models without a locator plus a matching server passenger offset. That path has not yet been implemented or validated against the client runtime. Native seat counts are preserved as-is.
- Horse second-rider interaction, positioning, and dismounting need client/server multiplayer testing.
- Additional generic vanilla mounts, a visual seat editor, per-form profiles, animated-bone attachment tuning, item and Pok� Ball use, manual steering handoff, and disconnect/recall/battle/portal race handling are deferred.
- An earlier prototype commit passed CI compilation and unit tests. Recent changes are being rebuilt; the disposable server workflow has not yet completed successfully. Compilation is not in-game or multiplayer testing.

## Build and install

CI runs `gradle --no-daemon clean build` with Java 21 and uploads `build/libs/backseat-riders-0.1.0.jar`. Install the JAR on both server and clients alongside Cobblemon 1.8.4. This project has not been installed into the live Minecraft instance and has not changed any worlds.

## Multiplayer verification checklist

On a disposable server and world, verify: two players share a tamed horse; owner boards a Pok�mon; trusted non-owner boards with a free native Pok�mon seat; untrusted player is denied even if another passenger is aboard; trusted rider drives when owner is absent and on a full mount; owner remains Cobblemon's selected driver when present; revocation blocks future boarding; all riders can safely dismount. Also confirm Cobblemon still rejects out-of-range riders, platform-mounted or pastured Pok�mon, empty-seat profiles, owners in battle, below-minimum scale, beam mode 3, and behaviours that disallow riding. Then check disconnect, recall, battle, portals, resource reload, and custom species/scale. These interaction and multiplayer checks have not been performed.

