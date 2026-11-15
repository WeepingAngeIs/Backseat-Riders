# Backseat Riders

Backseat Riders is a NeoForge mod for Minecraft 1.21.1, NeoForge 21.1.249, and Cobblemon 1.8.4.

## Implemented

- Owner-wide \`/rideables trust <player|UUID>\`, \`/rideables revoke <player|UUID>\`, and \`/rideables list\` commands. Trust data is keyed by owner UUID and trusted-player UUID; commands do not require mounting, and offline UUIDs can be revoked.
- Cobblemon's native rideability restrictions remain in place apart from the owner/passenger access gate. Trusted riders can board sent-out Pokémon when the owner is absent. When Cobblemon has no selected controller, an onboard trusted rider is selected and synchronized to clients.
- One fallback body-attached seat is added only when a Pokémon form has exactly one eligible native seat. Existing profiles with two or more seats are unchanged. Forms with zero eligible seats remain nonrideable.
- The generated seat uses a bounded body-relative position on both client and server after Cobblemon's delegate positions the rider. It does not follow animated model bones.
- Tamed horses accept the owner or a trusted party member as a second rider when the second player has an empty hand. The first passenger remains the controller.

## Verification

- GitHub Actions builds and runs JUnit tests on Java 21.
- A disposable dedicated server launches with Cobblemon 1.8.4 and checks mod/mixin loading.
- A disposable NeoForge GameTest constructs a single-seat Charizard, two-seat Dewgong, and zero-seat Bulbasaur; checks effective seat counts and stable fallback-seat identity; attaches a test passenger, invokes server passenger positioning, and checks finite coordinates at the expected body-relative position. This test verifies server logic, not real-player behavior or client rendering.
- The mod has not been installed into the live Minecraft instance, and no worlds have been changed.

## Deferred or not verified

The visual seat editor, admin seat-count settings, per-species/form position profiles, locator/bone attachment authoring, animated-bone seat motion, and generic cows/sheep/armadillos are deferred. Passenger item/Poké Ball use, manual steering handoff, and complex battle/recall/portal/disconnect interactions are not added. Horse interactions/dismount and real multiplayer steering still need in-game verification. GameTest checks server positioning logic, not visual alignment in the client; the body fallback should be reviewed on different Pokémon models and scales.

## Build and install

GitHub Actions uses Java 21 and produces \`build/libs/backseat-riders-0.1.0.jar\`. Install it alongside matching Cobblemon 1.8.4 on both the server and every client.
