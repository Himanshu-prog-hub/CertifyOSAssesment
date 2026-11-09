package com.certifyos.assessment.service;

import com.certifyos.assessment.dto.MetadataResponse;
import java.net.URI;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DefaultMetadataServiceTest {

    private final DefaultMetadataService service = new DefaultMetadataService();

    @Test
    void extractsPageMetadataAndResolvesRelativeIcon() {
        String html = """
                <html>
                  <head>
                    <title>Fallback title</title>
                    <meta property="og:title" content="Example site">
                    <meta name="description" content="A short summary">
                    <link rel="icon" href="/assets/logo.png">
                  </head>
                </html>
                """;

        MetadataResponse metadata = service.parseMetadata(
                html, URI.create("https://example.com/docs/page"), "https://example.com/docs/page"
        );

        Assertions.assertEquals("Example site", metadata.title());
        Assertions.assertEquals("A short summary", metadata.description());
        Assertions.assertEquals("https://example.com/assets/logo.png", metadata.image());
        Assertions.assertEquals("https://example.com/docs/page", metadata.url());
    }

    @Test
    void fallsBackToPageTitleAndFavicon() {
        MetadataResponse metadata = service.parseMetadata(
                "<html><head><title>Page title</title></head></html>",
                URI.create("https://example.com/docs/page"),
                "https://example.com/docs/page"
        );

        Assertions.assertEquals("Page title", metadata.title());
        Assertions.assertEquals("https://example.com/favicon.ico", metadata.image());
    }
}
