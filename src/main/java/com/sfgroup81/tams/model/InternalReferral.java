package com.sfgroup81.tams.model;

public record InternalReferral(
        String referralId,
        String userId,
        String recommenderName,
        String note,
        String taggedBy,
        String updatedAt
) {
}
