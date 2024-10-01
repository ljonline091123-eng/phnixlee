package com.zhaocai.business.bidding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.bidding.domain.BiddingInfo;
import com.zhaocai.business.bidding.vo.req.query.BiddingInfoQueryVO;
import com.zhaocai.business.bidding.vo.req.query.BiddingQuotationQueryVO;
import com.zhaocai.business.bidding.vo.res.*;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 投标单信息Mapper接口
 *
 * @author WH
 * @date 2024-05-24
 */
public interface BiddingInfoMapper extends BaseMapper<BiddingInfo> {

    /**
     * 查询投标单信息列表
     *
     * @param query 查询参数
     * @return
     */
    List<BiddingInfoListVO> selectBiddingInfoList(@Param("query") BiddingInfoQueryVO query);

    List<BiddingInfoOpenListVO> selectBiddingInfoOpenList(@Param("query") BiddingInfoQueryVO query);

    /**
     * 查询投标单信息报价列表（评标）
     *
     * @param query 查询参数
     * @return
     */
    List<BiddingQuotationListVO> findBiddingQuotationList(@Param("query") BiddingQuotationQueryVO query);

    List<CalibrationReportListVO> findCalibrationReportList(@Param("noticeId") Long noticeId,
                                                            @Param("noticeStatus") Integer noticeStatus,
                                                            @Param("biddingStatus") Integer biddingStatus);


    List<BidEvaluationExpertScoreVo> findBidEvaluationList(@Param("noticeId") Long noticeId);
}
