

# URL Metadata Boilerplate

Minimal Spring Boot and browser UI scaffold for submitting a URL and returning
metadata in the shape `{ title, url, description, image }`.

## Structure

- `src/main/resources/static/index.html` — URL input and API request.
- `controller/MetadataController` — `POST /api/metadata` endpoint.
- `dto/` — request and response contracts.
- `service/` — metadata-fetching integration seam.

`DefaultMetadataService` fetches public HTTP(S) pages and extracts the title,
description, and site icon (falling back to Open Graph image and `/favicon.ico`).
The fetcher limits redirects, time, and response size, rejects non-public
addresses, and only accepts standard HTTP(S) ports.

## Run

Requires Java 21 and Maven.

```sh
mvn spring-boot:run
```

Open `http://localhost:8080`. The frontend posts JSON such as
`{"url":"https://youtube.com"}` to `POST /api/metadata`.