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
    public String verify(String url) {
        String canonical=canonical(url);
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
