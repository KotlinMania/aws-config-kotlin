# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 7/62 (11.3%)
- **Function parity:** 18/667 matched (target 38) — 2.7%
- **Class/type parity:** 9/159 matched (target 31) — 5.7%
- **Combined symbol parity:** 27/826 matched (target 69) — 3.3%
- **Average inline-code cosine:** 0.28 (function body across 5 matched files)
- **Average documentation cosine:** 0.38 (doc text across 5 matched files)
- **Cheat-zeroed Files:** 2
- **Critical Issues:** 7 files with <0.60 function similarity

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

### 1. profile

- **Target:** `profile.Profile`
- **Similarity:** 0.14
- **Dependents:** 2
- **Priority Score:** 2030608.6
- **Functions:** 1/4 matched (target 1)
- **Missing functions:** `new`, `taken_error`, `value_initialized_once`
- **Types:** 2/2 matched (target 7)
- **Missing types:** _none_
- **Tests:** 0/2 matched

### 2. default_provider.auth_scheme_preference

- **Target:** `authschemepreference.AuthSchemePreference`
- **Similarity:** 0.17
- **Dependents:** 1
- **Priority Score:** 1040708.3
- **Functions:** 2/6 matched (target 5)
- **Missing functions:** `auth_scheme_preference_provider`, `fmt`, `environment_priority`, `load_from_profile`
- **Types:** 1/1 matched (target 6)
- **Missing types:** _none_
- **Tests:** 1/3 matched

### 3. environment.mod

- **Target:** `environment.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 70710.0
- **Functions:** 0/4 matched (target 0)
- **Missing functions:** `fmt`, `parse_bool`, `parse_uint`, `parse_url`
- **Types:** 0/3 matched (target 0)
- **Missing types:** `InvalidBooleanValue`, `InvalidUintValue`, `InvalidUrlValue`

### 4. json_credentials

- **Target:** `awsconfig.JsonCredentials`
- **Similarity:** 0.42
- **Dependents:** 0
- **Priority Score:** 21605.8
- **Functions:** 11/13 matched (target 16)
- **Missing functions:** `from`, `fmt`
- **Types:** 3/3 matched (target 9)
- **Missing types:** _none_
- **Tests:** 9/9 matched

### 5. retry

- **Target:** `retry.Retry`
- **Similarity:** 0.29
- **Dependents:** 0
- **Priority Score:** 20507.1
- **Functions:** 1/3 matched
- **Missing functions:** `fmt`, `source`
- **Types:** 2/2 matched (target 5)
- **Missing types:** _none_

### 6. sensitive_command

- **Target:** `awsconfig.SensitiveCommand`
- **Similarity:** 0.37
- **Dependents:** 0
- **Priority Score:** 10506.3
- **Functions:** 3/4 matched
- **Missing functions:** `fmt`
- **Types:** 1/1 matched
- **Missing types:** _none_

### 7. imds.mod

- **Target:** `environment.Parsing [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched (target 9)
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 3)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only by basename: `environment/mod.rs` vs expected `imds/mod.rs`
- **Proposed provenance header:** `// port-lint: source imds/mod.rs` (current: `// port-lint: source environment/mod.rs`)
- **Lint issues:** 1

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
| `lib` | `Lib` | 0 | `lib.rs` | `Lib.kt` |
| `credentials.mod` | `meta.credentials.Mod` | 0 | `meta/credentials/mod.rs` | `meta/credentials/Mod.kt` |
| `meta.mod` | `meta.Mod` | 0 | `meta/mod.rs` | `meta/Mod.kt` |

