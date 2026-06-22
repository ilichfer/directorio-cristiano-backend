package com.directoriocristiano.service;

import com.directoriocristiano.dto.*;
import com.directoriocristiano.exception.ResourceNotFoundException;
import com.directoriocristiano.model.entity.Business;
import com.directoriocristiano.model.entity.Review;
import com.directoriocristiano.model.entity.ServiceItem;
import com.directoriocristiano.model.entity.User;
import com.directoriocristiano.repository.BusinessRepository;
import com.directoriocristiano.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BusinessServiceImpl implements IBusinessService {

    private final BusinessRepository businessRepository;
    private final ReviewRepository reviewRepository;

    public PageResponse<BusinessResponse> getAll(String search, String category, String zone,
                                                  int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        String searchParam = (search != null && !search.isBlank()) ? search : null;
        String categoryParam = (category != null && !category.isBlank() && !category.equals("Todos")) ? category : null;
        String zoneParam = (zone != null && !zone.isBlank() && !zone.equals("Todas")) ? zone : null;

        Page<Business> businessPage = businessRepository.searchBusinesses(
                searchParam, categoryParam, zoneParam, pageable);

        List<BusinessResponse> content = businessPage.getContent().stream()
                .map(BusinessResponse::from)
                .toList();

        return PageResponse.from(businessPage, content);
    }

    public BusinessResponse getById(UUID id) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio", "id", id));
        return BusinessResponse.from(business);
    }

    public List<BusinessResponse> getMyBusinesses(User user) {
        return businessRepository.findByOwnerId(user.getId()).stream()
                .map(BusinessResponse::from)
                .toList();
    }

    @Transactional
    public BusinessResponse create(BusinessRequest request, User owner) {
        Business business = Business.builder()
                .owner(owner)
                .name(request.name())
                .ownerName(owner.getDisplayName())
                .category(request.category())
                .zone(request.zone())
                .description(request.description())
                .slogan(request.slogan())
                .logoUrl(request.logoUrl())
                .coverUrl(request.coverUrl())
                .rating(BigDecimal.ZERO)
                .featured(false)
                .values(request.values() != null ? request.values() : new ArrayList<>())
                .contactPhone(request.contactPhone())
                .contactWhatsapp(request.contactWhatsapp())
                .contactEmail(request.contactEmail())
                .contactWebsite(request.contactWebsite())
                .contactAddress(request.contactAddress())
                .services(new ArrayList<>())
                .reviews(new ArrayList<>())
                .build();

        if (request.services() != null) {
            for (int i = 0; i < request.services().size(); i++) {
                var svcReq = request.services().get(i);
                ServiceItem service = ServiceItem.builder()
                        .business(business)
                        .name(svcReq.name())
                        .price(svcReq.price())
                        .description(svcReq.description())
                        .sortOrder(i)
                        .build();
                business.getServices().add(service);
            }
        }

        business = businessRepository.save(business);
        return BusinessResponse.from(business);
    }

    @Transactional
    public BusinessResponse update(UUID id, BusinessRequest request, User owner) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio", "id", id));

        if (!business.getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("No tienes permiso para modificar este negocio");
        }

        business.setName(request.name());
        business.setCategory(request.category());
        business.setZone(request.zone());
        business.setDescription(request.description());
        business.setSlogan(request.slogan());
        business.setLogoUrl(request.logoUrl());
        business.setCoverUrl(request.coverUrl());
        business.setValues(request.values() != null ? request.values() : new ArrayList<>());
        business.setContactPhone(request.contactPhone());
        business.setContactWhatsapp(request.contactWhatsapp());
        business.setContactEmail(request.contactEmail());
        business.setContactWebsite(request.contactWebsite());
        business.setContactAddress(request.contactAddress());

        business.getServices().clear();
        if (request.services() != null) {
            for (int i = 0; i < request.services().size(); i++) {
                var svcReq = request.services().get(i);
                ServiceItem service = ServiceItem.builder()
                        .business(business)
                        .name(svcReq.name())
                        .price(svcReq.price())
                        .description(svcReq.description())
                        .sortOrder(i)
                        .build();
                business.getServices().add(service);
            }
        }

        business = businessRepository.save(business);
        return BusinessResponse.from(business);
    }

    @Transactional
    public void delete(UUID id, User owner) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio", "id", id));

        if (!business.getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("No tienes permiso para eliminar este negocio");
        }

        businessRepository.delete(business);
    }

    @Transactional
    public ReviewResponse addReview(UUID businessId, ReviewRequest request, User user) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio", "id", businessId));

        Review review = Review.builder()
                .business(business)
                .user(user)
                .authorName(request.authorName())
                .rating(request.rating())
                .comment(request.comment())
                .honesty(request.honesty())
                .quality(request.quality())
                .punctuality(request.punctuality())
                .kindness(request.kindness())
                .verifiedClient(user != null)
                .build();

        review = reviewRepository.save(review);
        updateBusinessRating(business);

        return ReviewResponse.from(review);
    }

    public PageResponse<ReviewResponse> getReviews(UUID businessId, int page, int size) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Negocio", "id", businessId));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> reviewPage = reviewRepository.findByBusinessIdOrderByCreatedAtDesc(businessId, pageable);

        List<ReviewResponse> content = reviewPage.getContent().stream()
                .map(ReviewResponse::from)
                .toList();

        return PageResponse.from(reviewPage, content);
    }

    private void updateBusinessRating(Business business) {
        Double avg = reviewRepository.averageRatingByBusinessId(business.getId());
        business.setRating(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
        businessRepository.save(business);
    }
}
