# EFI TACZ Combat Roadmap

## Current Fire Rate Formula (What Is Used Today)

The current shot cadence in EFI follows TACZ runtime values, then converts to Minecraft ticks:

1. TACZ computes interval in milliseconds:

   intervalMs = 60000 / rpm

2. `rpm` is runtime-adjusted by TACZ using:
- current fire mode (`AUTO`, `SEMI`, `BURST`)
- attachment cache (`RpmModifier`)
- heat curve (`lerpRPM`) when heat data exists

3. EFI converts ms to ticks:

   shootIntervalTicks = max(1, floor(intervalMs / 50))

4. EFI applies cooldown directly after each successful shot:

   inactionTime = shootIntervalTicks

This means shot spacing is based on live TACZ gun data per shot attempt, not static config-only values.

## Why Some Weapons Can Still Feel Slower

Even with the fire-rate fixes, perceived cadence can still vary due to:
- server TPS/load (real-time spacing stretches when tick rate drops)
- burst-specific cadence path (`burst min interval` can differ from full-auto rpm path)
- behavior/state transitions (reload checks, target loss, stamina/combat gating)
- integer tick quantization (all cadence is snapped to whole ticks)

## Roadmap Items

### P1: Fire Cadence Verification Pass
- Add debug logging toggle for: fire mode, rpm, intervalMs, shootIntervalTicks, and applied inactionTime.
- Validate representative guns (SMG, AR, LMG, DMR, burst rifle) under stable 20 TPS.
- Compare expected theoretical RPM vs observed shots per 10 seconds.

### P2: Burst/Auto Consistency Audit
- Ensure burst interval path and auto interval path produce expected relative cadence per TACZ data.
- Add guardrails for edge values (very high RPM, very low RPM).

### P3: Aiming Accuracy Improvement (Non-Bug Enhancement)
- Replace computed geometric suppliers with live NPC look rotation suppliers (`getXRot` / `getYRot`).
- Treat this as an accuracy/feel enhancement, not a fire-rate bug fix.
- Re-test hit consistency on moving targets after change.

### P4: Optional Cadence Telemetry Command
- Add a lightweight debug command or config switch to print cadence metrics in dev/testing worlds.
- Keep disabled by default for production packs.

### P5: Generic Attribute Support in Advanced Mobpatch JSON
- Expand `attributes` parsing so datapacks can define additional vanilla and modded attributes beyond the current whitelist.
- Keep backward compatibility for existing keys (`weight`, `impact`, `armor_negation`, `max_stamina`, `stamina_regan_multiply`, `max_strikes`, `attack_damage`).
- Add validation/logging for unknown or invalid attribute ids to prevent silent misconfiguration.
- Apply parsed attributes safely only when the target entity actually has that attribute instance.

## Status

- Fire-rate bug fixes: completed.
- Aim rotation improvement: planned as enhancement.
- Generic advanced attribute parsing: planned.