package com.tus.binary.suite.dto;

public record ValidationResult(String protocol, int size, boolean match, String original, String decoded) {
}
