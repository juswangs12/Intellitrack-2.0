package com.intellitrack.service;

import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.regex.Pattern;

@Service
public class GoogleDocLinkService {
    private static final Pattern DOC = Pattern.compile("^https://docs\\.google\\.com/document/d/([A-Za-z0-9_-]{10,200})(?:/[^?#]*)?(?:\\?[^#]*)?(?:#.*)?$");
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
    public String canonical(String url) {
        var m=DOC.matcher(url==null?"":url.trim());
        if(!m.matches()) throw new IllegalArgumentException("Use a Google Docs document link starting with https://docs.google.com/document/d/");
        return "https://docs.google.com/document/d/"+m.group(1)+"/edit";
    }
    static String validateWebLink(String url) {
        String link=url==null?"":url.trim();
        if(link.isEmpty() || link.length()>2048) throw new IllegalArgumentException("Enter an HTTPS submission link of at most 2048 characters.");
        try {
            URI uri=URI.create(link);
            String host=uri.getHost()==null?"":uri.getHost().toLowerCase(java.util.Locale.ROOT);
            if(!"https".equalsIgnoreCase(uri.getScheme()) || uri.getRawUserInfo()!=null || !host.contains(".")
                || host.endsWith(".") || host.endsWith(".localhost") || host.endsWith(".local") || host.endsWith(".internal")
                || host.matches("[0-9.]+") || host.contains(":"))
                throw new IllegalArgumentException();
            return link;
        } catch(IllegalArgumentException ex) {
            throw new IllegalArgumentException("Use a complete HTTPS link to Google Docs, a repository or a website. Local addresses and links containing passwords are not accepted.");
        }
    }
    public String verify(String url) {
        String link=validateWebLink(url);
        URI uri=URI.create(link);
        // Only Google Docs has a fixed-origin access probe; never fetch arbitrary submitted URLs.
        if (!"docs.google.com".equalsIgnoreCase(uri.getHost()) || !uri.getPath().startsWith("/document/")) return link;
        String canonical=canonical(link);
        // Anonymous request, fixed Google origin, no cookies or user tokens. Discard body: AI never reads documents.
        String probe=canonical.substring(0,canonical.length()-4)+"export?format=txt";
        try {
            var response=client.send(HttpRequest.newBuilder(URI.create(probe)).timeout(Duration.ofSeconds(20)).GET().build(), HttpResponse.BodyHandlers.discarding());
            for (int redirects=0; redirects<3 && response.statusCode()/100==3; redirects++) {
                URI next=URI.create(probe).resolve(response.headers().firstValue("location").orElse(""));
                String host=next.getHost();
                if (!"https".equals(next.getScheme()) || host==null || !(host.equals("docs.google.com") || host.endsWith(".googleusercontent.com"))) break;
                probe=next.toString();
                response=client.send(HttpRequest.newBuilder(next).timeout(Duration.ofSeconds(20)).GET().build(), HttpResponse.BodyHandlers.discarding());
            }
            String type=response.headers().firstValue("content-type").orElse("");
            if(response.statusCode()!=200 || !type.toLowerCase().startsWith("text/plain"))
                throw new IllegalArgumentException("Document access could not be verified. Set Google Docs sharing to Anyone with the link — Viewer, then try again.");
            return canonical;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt(); throw new IllegalArgumentException("Document verification interrupted; try again");
        } catch (java.io.IOException ex) { throw new IllegalArgumentException("Google Docs could not be reached; your submission has not been saved. Try again."); }
    }
}
