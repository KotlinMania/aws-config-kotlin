#if canImport(Testing)
import Testing
import AwsConfig

@Suite("AwsConfig Swift Export Tests")
struct AwsConfigExportTests {
    @Test("AwsConfig swift module imported cleanly")
    func testSwiftModuleLoads() throws {
        #expect(Bool(true), "AwsConfig swift module imported cleanly")
    }

    @Test("RetryConfigError maxAttemptsMustNotBeZero has expected description")
    func testRetryConfigErrorMaxAttemptsZero() throws {
        let err = AwsConfig.retry.RetryConfigError.Companion.shared.maxAttemptsMustNotBeZero()
        let desc = err.errorDescription()
        #expect(desc.contains("invalid configuration: It is invalid to set max attempts to 0"))
    }

    @Test("RetryConfigError invalidRetryMode retains message")
    func testRetryConfigErrorInvalidRetryMode() throws {
        let err = AwsConfig.retry.RetryConfigError.Companion.shared.invalidRetryMode(message: "unknown mode: aggressive")
        let desc = err.errorDescription()
        #expect(desc == "invalid retry configuration")
    }

    @Test("RetryConfigError failedToParseMaxAttempts description")
    func testRetryConfigErrorFailedToParse() throws {
        let err = AwsConfig.retry.RetryConfigError.Companion.shared.failedToParseMaxAttempts(message: "not a number")
        let desc = err.errorDescription()
        #expect(desc == "failed to parse max attempts")
    }
}
#elseif canImport(XCTest)
import XCTest
import AwsConfig

final class AwsConfigExportTests: XCTestCase {
    func testSwiftModuleLoads() throws {
        XCTAssertTrue(true, "AwsConfig swift module imported cleanly")
    }

    func testRetryConfigErrorMaxAttemptsZero() throws {
        let err = AwsConfig.retry.RetryConfigError.Companion.shared.maxAttemptsMustNotBeZero()
        let desc = err.errorDescription()
        XCTAssertTrue(desc.contains("invalid configuration: It is invalid to set max attempts to 0"))
    }

    func testRetryConfigErrorInvalidRetryMode() throws {
        let err = AwsConfig.retry.RetryConfigError.Companion.shared.invalidRetryMode(message: "unknown mode: aggressive")
        let desc = err.errorDescription()
        XCTAssertEqual(desc, "invalid retry configuration")
    }

    func testRetryConfigErrorFailedToParse() throws {
        let err = AwsConfig.retry.RetryConfigError.Companion.shared.failedToParseMaxAttempts(message: "not a number")
        let desc = err.errorDescription()
        XCTAssertEqual(desc, "failed to parse max attempts")
    }
}
#endif
