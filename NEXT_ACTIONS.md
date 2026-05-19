# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Current Progress:** 3.2% (2/62 files)
- **Matched Files:** 2
- **Average Similarity:** 0.03
- **Critical Issues:** 2 files with <0.60 similarity

## Priority 1: Fix Incomplete High-Dependency Files

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
