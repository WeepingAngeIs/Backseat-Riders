# Backseat Riders

Backseat Riders is a NeoForge mod for Minecraft 1.21.1, NeoForge 21.1.249, and Cobblemon 1.8.4.

## Implemented

- Owner-wide `/rideables trust <player|UUID>`, `/rideables revoke <player|UUID>`, and `/rideables list` commands. Trust data is keyed by owner and trusted-player UUID, and commands do not require mounting. Offline UUIDs can be revoked.
- Cobblemon's native rideability restrictions remain in place apart from the owner/passenger access gate. Trusted riders can board sent-out Pokemon when the owner is absent. When Cobblemon has no selected controller, an onboard trusted rider is selected and synchronized to clients.
- One fallback body-attached seat is added to a form only when Cobblemon's filtered native seat list has exactly one seat. Existing 2+ seat profiles, including six-seat profiles, are unchanged. Forms with no eligible native seats remain nonrideable.
- The fallback seat uses a bounded body-relative position after Cobblemon's side-specific delegate positions the passenger. The same common PokemonEntity hook runs on client and server when no model locator exists. It is not attached to an animated bone.
- Tamed horses accept the owner or a trusted party member as the second rider when the second player has an empty hand. The first passenger remains the controller.
- GitHub Actions compiles/tests the mod and separately launches a disposable dedicated server with pinned Cobblemon.

## Deferred or not verified

- This is a limited playable foundation, not the full requested editor. The visual seat editor, administrator seat-count settings, per-species/form position profiles, locator/bone attachment authoring, animated-bone seat motion, and generic cows/sheep/armadillos are deferred.
- Forms that are not natively rideable (zero eligible seats), passenger item/Poke Ball use, manual steering handoff, and complex battle/recall/portal/disconnect interactions are not added here.
- A dedicated-server startup smoke checks mod loading and common mixin application. It does not test two players riding in-game, client rendering, passenger interaction, or multiplayer dismount behavior. The fallback's visual alignment still needs in-game review on different Pokemon models and scales.
- Install the mod on both server and clients. No live Minecraft instance or world has been changed.

## Build

GitHub Actions uses Java 21 and produces `build/libs/backseat-riders-0.1.0.jar`. Install it alongside matching Cobblemon 1.8.4 on the server and all clients.

## Manual multiplayer checklist

On a disposable world/server, verify: two players share a horse; two players can ride a native one-seat Pokemon; native two-seat and six-seat profiles retain all seats; a trusted non-owner can board and drive when the owner is absent; an untrusted player is denied even when a passenger is already mounted; owner control remains preferred when present; revocation blocks future boarding; passengers can dismount safely. Also test scale changes, custom species, recall, battle, portals, disconnect, item use, Pokeballs, and native restrictions for pasture/platform/battle/beam states.
