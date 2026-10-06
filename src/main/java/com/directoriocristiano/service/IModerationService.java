package com.directoriocristiano.service;

import com.directoriocristiano.dto.ChangeRequestDetail;
import com.directoriocristiano.model.entity.User;

import java.util.UUID;

public interface IModerationService {
    ChangeRequestDetail detail(UUID requestId, User moderator);
    ChangeRequestDetail approve(UUID requestId, Integer expectedVersion, User moderator);
    ChangeRequestDetail reject(UUID requestId, Integer expectedVersion, String reason, User moderator);
}
