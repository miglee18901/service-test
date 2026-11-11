package org.example.utils;

import java.net.URI;

public final class HttpClientUtil {
    private HttpClientUtil() { }

    public static URI getSafeHttpUri(String host, String path) {
        try {
            URI base = URI.create(host);
            if (base.getHost() == null || !("http".equalsIgnoreCase(base.getScheme())
                    || "https".equalsIgnoreCase(base.getScheme()))) return null;
            URI relative = URI.create(path);
            if (relative.isAbsolute() || relative.getRawAuthority() != null) return null;
            return base.resolve(relative);
        } catch (Exception exception) {
            return null;
        }
    }
}
