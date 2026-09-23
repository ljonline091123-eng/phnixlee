package com.zhaocai.business.bidding.service;

import com.zhaocai.business.bidding.domain.BiddingResult;

/** 旧定标结果向定标聚合的兼容同步。 */
public interface IAwardCompatibilityService {
    void syncResult(BiddingResult result);
}
