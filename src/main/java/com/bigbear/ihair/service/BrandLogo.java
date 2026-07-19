package com.bigbear.ihair.service;

import java.time.Instant;

public record BrandLogo(byte[] data, String contentType, String checksum, Instant lastModified) {
    public BrandLogo {
        data = data.clone();
    }

    @Override
    public byte[] data() {
        return data.clone();
    }

    public String etag() {
        return "\"" + checksum + "\"";
    }
}
