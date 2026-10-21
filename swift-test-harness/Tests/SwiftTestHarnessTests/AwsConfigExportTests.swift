#if canImport(Testing)
import Testing
import AwsConfig

@Suite("AwsConfig Swift Export Tests")
struct AwsConfigExportTests {
    @Test("AwsConfig swift module imported cleanly")
    func testSwiftModuleLoads() throws {
        #expect(Bool(true), "AwsConfig swift module imported cleanly")
    }
}
#elseif canImport(XCTest)
import XCTest
import AwsConfig

final class AwsConfigExportTests: XCTestCase {
    func testSwiftModuleLoads() throws {
        XCTAssertTrue(true, "AwsConfig swift module imported cleanly")
    }
}
#endif
