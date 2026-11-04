package com.intellitrack.service;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SubmissionWebLinkTest {
    @Test void repositoriesAndWebsitesAreSavedWithoutFetchingThem() {
        var service=new GoogleDocLinkService();
        for(String url:new String[]{"https://github.com/team/capstone", "https://gitlab.com/team/project/-/tree/main", "https://capstone.example.test/demo?view=student#overview"})
            assertEquals(url,service.verify("  "+url+"  "));
    }
    @Test void invalidOrUnsafeLinksAreRejectedBeforeAnyRequest() {
        var service=new GoogleDocLinkService();
        for(String url:new String[]{"", "javascript:alert(1)","data:text/html,hello","file:///tmp/test", "http://example.com", "https://user:password@example.com", "https://localhost", "https://127.0.0.1", "https://[::1]", "https://server.local", "https://example.com/a b", "https://example.com/\nheader", "https://example.com/"+"a".repeat(2048)})
            assertThrows(IllegalArgumentException.class,()->service.verify(url),url);
    }
    @Test void malformedGoogleDocumentStillUsesStrictDocumentValidation() {
        assertThrows(IllegalArgumentException.class,()->new GoogleDocLinkService().verify("https://docs.google.com/document/d/bad"));
    }
}
