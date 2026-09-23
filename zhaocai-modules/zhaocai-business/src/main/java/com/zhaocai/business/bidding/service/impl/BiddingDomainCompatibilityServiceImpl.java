package com.zhaocai.business.bidding.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhaocai.business.bidding.domain.BiddingInfo;
import com.zhaocai.business.bidding.domain.model.BidSubmission;
import com.zhaocai.business.bidding.domain.model.BidSubmissionVersion;
import com.zhaocai.business.bidding.mapper.model.BidSubmissionMapper;
import com.zhaocai.business.bidding.mapper.model.BidSubmissionVersionMapper;
import com.zhaocai.business.bidding.service.IBiddingDomainCompatibilityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/** 兼容阶段的增量同步实现，旧表仍是业务主写模型。 */
@Service
public class BiddingDomainCompatibilityServiceImpl implements IBiddingDomainCompatibilityService {

    private final BidSubmissionMapper submissionMapper;
    private final BidSubmissionVersionMapper versionMapper;

    public BiddingDomainCompatibilityServiceImpl(BidSubmissionMapper submissionMapper,
                                                 BidSubmissionVersionMapper versionMapper) {
        this.submissionMapper = submissionMapper;
        this.versionMapper = versionMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncSubmission(BiddingInfo biddingInfo) {
        if (biddingInfo == null || biddingInfo.getNoticeId() == null || biddingInfo.getVendorId() == null) {
            return;
        }
        Date now = biddingInfo.getUpdateTime() == null ? new Date() : biddingInfo.getUpdateTime();
        BidSubmission submission = submissionMapper.selectOne(new LambdaQueryWrapper<BidSubmission>()
                .eq(BidSubmission::getNoticeId, biddingInfo.getNoticeId())
                .eq(BidSubmission::getVendorId, biddingInfo.getVendorId())
                .last("limit 1"));
        if (submission == null) {
            submission = new BidSubmission();
            submission.setNoticeId(biddingInfo.getNoticeId());
            submission.setSchemeId(biddingInfo.getSchemeId());
            submission.setVendorId(biddingInfo.getVendorId());
            submission.setStatus(BidSubmission.DRAFT);
            submissionMapper.insert(submission);
        } else if (submission.getSchemeId() == null && biddingInfo.getSchemeId() != null) {
            submission.setSchemeId(biddingInfo.getSchemeId());
            submissionMapper.updateById(submission);
        }

        int versionNo = biddingInfo.getTwiceQuotVersion() == null || biddingInfo.getTwiceQuotVersion() < 1
                ? 1 : biddingInfo.getTwiceQuotVersion();
        BidSubmissionVersion version = versionMapper.selectOne(new LambdaQueryWrapper<BidSubmissionVersion>()
                .eq(BidSubmissionVersion::getSubmissionId, submission.getId())
                .eq(BidSubmissionVersion::getVersionNo, versionNo)
                .last("limit 1"));
        if (version == null) {
            version = new BidSubmissionVersion();
            version.setSubmissionId(submission.getId());
            version.setVersionNo(versionNo);
            versionMapper.insert(version);
        }
        version.setTaxPrice(biddingInfo.getTaxPrice());
        version.setNotTaxPrice(biddingInfo.getNotTaxPrice());
        version.setContact(biddingInfo.getContact());
        version.setPhone(biddingInfo.getPhone());
        boolean submitted = Integer.valueOf(1).equals(biddingInfo.getSubmitStatus())
                || biddingInfo.getBiddingStatus() != null;
        version.setStatus(submitted ? 1 : 0);
        version.setSubmittedAt(submitted ? now : null);
        versionMapper.updateById(version);

        submission.setCurrentVersionId(version.getId());
        submission.setStatus(submitted ? BidSubmission.SUBMITTED : BidSubmission.DRAFT);
        submission.setSubmittedAt(submitted ? now : null);
        submissionMapper.updateById(submission);
    }
}
