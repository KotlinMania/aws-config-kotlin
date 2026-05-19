// port-lint: source environment/mod.rs
package io.github.kotlinmania.awsconfig.environment

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Providers that load configuration from environment variables.
 */

/**
 * Load credentials from the environment.
 */
/*
 * Upstream re-export ledger:
 *
 * - `pub use credentials::EnvironmentVariableCredentialsProvider;`
 *   Defining file: environment/credentials.rs.
 *   Kotlin owner: EnvironmentVariableCredentialsProvider in the environment package.
 */

/**
 * Load regions from the environment.
 */
/*
 * Upstream re-export ledger:
 *
 * - `pub use region::EnvironmentVariableRegionProvider;`
 *   Defining file: environment/region.rs.
 *   Kotlin owner: EnvironmentVariableRegionProvider in the environment package.
 */

/*
 * Downstream caller migration ledger:
 *
 * - 2026-05-19: Scanned aws-config-dependent sibling ports. The only downstream Rust
 *   dependency found was codex-kotlin through tmp/codex/codex-rs/Cargo.toml and
 *   tmp/codex/codex-rs/aws-auth/Cargo.toml. No Kotlin callers import, wildcard-import,
 *   or fully qualify EnvironmentVariableCredentialsProvider or EnvironmentVariableRegionProvider
 *   through this environment package, so there were no downstream Kotlin files to rewrite.
 */
