package com.bigbear.ihair.service;

public record ValidatedPng(byte[] data, int width, int height, String checksum) {
}
