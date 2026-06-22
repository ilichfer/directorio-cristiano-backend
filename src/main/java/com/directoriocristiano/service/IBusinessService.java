package com.directoriocristiano.service;

import com.directoriocristiano.dto.*;
import com.directoriocristiano.model.entity.User;

import java.util.List;
import java.util.UUID;

public interface IBusinessService {
    PageResponse<BusinessResponse> getAll(String search, String category, String zone,
                                          int page, int size, String sortBy, String sortDir);
    BusinessResponse getById(UUID id);
    List<BusinessResponse> getMyBusinesses(User user);
    BusinessResponse create(BusinessRequest request, User owner);
    BusinessResponse update(UUID id, BusinessRequest request, User owner);
    void delete(UUID id, User owner);
    ReviewResponse addReview(UUID businessId, ReviewRequest request, User user);
    PageResponse<ReviewResponse> getReviews(UUID businessId, int page, int size);
}
