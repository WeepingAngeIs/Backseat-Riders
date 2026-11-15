# Backseat Riders

Backseat Riders targets Minecraft 1.21.1, NeoForge 21.1.249, and Cobblemon 1.8.4.

## Implemented

- Owner-wide UUID trust through `/rideables trust <player|UUID>`, `/rideables revoke <player|UUID>`, and `/rideables list`. Owners can manage grants without mounting a Pok�mon; offline UUIDs can be revoked. Revocation dismounts an online trusted rider from the owner's Pok�mon or horse.
- Cobblemon's native rideability checks remain in place. Trusted riders can board an owner's eligible sent-out Pok�mon and may drive when configured. The owner remains the preferred native controller. Server-side saved data stores trust and rideable settings.
- A default minimum of two seats for native rideable Pok�mon forms. Existing native seat profiles with more seats are retained. For forms with no native seat definitions, fallback seats are generated only when Cobblemon still provides a native riding behavior; forms without riding behavior remain nonrideable.
- Stable fallback seat identities and synced seat counts. The fallback position is body-relative on both client and server; configured minimums can expand to 16 seats. Seat reduction does not evict existing occupants.
- Tamed horse second rider for the owner or trusted player, with empty hand. The first passenger remains the controller.
- Operator commands: `/rideables settings show`, `/rideables settings minimum-seats <2..16>`, `/rideables settings trusted-board <true|false>`, and `/rideables settings trusted-drive <true|false>`. These settings persist in overworld saved data. Turning trusted boarding off dismounts non-owner passengers from owned Pok�mon and horses.

## Verification

- GitHub Actions compiles on Java 21 and runs JUnit policy/position tests.
- A disposable NeoForge 21.1.249 server launches with Cobblemon 1.8.4 and checks mixin application.
- A disposable GameTest checks one-seat Charizard fallback, two-seat Dewgong and six-seat Camerupt preservation, Bulbasaur's zero-seat behavior eligibility, configured seat expansion, stable seat identity, and server passenger positioning. This is server logic verification, not player multiplayer or client visual testing.
- The mod has not been installed in the live CurseForge instance, and no worlds or servers have been modified.

## Deferred or not verified

The visual `/rideables settings` editor, species/form-specific seat profiles and saved positions, animated model-bone/locator binding, and added ride behaviors for Pok�mon that do not already have native Cobblemon riding behavior are not implemented. The fallback is a body attachment and may need per-model tuning. Generic cows, sheep, armadillos, item/Pok� Ball use by passengers, manual steering handoff, and real multiplayer horse/creature interaction are not verified. Battle, recall, portal, disconnect, resource reload, and scale/race interactions still need disposable in-game testing.

## Build and install

GitHub Actions builds `build/libs/backseat-riders-0.1.0.jar`. Install it on the server and every client with matching Cobblemon 1.8.4. Compilation and GameTest results do not establish real-player in-game behavior.

