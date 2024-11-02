package com.zhaocai.business.bidding.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.bidding.domain.BiddingResult;
import com.zhaocai.business.bidding.vo.req.CalibrationEntranceVO;
import com.zhaocai.business.bidding.vo.req.CalibrationReleaseVO;
import com.zhaocai.business.bidding.vo.req.CalibrationVO;
import com.zhaocai.business.bidding.vo.req.ResultReleasVO;
import com.zhaocai.business.bidding.vo.res.BidResultVO;
import com.zhaocai.business.bidding.vo.res.BiddingResultDetailVO;
import com.zhaocai.business.bidding.vo.res.BiddingResultListVO;
import com.zhaocai.business.bidding.vo.res.WinningBidResultVO;
import com.zhaocai.business.process.service.IPBMOverrideService;
import com.zhaocai.business.process.service.IProcessBusinessBaseService;
import com.zhaocai.business.procurement.vo.res.ProcurementSchemeBiddingVendorVO;

import java.util.List;

/**
 * 投标结果信息Service接口
 *
 * @author WH
 * @date 2024-05-24
 */
public interface IBiddingResultService  extends IService<BiddingResult> , IProcessBusinessBaseService, IPBMOverrideService {

    /**
     * 详情
     *
     * @param id
     * @return
     */
    BiddingResultDetailVO detail(Long id);

    /**
     * 定标
     *
     * @param entranceVO
     * @return
     */
    boolean calibration(CalibrationEntranceVO entranceVO);

    /**
     * 查询定标供应商数据列表（中标公示）
     *
     * @param noticeId 招标公告id
     * @return
     */
    List<BiddingResultListVO> getBiddingResult(Long noticeId);

    boolean calibrationRelease(CalibrationReleaseVO calibrationReleaseVO);

    /**
     * 获取采购方案的投标供应商
     * @param schemeId
     * @return
     */
    List<ProcurementSchemeBiddingVendorVO> listBiddingVendorBySchemeId(Long schemeId);
    /**
     * 查询中标结果数据列表（结果发布）
     *
     * @param noticeId 招标公告id
     * @return
     */
    List<WinningBidResultVO> getWinningBidResult(Long noticeId);

    /**
     * 结果发布
     *
     * @param resultReleasVO
     * @return
     */
    boolean winningBidResultRelease(ResultReleasVO resultReleasVO);

    /**
     * 查询中标结果数据列表（招标结果）
     *
     * @param noticeId
     * @return
     */
    List<BidResultVO> getBidResult(Long noticeId);

}
