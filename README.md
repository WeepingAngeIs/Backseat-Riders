# Backseat Riders

Backseat Riders targets Minecraft 1.21.1, NeoForge 21.1.249, and Cobblemon 1.8.4.

## Implemented

- Owner-wide UUID trust through `/rideables trust <player|UUID>`, `/rideables revoke <player|UUID>`, and `/rideables list`. Owners can manage grants without mounting a Pokemon; an online revoked rider is dismounted from that owner's Pokemon or horse.
- Explicit Pok�mon steering transfer through `/rideables handoff <player>` and `/rideables handoff reset`. The requester must be the owner or a currently authorized driver, and the target must be an authorized onboard rider. The server revalidates the requested driver on each control selection; revocation, dismount, or disabling trusted driving clears the handoff.
- Opt-in cow, sheep, and armadillo riding. Operators first assign ownership with `/rideables animal register <entity-selector> <player|UUID>`; ownership can be changed with `/rideables animal transfer <entity-selector> <player|UUID>` or removed with `/rideables animal unregister <entity-selector>`. Riding stays disabled by default until `/rideables settings friendly-animals true`. Only the assigned owner and that owner's trusted riders may mount; adult animals get two ground-only seats. Registration never assigns ownership to the first person who interacts.
- Friendly-animal boarding uses an empty main hand and does not run while sneaking. The mount handler runs at the final interaction priority and declines events already canceled by another mod. When Open Parties and Claims is installed, Backseat Riders calls its documented `IChunkProtectionAPI.onEntityInteraction` check before mounting; a protected interaction is denied, and an API failure fails closed. Other claim mods are only respected when they cancel the interaction event; this is not universal claim compatibility. The server stores entity UUID to owner UUID assignments in overworld SavedData; transfer/removal and disabling the feature dismount current riders. Trusted rider revocation also dismounts them.
- Cobblemon's native rideability checks remain in place. Trusted riders can board an owner's eligible sent-out Pokemon and may drive when configured. Server-side saved data stores trust and rideable settings.
- A default minimum of two seats for owned behavior-backed Pok�mon and for behaviorless Pok�mon when the ground fallback is enabled. Native seat profiles with more seats are preserved. Empty native seat profiles and behaviorless forms use stable generated seats.
- Behaviorless forms use a conservative owner/trusted ground controller by default, with no custom flight or swimming. Movement is limited to grounded horizontal input and runs through Cobblemon's existing travel/collision path. Server operators can opt out or adjust speed.
- Stable generated seat identities, server-synchronized seat counts and per-form generated-seat offsets. Fallback positions rotate with mount yaw and attach to the body. Global and per-form minimums can expand to 16 generated seats; reducing a configured minimum does not eject current occupants.
- Tamed horse second rider for the owner or trusted player, with empty hand. The first passenger remains the controller.
- Operator visual editor: `/rideables settings editor <species> <form>`. It provides a top-down live seat marker preview, seat minimum controls, seat selection, and bounded X/Y/Z offset adjustments. Save applies changes on the server; Cancel discards the local draft. Per-form counts and offsets persist in overworld saved data. The preview is a 2D body diagram, not the rendered species model.
- Operator commands: `/rideables settings show`, `/rideables settings minimum-seats <2..16>`, `/rideables settings trusted-board <true|false>`, `/rideables settings trusted-drive <true|false>`, `/rideables settings friendly-animals <true|false>`, `/rideables settings ground-fallback <true|false>`, `/rideables settings ground-speed <0.02..0.12>`, `/rideables settings seat-offset <species> <form> <seat-index> <x> <y> <z>`, and `/rideables settings clear-seat-offset <species> <form> <seat-index>`. Offsets are mount-local coordinates in blocks and apply to generated seats.

## Verification

- GitHub Actions builds on Java 21 and runs JUnit policy, geometry, and editor-input validation tests, plus server GameTest coverage for global/per-form settings persistence.
- A disposable NeoForge 21.1.249 server launches with Cobblemon 1.8.4 and checks mixin application.
- A disposable GameTest checks one-seat Charizard fallback, two-seat Dewgong and six-seat Camerupt preservation, two generated ground seats for behaviorless Bulbasaur, opt-out, configured seat expansion, stable seat identity, server passenger positioning, saved global/per-form settings and offsets, a trusted Pok�mon handoff and control loss after revocation, and registered cow ownership persistence, disabled-by-default denial, two-passenger boarding, trusted ground movement, and revocation dismount. Its passengers are test doubles rather than two human clients.
- A disposable Xvfb client startup check is included in the current workflow; it verifies client-side mixin loading, not riding visuals or real-player interaction.
- The mod has not been installed in the live CurseForge instance, and no worlds or servers have been modified.

## Deferred or not verified

Animated model-bone/locator binding and model-rendered 3D preview are not implemented; the editor previews generated offsets on a top-down diagram. Body fallback positions may need tuning for individual models. Behaviorless Pok�mon and registered friendly animals use configurable conservative ground movement; they do not gain custom flying or swimming controls. Open Parties and Claims uses its documented entity-protection API; other claim mods are only respected when they cancel the interaction event, so compatibility with other land-claim mods must be tested individually. Cobblemon's installed client jar includes a seat `handsBusy` flag consulted by `LocalPlayerMixin`; actual passenger inventory/Pok� Ball actions have not been gameplay-tested. Real multiplayer riding, battle/recall/portal/disconnect/resource-reload behavior, and scale/race cases are not verified.

## Build and install

GitHub Actions builds `build/libs/backseat-riders-0.1.0.jar`. Install it on the server and every client with matching Cobblemon 1.8.4. Compilation, server startup, and GameTests do not establish real-player multiplayer behavior.

