# Backseat Riders

Backseat Riders targets Minecraft 1.21.1, NeoForge 21.1.249, and Cobblemon 1.8.4.

## Implemented

- Owner-wide UUID trust through `/rideables trust <player|UUID>`, `/rideables revoke <player|UUID>`, and `/rideables list`. Owners can manage grants without mounting a Pokemon; an online revoked rider is dismounted from that owner's Pokemon or horse.
- Cobblemon's native rideability checks remain in place. Trusted riders can board an owner's eligible sent-out Pokemon and may drive when configured. Server-side saved data stores trust and rideable settings.
- A default minimum of two seats for forms that already have native Cobblemon riding behavior. Native seat profiles with more seats are preserved. For behavior-backed forms with an empty native seat list, fallback seats may be generated. Forms without native riding behavior remain nonrideable.
- Stable generated seat identities, server-synchronized seat counts and per-form generated-seat offsets. Fallback positions rotate with mount yaw and attach to the body. Minimums can expand to 16 generated seats; reducing the configured minimum does not eject current occupants.
- Tamed horse second rider for the owner or trusted player, with empty hand. The first passenger remains the controller.
- Operator commands: `/rideables settings show`, `/rideables settings minimum-seats <2..16>`, `/rideables settings trusted-board <true|false>`, `/rideables settings trusted-drive <true|false>`, `/rideables settings seat-offset <species> <form> <seat-index> <x> <y> <z>`, and `/rideables settings clear-seat-offset <species> <form> <seat-index>`. Offsets are mount-local coordinates in blocks and apply to generated seats. Settings and offsets persist in overworld saved data.

## Verification

- GitHub Actions builds on Java 21 and runs JUnit policy, geometry, and settings persistence tests.
- A disposable NeoForge 21.1.249 server launches with Cobblemon 1.8.4 and checks mixin application.
- A disposable GameTest checks one-seat Charizard fallback, two-seat Dewgong and six-seat Camerupt preservation, Bulbasaur's lack of native riding behavior, configured seat expansion, stable seat identity, and server passenger positioning.
- A disposable Xvfb client startup check is included in the current workflow; it verifies client-side mixin loading, not riding visuals or real-player interaction.
- The mod has not been installed in the live CurseForge instance, and no worlds or servers have been modified.

## Deferred or not verified

The visual seat editor and animated model-bone/locator binding are not implemented. Body fallback positions may need tuning for individual models. Pokemon without a Cobblemon riding behavior, including the installed Bulbasaur species data, remain nonrideable: adding universal movement would require inventing a movement style and stats for species with no native ride profile, and could grant unsuitable flight, swimming, or land controls. Other friendly animals, passenger item/Poke Ball use, manual steering handoff, real multiplayer riding, battle/recall/portal/disconnect/resource-reload behavior, and scale/race cases are not verified.

## Build and install

GitHub Actions builds `build/libs/backseat-riders-0.1.0.jar`. Install it on the server and every client with matching Cobblemon 1.8.4. Compilation, server startup, and GameTests do not establish real-player multiplayer behavior.

