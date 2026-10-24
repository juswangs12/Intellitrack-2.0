package com.intellitrack.service;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class TrackingPolicyTest {
    private final LocalDateTime due=LocalDateTime.of(2026,10,1,23,59);
    @Test void deadlineBoundaryIsInclusive() {
        assertEquals("SUBMITTED_ON_TIME",TrackingPolicy.status(due,due,due.plusDays(1)));
        assertEquals("SUBMITTED_LATE",TrackingPolicy.status(due.plusSeconds(1),due,due.plusDays(1)));
        assertEquals("DUE_TODAY",TrackingPolicy.status(null,due,due));
        assertEquals("OVERDUE",TrackingPolicy.status(null,due,due.plusSeconds(1)));
        assertEquals("AWAITING_SUBMISSION",TrackingPolicy.status(null,due,due.minusDays(1)));
    }
    @Test void termRequiresConsecutiveYearsAndSemester() {
        assertEquals("2026-2027 / 2",TrackingPolicy.term("2026-2027 / 2"));
        assertThrows(IllegalArgumentException.class,()->TrackingPolicy.term("2026-2028 / 1"));
        assertThrows(IllegalArgumentException.class,()->TrackingPolicy.term("2026-2027 / 3"));
    }
    @Test void docsLinksRejectLookalikeHosts() {
        var links=new GoogleDocLinkService();
        assertEquals("https://docs.google.com/document/d/abcdefghijk/edit",links.canonical("https://docs.google.com/document/d/abcdefghijk/edit?usp=sharing"));
        for(String url:new String[]{"https://docs.google.com.evil.test/document/d/abcdefghijk","http://docs.google.com/document/d/abcdefghijk","https://drive.google.com/file/d/abcdefghijk","https://docs.google.com@evil.test/document/d/abcdefghijk"})
            assertThrows(IllegalArgumentException.class,()->links.canonical(url));
    }
    @Test void tokenEncryptionIsRandomizedAndAuthenticated() {
        var cipher=new TokenCipher(Base64.getEncoder().encodeToString(new byte[32]));
        var first=cipher.encrypt("refresh-secret");
        assertNotEquals(first,cipher.encrypt("refresh-secret"));
        assertEquals("refresh-secret",cipher.decrypt(first));
        byte[] corrupted=Base64.getDecoder().decode(first);corrupted[15]^=1;
        assertThrows(IllegalStateException.class,()->cipher.decrypt(Base64.getEncoder().encodeToString(corrupted)));
        assertFalse(new TokenCipher("").configured());
    }
    @Test void calendarIdsAreStableAndIsolated() {
        assertEquals(GoogleWorkspaceService.eventId(1L,2L),GoogleWorkspaceService.eventId(1L,2L));
        assertNotEquals(GoogleWorkspaceService.eventId(1L,2L),GoogleWorkspaceService.eventId(2L,2L));
        assertTrue(GoogleWorkspaceService.eventId(1L,2L).matches("[0-9a-f]{64}"));
    }
}
