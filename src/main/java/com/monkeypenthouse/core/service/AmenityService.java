package com.monkeypenthouse.core.service;

import com.monkeypenthouse.core.dto.AmenityDTO.*;
import com.monkeypenthouse.core.vo.*;
import org.jets3t.service.CloudFrontServiceException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface AmenityService {
    void add(SaveReqDTO amenityDTO) throws Exception;
    GetByIdResponseVo getById(Long id) throws CloudFrontServiceException, IOException;

    GetPageResponseVo getAmenitiesDibsOn(UserDetails userDetails, Pageable pageable) throws CloudFrontServiceException, IOException;

    GetPageResponseVo getPageByRecommended(Pageable pageable) throws CloudFrontServiceException, IOException;

    GetPageResponseVo getPage(Pageable pageable) throws CloudFrontServiceException, IOException;

    GetPageResponseVo getPageByCategory(Long category, Pageable pageable) throws CloudFrontServiceException, IOException;

    GetTicketsOfAmenityResponseVo getTicketsOfAmenity(Long amenityId);

    void updateStatusOfAmenity();

    GetViewedResponseVo getViewed(List<Long> amenityIds) throws CloudFrontServiceException, IOException;
}
