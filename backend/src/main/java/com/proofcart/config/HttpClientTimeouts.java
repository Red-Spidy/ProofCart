package com.proofcart.config;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * Every RestClient built with plain {@code RestClient.builder().build()} (no request factory)
 * has no timeout at all — a genuinely unreachable host (packets silently dropped, not a fast
 * connection-refused) hangs the calling thread far longer than any caller's own fallback logic
 * accounts for, since the fallback only runs once an exception is thrown. "Unreachable" and
 * "returned an error" are different failure modes; this makes the first one fail as fast as the
 * second so a fallback (deterministic parser, health-check result, etc.) actually gets a chance
 * to run instead of the request just hanging.
 */
public final class HttpClientTimeouts {

    private HttpClientTimeouts() {
    }

    public static ClientHttpRequestFactory bounded(int connectTimeoutMs, int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return factory;
    }
}
