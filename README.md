# aws-config-kotlin in Kotlin

[![GitHub link](https://img.shields.io/badge/GitHub-KotlinMania%2Faws--config--kotlin-blue.svg)](https://github.com/KotlinMania/aws-config-kotlin)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kotlinmania/aws-config-kotlin)](https://central.sonatype.com/artifact/io.github.kotlinmania/aws-config-kotlin)
[![Build status](https://img.shields.io/github/actions/workflow/status/KotlinMania/aws-config-kotlin/ci.yml?branch=main)](https://github.com/KotlinMania/aws-config-kotlin/actions)

This is a Kotlin Multiplatform line-by-line transliteration port of [`smithy-lang/smithy-rs`](https://github.com/smithy-lang/smithy-rs).

**Original Project:** This port is based on [`smithy-lang/smithy-rs`](https://github.com/smithy-lang/smithy-rs). All design credit and project intent belong to the upstream authors; this repository is a faithful port to Kotlin Multiplatform with no behavioural changes intended.

### Porting status

This is an **in-progress port**. The goal is feature parity with the upstream Rust crate while providing a native Kotlin Multiplatform API. Every Kotlin file carries a `// port-lint: source <path>` header naming its upstream Rust counterpart so the AST-distance tool can track provenance.

---

## Upstream README — `smithy-lang/smithy-rs`

> The text below is reproduced and lightly edited from [`https://github.com/smithy-lang/smithy-rs`](https://github.com/smithy-lang/smithy-rs). It is the upstream project's own description and remains under the upstream authors' authorship; links have been rewritten to absolute upstream URLs so they continue to resolve from this repository.

## Smithy Rust [![CI on Branch `main`](https://github.com/smithy-lang/smithy-rs/actions/workflows/ci-main.yml/badge.svg)](https://github.com/smithy-lang/smithy-rs/actions/workflows/ci-main.yml)


Smithy code generators for Rust that generate clients, servers, and the entire AWS SDK.
The latest unreleased SDK build can be found in [aws-sdk-rust/next](https://github.com/awslabs/aws-sdk-rust/tree/next).

[Design documentation](https://smithy-lang.github.io/smithy-rs/design/)

**All internal and external interfaces are considered unstable and subject to change without notice.**

Setup
-----

1. `./gradlew` will setup gradle for you. JDK 17 is required.
2. Running tests requires a working Rust installation. See [Rust docs](https://www.rust-lang.org/learn/get-started) for
installation instructions on your platform.

Development
-----------

For development, pre-commit hooks make it easier to pass automated linting when opening a pull request. Setup:
```bash
brew install pre-commit # (or appropriate for your platform: https://pre-commit.com/)
pre-commit install
```

Project Layout
--------------

* `aws`: AWS specific codegen & Rust code (signing, endpoints, customizations, etc.)
  Common commands:
  * `./gradlew :aws:sdk:assemble`: Generate (but do not test / compile etc.) a fresh SDK into `sdk/build/aws-sdk`
  * `./gradlew :aws:sdk:sdkTest`: Generate & run all tests for a fresh SDK. (Note that these tests require Go to be
  installed for FIP support to compile properly)
  * `./gradlew :aws:sdk:{cargoCheck, cargoTest, cargoDocs, cargoClippy}`: Generate & run specified cargo command.
* `codegen-core`: Common code generation logic useful for clients and servers
* `codegen-client`: Smithy client code generation
* `codegen-client-test`: Smithy protocol test generation & integration tests for Smithy client whitelabel code
* [`design`](https://github.com/smithy-lang/smithy-rs/blob/HEAD/design): Design documentation. See the [design/README.md](https://github.com/smithy-lang/smithy-rs/blob/HEAD/design/README.md) for details about building / viewing.
* `codegen-server`: Smithy server code generation
* `codegen-server-test`: Smithy protocol test generation & integration tests for Smithy server whitelabel code
* `examples`: A collection of server implementation examples

Testing
-------

Running all of smithy-rs's tests can take a very long time, so it's better to know which parts
to test based on the changes being made, and allow continuous integration to find other issues
when posting a pull request.

In general, the components of smithy-rs affect each other in the following order (with earlier affecting later):

1. `rust-runtime`
2. `codegen` and `codegen-server`
3. `aws/rust-runtime`
4. `aws/codegen-aws-sdk`

Some components, such as `codegen-client-test` and `codegen-server-test`, are purely for testing other components.

### Testing `rust-runtime` and `aws/rust-runtime`

To test the `rust-runtime` crates:

```bash
# Run all Rust tests for `rust-runtime/` (from repo root):
cargo test --manifest-path=rust-runtime/Cargo.toml
# Run clippy for `rust-runtime/` (from repo root):
cargo clippy --manifest-path=rust-runtime/Cargo.toml

# Or
cd rust-runtime
cargo test
cargo clippy
```

To test the `aws/rust-runtime` crates:

```bash
# Run all Rust tests for `aws/rust-runtime/` (from repo root):
cargo test --manifest-path=aws/rust-runtime/Cargo.toml
# Run clippy for `aws/rust-runtime/` (from repo root):
cargo clippy --manifest-path=aws/rust-runtime/Cargo.toml

# Or
cd aws/rust-runtime
cargo test
cargo clippy
```

Some runtime crates have a `additional-ci` script that can also be run. These scripts often require
[`cargo-hack`](https://github.com/taiki-e/cargo-hack) and [`cargo-udeps`](https://github.com/est31/cargo-udeps)
to be installed.

### Testing Client/Server Codegen

To test the code generation, the following can be used:

```bash
# Run Kotlin codegen unit tests
./gradlew codegen-core:check
./gradlew codegen-client:check
./gradlew codegen-server:check
# Run client codegen tests
./gradlew codegen-client-test:check
# Run server codegen tests
./gradlew codegen-server-test:check
```

Several Kotlin unit tests generate Rust projects and compile them. When these fail, they typically
output links to the location of the generated code so that it can be inspected.

To look at generated code when the codegen tests fail, check these paths depending on the test suite that's failing:
- For codegen-client-test: `codegen-client-test/build/smithyprojections/codegen-client-test`
- For codegen-server-test: `codegen-server-test/build/smithyprojections/codegen-server-test`

### Testing SDK Codegen

See the readme in `aws/sdk/` for more information about these targets as they can be configured
to generate more or less AWS service clients.

```bash
# Run Kotlin codegen unit tests
./gradlew aws:codegen-aws-sdk:check
# Generate an SDK, but do not attempt to compile / run tests. Useful for inspecting generated code
./gradlew :aws:sdk:assemble
# Run all the tests
./gradlew :aws:sdk:sdkTest
# Validate that the generated code compiles
./gradlew :aws:sdk:cargoCheck
# Validate that the generated code passes Clippy
./gradlew :aws:sdk:cargoClippy
# Validate the generated docs
./gradlew :aws:sdk:cargoDoc
```

The generated SDK will be placed in `aws/sdk/build/aws-sdk`.

### MSRV Policy
The MSRV (**M**inimum **S**upported **R**ust **V**ersion) for the crates in this project is `stable-2`, i.e. the current `stable` Rust version and the prior two versions. Older versions may work. In rare circumstances our MSRV may exceed `stable-2` for security purposes (ex: to update a dependency for a security fix that requires an MSRV bump).

---

## About this Kotlin port

### Installation

```kotlin
dependencies {
    implementation("io.github.kotlinmania:aws-config-kotlin:0.1.0")
}
```

### Building

```bash
./gradlew build
./gradlew test
```

### Targets

- macOS arm64
- Linux x64
- Windows mingw-x64
- iOS arm64 / simulator-arm64 (Swift export + XCFramework)
- JS (browser + Node.js)
- Wasm-JS (browser + Node.js)
- Android (API 24+)

### Porting guidelines

See [AGENTS.md](AGENTS.md) and [CLAUDE.md](CLAUDE.md) for translator discipline, port-lint header convention, and Rust → Kotlin idiom mapping.

### License

This Kotlin port is distributed under the same Apache-2.0 license as the upstream [`smithy-lang/smithy-rs`](https://github.com/smithy-lang/smithy-rs). See [LICENSE](LICENSE) (and any sibling `LICENSE-*` / `NOTICE` files mirrored from upstream) for the full text.

Original work copyrighted by the smithy-rs authors.  
Kotlin port: Copyright (c) 2026 Sydney Renee and The Solace Project.

### Acknowledgments

Thanks to the [`smithy-lang/smithy-rs`](https://github.com/smithy-lang/smithy-rs) maintainers and contributors for the original Rust implementation. This port reproduces their work in Kotlin Multiplatform; bug reports about upstream design or behavior should go to the upstream repository.
