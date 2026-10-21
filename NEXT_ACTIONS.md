# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 6/63 (9.5%)
- **Function parity:** 26/673 matched (target 52) — 3.9%
- **Class/type parity:** 12/159 matched (target 43) — 7.5%
- **Combined symbol parity:** 38/832 matched (target 95) — 4.6%
- **Average inline-code cosine:** 0.11 (function body across 5 matched files)
- **Average documentation cosine:** 0.37 (doc text across 5 matched files)
- **Cheat-zeroed Files:** 1
- **Critical Issues:** 6 files with <0.60 function similarity
- **Needs Review:** 0 files with 0.60-0.84 function similarity
- **Excellent:** 0 files with >=0.85 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

1. **provider_config** (28 deps)
   - Path: `src/provider_config.rs`
   - Essential for 28 other files

2. **sso.credentials** (12 deps)
   - Path: `src/sso/credentials.rs`
   - Essential for 12 other files

3. **imds.region** (11 deps)
   - Path: `src/imds/region.rs`
   - Essential for 11 other files

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. profile

- **Target:** `profile.Profile`
- **Similarity:** 0.01
- **Dependents:** 2
- **Priority Score:** 2030609.9
- **Functions:** 1/4 matched (target 1)
- **Missing functions:** `new`, `taken_error`, `value_initialized_once`
- **Types:** 2/2 matched (target 7)
- **Missing types:** _none_
- **Tests:** 0/2 matched

### 2. default_provider.auth_scheme_preference

- **Target:** `authschemepreference.AuthSchemePreference`
- **Similarity:** 0.05
- **Dependents:** 1
- **Priority Score:** 1040709.6
- **Functions:** 2/6 matched (target 5)
- **Missing functions:** `auth_scheme_preference_provider`, `fmt`, `environment_priority`, `load_from_profile`
- **Types:** 1/1 matched (target 6)
- **Missing types:** _none_
- **Tests:** 1/3 matched

### 3. json_credentials

- **Target:** `awsconfig.JsonCredentials`
- **Similarity:** 0.15
- **Dependents:** 0
- **Priority Score:** 21808.5
- **Functions:** 13/15 matched (target 26)
- **Missing functions:** `from`, `fmt`
- **Types:** 3/3 matched (target 20)
- **Missing types:** _none_
- **Tests:** 9/9 matched

### 4. sensitive_command

- **Target:** `awsconfig.CommandWithSensitiveArgs`
- **Similarity:** 0.16
- **Dependents:** 0
- **Priority Score:** 20608.4
- **Functions:** 3/5 matched (target 4)
- **Missing functions:** `fmt`, `fmt`
- **Types:** 1/1 matched
- **Missing types:** _none_

### 5. retry

- **Target:** `retry.Retry`
- **Similarity:** 0.19
- **Dependents:** 0
- **Priority Score:** 20508.1
- **Functions:** 1/3 matched (target 7)
- **Missing functions:** `fmt`, `source`
- **Types:** 2/2 matched (target 5)
- **Missing types:** _none_

### 6. environment.mod

- **Target:** `environment.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 910.0
- **Functions:** 6/6 matched (target 9)
- **Missing functions:** _none_
- **Types:** 3/3 matched (target 4)
- **Missing types:** _none_

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Missing

| Source | Expected target | Deps | Source path | Expected path |
|--------|-----------------|------|-------------|---------------|
| `imds.mod` | `imds.Mod` | 0 | `src/imds/mod.rs` | `imds/Mod.kt` |
| `lib` | `Lib` | 0 | `src/lib.rs` | `Lib.kt` |
| `credentials.mod` | `meta.credentials.Mod` | 0 | `src/meta/credentials/mod.rs` | `meta/credentials/Mod.kt` |
| `meta.mod` | `meta.Mod` | 0 | `src/meta/mod.rs` | `meta/Mod.kt` |

