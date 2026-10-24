package com.intellitrack.service;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
class EmailDomainPolicyTest {
 @Test void allowlistUsesExactDomainsAndSupportsOptionalAtPrefix(){
  var service=new AuthService();ReflectionTestUtils.setField(service,"allowedEmailDomains","@school.edu, partner.edu");
  assertTrue(service.isEmailDomainAllowed("student@SCHOOL.EDU"));
  assertTrue(service.isEmailDomainAllowed("staff@partner.edu"));
  assertFalse(service.isEmailDomainAllowed("student@evilschool.edu"));
  assertFalse(service.isEmailDomainAllowed("student@school.edu.evil.test"));
  assertFalse(service.isEmailDomainAllowed(null));
 }
 @Test void emptyAllowlistPreservesUnrestrictedDomainPolicy(){
  var service=new AuthService();ReflectionTestUtils.setField(service,"allowedEmailDomains","");
  assertTrue(service.isEmailDomainAllowed("student@gmail.com"));
 }
}
