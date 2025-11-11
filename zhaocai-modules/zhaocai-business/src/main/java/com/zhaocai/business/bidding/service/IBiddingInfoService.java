package com.zhaocai.business.bidding.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.bidding.domain.BiddingInfo;
import com.zhaocai.business.bidding.vo.req.*;
import com.zhaocai.business.bidding.vo.req.query.BiddingInfoQueryVO;
import com.zhaocai.business.bidding.vo.req.query.BiddingQuotationQueryVO;
import com.zhaocai.business.bidding.vo.res.*;

import java.text.ParseException;
import java.util.List;

/**
 * 投标单信息Service接口
 *
 * @author WH
 * @date 2024-05-24
 */
public interface IBiddingInfoService  extends IService<BiddingInfo> {

    /**
     * 查询投标单信息列表
     *
     * @param queryVO 查询参数
     * @return
     */
    List<BiddingInfoListVO> getList(BiddingInfoQueryVO queryVO);

    List<BiddingInfoOpenListVO> getOpenList(BiddingInfoQueryVO queryVO);

    /**
     * 查询投标单信息报价列表（评标）
     *
     * @param queryVO 查询参数
     * @return
     */
    List<BiddingQuotationListVO> getBiddingQuotationList(BiddingQuotationQueryVO queryVO) throws ParseException;

    /**
     * 获取投标单详情信息
     *
     * @param id 投标单id
     * @return
     */
    BiddingInfoDetailVO getInfo(Long id);

    /**
     * 获取投标记录及详情
     *
     * @param noticeId 招标公告id
     * @return
     */
    List<BiddingInfoDetailVO> getBiddingHistoryRecords(Long noticeId);

    /**
     * 财务收取保证金操作
     *
     * @param id 投标单id
     * @param collectDeposit 是否收取保证金
     * @return
     */
    boolean collectDeposit(Long id, Integer collectDeposit);

    /**
     * 进入下一环节（手动进入开标）
     *
     * @param noticeId 招标公告id
     * @return
     */
    boolean intoBidOpeningStage(Long noticeId);

    /**
     * 废标
     *
     * @param abandonBidVO 废标参数
     * @return
     */
    boolean abandonBid(AbandonBidVO abandonBidVO);

    /**
     * 废标操作（回标情况）
     *
     * @param abandonBidVO 废标参数
     * @return
     */
    boolean abandonBidMore(AbandonBidVO abandonBidVO);

    /**
     * 废标操作（采购方案同步废除）
     *
     * @param abandonBidVO 废标参数
     * @return
     */
    boolean abandonBidMoreScheme(AbandonBidVO abandonBidVO);

    /**
     * 废标操作（采购计划同步废除）
     *
     * @param abandonBidVO 废标参数
     * @return
     */
    boolean abandonBidMorePlan(AbandonBidVO abandonBidVO);

    /**
     * 开启评标
     *
     * @param noticeId 公告id
     * @return
     */
    boolean startEvaluat(Long noticeId);

    /**
     * 查询专家评标状态
     *
     * @param noticeId 公告id
     * @return
     */
    List<ExpertEvalStatusVO> getExpertEvalStatus(Long noticeId, Integer evalStatus);

    /**
     * 评标结束
     *
     * @param evaluatBidVO 评标结束参数
     * @return
     */
    boolean evaluatBid(EvaluatBidVO evaluatBidVO);

    String getTwiceTime(Long noticeId);

    /**
     * 开始调价（旧：二次洽商配置操作）
     *
     * @param twiceBidConfVO 开始调价（旧：二次洽商配置操作）参数
     * @return
     */
    boolean twiceBidConf(TwiceBidConfVO twiceBidConfVO);

    /**
     * 结束调价
     *
     * @param twiceBidConfVO 开始调价（旧：二次洽商配置操作）参数
     * @return
     */
    boolean twiceBidFinish(TwiceBidConOverVO twiceBidConfVO);

    /**
     * 评标附件上传
     *
     * @param uploadVO 参数
     * @return
     */
    boolean uploadEvalAttach(EvalAttachUploadVO uploadVO);


    /**
     * 获取采购方案的投标供应商信息
     * @param schemeId
     * @return
     */
    List<BiddingVendorVO> listBiddingVendor(Long schemeId);

    List<CalibrationReportListVO> getCalibrationReportList(Long noticeId);
    /**
     * 报价汇总
     * @param queryVO
     * @return
     */
    List<BiddingQuotationSummaryListVO> getBiddingQuotationSummary(BiddingQuotationQueryVO queryVO);

    /**
     * 评分汇总
     * @param noticeId
     * @param scoreType
     * @return
     */
    List<BidEvaluationVo>  getBidEvaluationList(Long noticeId, int scoreType);

    /**
     * 未评标专家催办操作
     * @param urgeExpertMesVO
     * @return
     */
    boolean urgeExpertMes(UrgeExpertMesVO urgeExpertMesVO);

    List<BiddingInfo> getMaxPriceVersion(Long noticeId, Long schemeId);

    /**
     * 评分汇总-新评分(2025-10-29修改为不分专家类型（商务、技术都需要进行评分)
     * @param noticeId
     * @param scoreType
     * @return
     */
    List<BidEvaluationVo>  getBidEvaluationListByNew(Long noticeId, int scoreType);
}
