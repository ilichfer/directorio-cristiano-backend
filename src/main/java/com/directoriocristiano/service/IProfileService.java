package com.directoriocristiano.service;

import com.directoriocristiano.dto.UserProfileResponse;
import com.directoriocristiano.model.entity.User;

public interface IProfileService {
    UserProfileResponse activateEntrepreneur(User user, boolean acceptAgreement);
    UserProfileResponse deactivateEntrepreneur(User user);
}
