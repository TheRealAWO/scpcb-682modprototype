# SCP-682 Player Proof of Concept

This branch is a **source modification of SCP: Containment Breach itself**, not a browser/WebGL recreation.

The original CB renderer, `.rmesh` environments, map generation, doors, collision, events, NPCs, particles, audio and UI remain the substrate. The new gameplay layer is isolated in `SCP682Player.bb` and is hooked into the existing per-frame gameplay/UI path through `Achievements.bb` to minimize changes to the base game while the mechanics are prototyped.

## Branch

`prototype/scp-682-player`

## Current prototype mechanics

- SCP-682 **Biomass** replaces ordinary human survivability.
- Ordinary CB injury/blood-loss state is converted into Biomass damage.
- Repeated damage builds **adaptation**, reducing subsequent damage.
- Biomass regenerates after a short damage delay.
- Lethal CB states are intercepted and converted into trauma/adaptation rather than a normal D-9341 game-over.
- Hitting zero Biomass causes a short **reconstitution** state instead of permanent death.
- **Rage** builds from combat/damage and can trigger an adaptive rage state.
- Human-specific survival systems are suppressed for the prototype: stamina exhaustion, infection, blood loss, sanity and ordinary blinking are not meaningful limitations for 682.
- Nearby CB doors can be forced open without keycards.
- Existing CB NPCs are used as combat targets; the prototype does not replace the facility or enemy roster with stand-ins.

## Controls

| Input | Action |
|---|---|
| LMB | Slash |
| RMB | Maul |
| R | Charge |
| Q | Body slam |
| F | Roar |
| C | Adaptive rage (when full) |
| E | Normal door interaction |
| WASD / mouse | Existing Containment Breach movement/look |

The normal CB HUD is supplemented by Biomass, Rage and Adaptation meters.

## Character-art status

The supplied feminized anthropomorphic SCP-682 design is the identity target for the finished first-person player body, but **this proof of concept intentionally does not fake that model with a cropped character-sheet PNG or primitive replacement mesh**.

A production-quality first-person body still requires a purpose-built, textured and rigged 3D asset with compatible first-person animations. Until such an asset exists, this branch focuses on integrating the correct gameplay into the real CB engine.

## Build status

This is an early Blitz3D source proof of concept. It is based on the fork's existing SCP:CB 1.3.11 source and therefore retains the original project's Blitz3D/FMOD build requirements.

The branch has been source-reviewed for integration points, but the current ChatGPT environment does not contain the original Blitz3D compiler/toolchain, so **a successful Windows executable build has not yet been claimed**. The next validation step is compiling the branch with the same Blitz3D setup used by the base repository and fixing any compiler/runtime regressions discovered there.

## Scope of the POC

The goal of this branch is to prove the central transformation:

> SCP: Containment Breach's actual facility and systems, but with the player role replaced by an extremely durable, regenerating, melee-only SCP-682.

It is not yet a finished total conversion. In particular, scripted events whose logic assumes D-9341 will need individual 682-specific outcomes, MTF tactics need a dedicated anti-682 state set, and a real 682 first-person character asset/animation set still needs to be integrated.

## Licensing

This fork inherits the source project's Creative Commons Attribution-ShareAlike 3.0 licensing requirements and existing contributor credits. Preserve the original attribution and license material when redistributing modified builds.
