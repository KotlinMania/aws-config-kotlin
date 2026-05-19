# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 0/62 (0.0%)
- **Function parity:** 0/666 matched — 0.0%
- **Class/type parity:** 0/156 matched — 0.0%
- **Combined symbol parity:** 0/822 matched — 0.0%
- **Average inline-code cosine:** 0.00 (function body across 0 matched files)
- **Average documentation cosine:** 0.00 (doc text across 0 matched files)
- **Cheat-zeroed Files:** 0
- **Critical Issues:** 0 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

1. **provider_config** (28 deps)
   - Path: `provider_config.rs`
   - Essential for 28 other files

2. **sso.credentials** (12 deps)
   - Path: `sso/credentials.rs`
   - Essential for 12 other files

3. **imds.region** (11 deps)
   - Path: `imds/region.rs`
   - Essential for 11 other files

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

## Next Commands

```bash
# Initialize task queue for systematic porting
cd tools/ast_distance
./ast_distance --init-tasks ../../tmp/aws-config/src rust ../../src/commonMain/kotlin/io/github/kotlinmania/awsconfig kotlin tasks.json ../../AGENTS.md

# Get next high-priority task
./ast_distance --assign tasks.json <agent-id>
```
## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Missing

| Source | Expected target | Deps | Source path | Expected path |
|--------|-----------------|------|-------------|---------------|
| `environment.mod` | `environment.Mod` | 0 | `environment/mod.rs` | `environment/Mod.kt` |
| `imds.mod` | `imds.Mod` | 0 | `imds/mod.rs` | `imds/Mod.kt` |
| `lib` | `Lib` | 0 | `lib.rs` | `Lib.kt` |
| `credentials.mod` | `meta.credentials.Mod` | 0 | `meta/credentials/mod.rs` | `meta/credentials/Mod.kt` |
| `meta.mod` | `meta.Mod` | 0 | `meta/mod.rs` | `meta/Mod.kt` |

