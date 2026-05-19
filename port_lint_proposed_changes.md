# port-lint Proposed Changes

**Generated:** 2026-05-19
**Source:** tmp/aws-config/src
**Target:** src/commonMain/kotlin/io/github/kotlinmania/awsconfig

These are review proposals only. They are emitted when a Rust -> Kotlin pair matches only after fallback normalization, so the existing `port-lint` header is not an exact provenance match.

| Target file | Current header | Proposed header | Source path | Reason |
|-------------|----------------|-----------------|-------------|--------|
| `src/commonMain/kotlin/io/github/kotlinmania/awsconfig/environment/Parsing.kt` | `// port-lint: source environment/mod.rs` | `// port-lint: source imds/mod.rs` | `imds/mod.rs` | `port-lint provenance header matched only by basename: 'environment/mod.rs' vs expected 'imds/mod.rs'` |
