package com.sfgroup81.tams.service;

import com.sfgroup81.tams.model.ApplicationStatusHistory;
import com.sfgroup81.tams.model.TAApplication;
import com.sfgroup81.tams.model.TAPosition;

import java.util.List;

public record ApplicantApplicationView(
        TAApplication application,
        TAPosition position,
        List<ApplicationStatusHistory> history
) {
}
