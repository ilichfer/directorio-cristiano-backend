package com.directoriocristiano.service;

import com.directoriocristiano.dto.ChangeRequestDetail;
import com.directoriocristiano.dto.ModerationEventResponse;
import com.directoriocristiano.dto.ModeratedBusinessItem;
import com.directoriocristiano.dto.ModerationInboxItem;
import com.directoriocristiano.dto.PageResponse;
import com.directoriocristiano.model.entity.User;

import java.util.List;
import java.util.UUID;

public interface IModerationService {
    PageResponse<ModerationInboxItem> inbox(String status, int page, int size, User moderator);
    List<ModerationEventResponse> history(UUID businessId);
    ChangeRequestDetail detail(UUID requestId, User moderator);
    ChangeRequestDetail approve(UUID requestId, Integer expectedVersion, User moderator);
    ChangeRequestDetail reject(UUID requestId, Integer expectedVersion, String reason, User moderator);
    PageResponse<ModeratedBusinessItem> listByStatus(String status, int page, int size, User moderator);
    void suspend(UUID businessId, String reason, User moderator);
    void reactivate(UUID businessId, User moderator);
}
