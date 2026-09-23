package com.zhaocai.business.bidding.domain.model;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/** 招投标核心领域对象的状态与计算规则测试。 */
public class BiddingDomainModelTest {

    @Test
    public void submissionShouldReferenceCurrentVersionWhenSubmitted() {
        BidSubmission submission = new BidSubmission();
        submission.setStatus(BidSubmission.DRAFT);

        submission.submit(101L, new Date());

        assertEquals(Integer.valueOf(BidSubmission.SUBMITTED), submission.getStatus());
        assertEquals(Long.valueOf(101L), submission.getCurrentVersionId());
        assertNotNull(submission.getSubmittedAt());
    }

    @Test(expected = IllegalStateException.class)
    public void withdrawnSubmissionCannotBeSubmittedAgain() {
        BidSubmission submission = new BidSubmission();
        submission.setStatus(BidSubmission.WITHDRAWN);
        submission.submit(101L, new Date());
    }

    @Test
    public void evaluationSheetShouldCalculateTotalBeforeSubmission() {
        EvaluationSheet sheet = new EvaluationSheet();
        sheet.setBusinessScore(new BigDecimal("20.5"));
        sheet.setTechnicalScore(new BigDecimal("50"));
        sheet.setQuotationScore(new BigDecimal("19.5"));

        sheet.submit(new Date());

        assertEquals(new BigDecimal("90.0"), sheet.getTotalScore());
        assertEquals(Integer.valueOf(1), sheet.getStatus());
    }

    @Test
    public void awardDecisionShouldFollowDecisionThenPublication() {
        AwardDecision decision = new AwardDecision();
        decision.setStatus(AwardDecision.DRAFT);

        decision.decide(201L, 301L, "综合评分第一名", new Date());
        decision.markPublished();

        assertEquals(Integer.valueOf(AwardDecision.PUBLISHED), decision.getStatus());
        assertEquals(Long.valueOf(201L), decision.getSelectedCandidateId());
    }

    @Test(expected = IllegalStateException.class)
    public void publicityShouldRejectInvalidPeriod() {
        AwardPublicity publicity = new AwardPublicity();
        publicity.setStartTime(new Date(2_000L));
        publicity.setEndTime(new Date(1_000L));
        publicity.publish(new Date());
    }

    @Test
    public void evaluationAssignmentShouldCompleteOnlyAfterStarting() {
        EvaluationAssignment assignment = new EvaluationAssignment();
        assignment.setStatus(EvaluationAssignment.PENDING);
        assignment.start();
        assignment.complete(new Date());

        assertEquals(Integer.valueOf(EvaluationAssignment.COMPLETED), assignment.getStatus());
        assertNotNull(assignment.getCompletedAt());
    }
}
