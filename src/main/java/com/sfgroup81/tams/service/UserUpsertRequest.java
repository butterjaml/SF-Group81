package com.sfgroup81.tams.service;

import com.sfgroup81.tams.model.TACategory;
import com.sfgroup81.tams.model.UserRole;

public record UserUpsertRequest(
        String name,
        String staffOrStudentId,
        String email,
        String password,
        UserRole role,
        TACategory taCategory
) {
}
