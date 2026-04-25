# Changelog

All notable changes to EFI-Unofficial are documented in this file.

## 2026-04-24

### Added
- Datapack `nbt_tag` provider matching is now evaluated on server tick for living mobs, enabling runtime patch transitions without requiring a relog.
- Per-entity-type provider state tracking was introduced with explicit current/previous provider maps so provider routing can flip deterministically across repeated tag add/remove cycles.
- Runtime capability patch replacement path now includes robust dispatcher acquisition with guarded fallback logging for visibility when capability access fails.

### Changed
- Dynamic patch routing flow was finalized to follow datapack matcher state (`nbt_tag`) each check window:
  - matcher active -> advanced provider path
  - matcher inactive -> previous/original provider restore path
- Provider storage strategy now preserves baseline provider state per entity type and maintains reversible transitions during repeated apply/restore operations.
- Temporary diagnostics used during swap debugging were removed from normal success paths to reduce log noise in production runs.
- Release metadata was normalized for stable distribution naming:
  - Gradle version set to `20.14.16`
  - resulting artifact name standardized to `EFI-Unofficial-20.14.16.jar`
  - mod metadata author/description updated for current project ownership and scope.

### Fixed
- Resolved relog-only behavior where advanced patches previously applied only after world re-entry.
- Fixed provider restore regression where baseline provider could be overwritten during repeated toggle cycles.
- Fixed repeated swap instability where apply/restore transitions could stop working after one forward/backward cycle.
- Fixed duplicate SynchedEntityData define failures during patch re-application by guarding duplicate define attempts in `AdvancedCustomHumanoidMobPatch.onConstructed`.
- Fixed non-living/non-mob swap-path contamination by enforcing mob-only guards on join/tick swap checks.
- Fixed runtime jar identity drift by aligning build version and output jar naming to release target.

### Notes
- Existing third-party datapack/resource issues logged by other mods are unaffected by this release and were not modified in this change set.

## 2026-04-23

### Fixed
- Fire-rate rounding bug in TACZ interval conversion was corrected to avoid artificial slowdown at fractional tick intervals.
- Post-shot cooldown override behavior was corrected so EFI no longer forces a slower cadence than TACZ for fast RPM weapons.
- NPC TACZ firing no longer relies only on one-shot behavior re-entry cadence.
- Sustained firing now continues while combat predicates remain valid, with automatic exit on key interrupt states.

### Changed
- TACZ sustained fire lifecycle was implemented in the mob patch as persistent state across ticks (request, active, keep-alive, clear).
- Behavior loop preservation now sends a sustain heartbeat in the combat behavior task so looping ranged series can maintain channelled fire.
- Reload, invalid target, and gear-swap transitions now explicitly clear sustained fire state to avoid stale hold state.
- Sustained-fire cleanup refactor removed duplicated post-shot state updates by consolidating shared shot aftermath logic into a single helper.
- Advanced mobs now use an innate baseline one-block step height policy so navigation, strafing, and animation-driven movement can clear single-block lips consistently.
- Prior branch-local step-assist toggles were removed because they did not reliably affect all movement paths.
- Removed redundant spawn-time `maxUpStep` assignment in favor of the existing per-tick step-height enforcement path.

### Added
- Project wiki documentation in docs/WIKI.md, including:
  - datapack schema coverage
  - behavior/predicate references
  - TACZ integration notes
  - ammo slot and reload flow details
  - known caveats and status notes
- Technical roadmap document in docs/roadmap.md for TACZ cadence and follow-up improvements.

## 2026-04-22

### Changed
- Initial 1.20.1 EFI-Unofficial integration and stabilization updates were applied across core compatibility, AI behavior routing, and datapack support surfaces.

### Notes
- Build validation command for this ForgeGradle project remains:
  - ./gradlew clean reobfJar
