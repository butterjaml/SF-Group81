package com.sfgroup81.tams.model;

import java.util.Arrays;
import java.util.List;

public record InternalReferral(
        String referralId,
        String userId,
        String recommenderName,
        String note,
        String taggedBy,
        String updatedAt
) {
    public List<String> recommenderNames() {
        if (recommenderName == null || recommenderName.isBlank()) {
            return List.of();
        }
        return Arrays.stream(recommenderName.split(";"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }

    public String displayRecommenders() {
        return String.join("; ", recommenderNames());
    }

    public boolean hasRecommenders() {
        return !recommenderNames().isEmpty();
    }
}
