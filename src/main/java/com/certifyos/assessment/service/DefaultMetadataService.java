package com.certifyos.assessment.service;

import com.certifyos.assessment.dto.MetadataResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DefaultMetadataService implements MetadataService {

    private static final int TIMEOUT_MILLIS = 5_000;
    private static final int MAX_RESPONSE_BYTES = 1_048_576;
    private static final int MAX_REDIRECTS = 5;

    @Override
    public MetadataResponse fetchMetadata(String url) {
        URI requestedUri = parseAndValidateUrl(url);
        URI pageUri = requestedUri;

        try {
            for (int redirectCount = 0; ; redirectCount++) {
                validatePublicHost(pageUri);
                Connection.Response response = Jsoup.connect(pageUri.toString())
                        .userAgent("CertiFyOS-MetadataFetcher/1.0")
                        .timeout(TIMEOUT_MILLIS)
                        .maxBodySize(MAX_RESPONSE_BYTES)
                        .followRedirects(false)
                        .ignoreHttpErrors(true)
                        .ignoreContentType(true)
                        .execute();

                if (isRedirect(response.statusCode())) {
                    if (redirectCount >= MAX_REDIRECTS) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_GATEWAY, "The website redirected too many times"
                        );
                    }
                    String location = response.header("Location");
                    if (location == null || location.isBlank()) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_GATEWAY, "The website returned an invalid redirect"
                        );
                    }
                    pageUri = parseAndValidateUrl(pageUri.resolve(location).toString());
                    continue;
                }

                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_GATEWAY,
                            "The website returned HTTP " + response.statusCode()
                    );
                }
                if (!isHtml(response.contentType())) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_GATEWAY, "The URL did not return an HTML page"
                    );
                }

                return parseMetadata(response.body(), pageUri, requestedUri.toString());
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Could not fetch metadata from the website", exception
            );
        }
    }

    MetadataResponse parseMetadata(String html, URI pageUri, String requestedUrl) {
        Document document = Jsoup.parse(html, pageUri.toString());
        String title = firstNonBlank(
                metaContent(document, "meta[property=og:title]"),
                metaContent(document, "meta[name=twitter:title]"),
                document.title()
        );
        String description = firstNonBlank(
                metaContent(document, "meta[name=description]"),
                metaContent(document, "meta[property=og:description]"),
                metaContent(document, "meta[name=twitter:description]")
        );
        String image = firstNonBlank(
                siteIcon(document, pageUri),
                metaUrl(document, "meta[property=og:image]", pageUri),
                pageUri.resolve("/favicon.ico").toString()
        );

        return new MetadataResponse(title, requestedUrl, description, image);
    }

    private static URI parseAndValidateUrl(String url) {
        try {
            URI uri = new URI(url == null ? "" : url.trim());
            String scheme = uri.getScheme();
            if (scheme == null
                    || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    || uri.getHost() == null
                    || uri.getUserInfo() != null
                    || (uri.getPort() != -1
                    && uri.getPort() != 80
                    && uri.getPort() != 443)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "URL must be an HTTP(S) URL on a standard port"
                );
            }
            return uri;
        } catch (URISyntaxException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid URL", exception);
        }
    }

    private static void validatePublicHost(URI uri) throws UnknownHostException {
        InetAddress[] addresses = InetAddress.getAllByName(uri.getHost());
        for (InetAddress address : addresses) {
            if (!isPublicAddress(address)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "URL host must resolve to a public IP address"
                );
            }
        }
    }

    private static boolean isPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return false;
        }

        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int first = Byte.toUnsignedInt(bytes[0]);
            int second = Byte.toUnsignedInt(bytes[1]);
            int third = Byte.toUnsignedInt(bytes[2]);
            return first != 0
                    && first != 10
                    && first != 127
                    && first < 224
                    && !(first == 100 && second >= 64 && second <= 127)
                    && !(first == 169 && second == 254)
                    && !(first == 172 && second >= 16 && second <= 31)
                    && !(first == 192 && (second == 0 || second == 168))
                    && !(first == 192 && second == 88 && third == 99)
                    && !(first == 198 && (second == 18 || second == 19))
                    && !(first == 198 && second == 51 && third == 100)
                    && !(first == 203 && second == 0 && third == 113);
        }

        int first = Byte.toUnsignedInt(bytes[0]);
        int second = Byte.toUnsignedInt(bytes[1]);
        int third = Byte.toUnsignedInt(bytes[2]);
        return first >= 0x20
                && first <= 0x3f
                && !(first == 0x20 && second == 0x01 && third == 0x0d && bytes[3] == (byte) 0xb8);
    }

    private static boolean isRedirect(int statusCode) {
        return statusCode == 301
                || statusCode == 302
                || statusCode == 303
                || statusCode == 307
                || statusCode == 308;
    }

    private static boolean isHtml(String contentType) {
        return contentType != null
                && (contentType.toLowerCase(Locale.ROOT).contains("text/html")
                || contentType.toLowerCase(Locale.ROOT).contains("application/xhtml+xml"));
    }

    private static String metaContent(Document document, String selector) {
        Element element = document.selectFirst(selector);
        return element == null ? null : element.attr("content").trim();
    }

    private static String metaUrl(Document document, String selector, URI pageUri) {
        String value = metaContent(document, selector);
        return value == null || value.isBlank() ? null : pageUri.resolve(value).toString();
    }

    private static String siteIcon(Document document, URI pageUri) {
        for (Element link : document.select("link[rel][href]")) {
            if (link.attr("rel").toLowerCase(Locale.ROOT).contains("icon")) {
                return pageUri.resolve(link.attr("href")).toString();
            }
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
