package com.zhaocai.business.bidding.controller;

import com.zhaocai.business.bidding.service.IBiddingInfoService;
import com.zhaocai.business.bidding.vo.req.*;
import com.zhaocai.business.bidding.vo.req.query.BiddingInfoQueryVO;
import com.zhaocai.business.bidding.vo.req.query.BiddingQuotationQueryVO;
import com.zhaocai.business.bidding.vo.res.*;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 投标单信息
 *
 * @author WH
 * @date 2024-05-24
 */
@RestController
@RequestMapping("/info")
@Api(value = "投标单信息", tags = "投标单信息接口")
public class BiddingInfoController extends BladeController {

    @Autowired
    private IBiddingInfoService biddingInfoService;

    /**
     * 查询投标单信息列表（回标情况）
     */
    @PostMapping("/getList")
    public ResultData<List<BiddingInfoListVO>> getList(@RequestBody BiddingInfoQueryVO queryVO) {
        List<BiddingInfoListVO> list = biddingInfoService.getList(queryVO);
        return ResultData.data(list);
    }

    /**
     * 查询投标单信息列表（开标）
     */
    @PostMapping("/getOpenList")
    public ResultData<List<BiddingInfoOpenListVO>> getOpenList(@RequestBody BiddingInfoQueryVO queryVO) {
        List<BiddingInfoOpenListVO> list = biddingInfoService.getOpenList(queryVO);
        return ResultData.data(list);
    }

    /**
     * 查询投标单信息报价列表（评标、二次洽商）
     */
    @PostMapping("/getBiddingQuotationList")
    public ResultData<List<BiddingQuotationListVO>> getBiddingQuotationList(@RequestBody BiddingQuotationQueryVO queryVO) {
        List<BiddingQuotationListVO> list = biddingInfoService.getBiddingQuotationList(queryVO);
        return ResultData.data(list);
    }

    /**
     * 获取投标单详情信息 business/info/{id}
     */
    @GetMapping(value = "/{id}")
    public ResultData<BiddingInfoDetailVO> getInfo(@PathVariable(value = "id", required = false) Long id)
    {
        return ResultData.data(biddingInfoService.getInfo(id));
    }

    /**
     * 获取投标记录及详情
     */
    @GetMapping(value = "/getBiddingHistoryRecords")
    public ResultData<List<BiddingInfoDetailVO>> getBiddingHistoryRecords(@ApiParam(value = "招标公告id", required = true) @RequestParam Long noticeId)
    {
        return ResultData.data(biddingInfoService.getBiddingHistoryRecords(noticeId));
    }

    /**
     * 财务收取保证金操作
     */
    @PostMapping("/collectDeposit")
    public ResultData collectDeposit(@RequestParam Long id, @RequestParam Integer collectDeposit) {
        return ResultData.status(biddingInfoService.collectDeposit(id, collectDeposit));
    }

    /**
     * 进入下一环节（手动进入开标）
     */
    @PostMapping("/intoBidOpeningStage")
    public ResultData intoBidOpeningStage(@RequestParam Long noticeId) {
        return ResultData.status(biddingInfoService.intoBidOpeningStage(noticeId));
    }

    /**
     * 废标操作（回标情况）
     */
    @PostMapping("/abandonBid")
    public ResultData abandonBid(@RequestBody AbandonBidVO abandonBidVO) {
        return ResultData.status(biddingInfoService.abandonBid(abandonBidVO));
    }

    /**
     * 废标操作（招标管理列表）
     */
    @PostMapping("/abandonBidMore")
    public ResultData abandonBidMore(@RequestBody AbandonBidVO abandonBidVO) {
        return ResultData.status(biddingInfoService.abandonBidMore(abandonBidVO));
    }

    /**
     * 废标操作（招标管理列表）选择废除到 采购方案
     */
    @PostMapping("/abandonBidMoreScheme")
    public ResultData abandonBidMoreScheme(@RequestBody AbandonBidVO abandonBidVO) {
        return ResultData.status(biddingInfoService.abandonBidMoreScheme(abandonBidVO));
    }

    /**
     * 废标操作（招标管理列表）选择废除到 采购方案到 采购计划
     */
    @PostMapping("/abandonBidMorePlan")
    public ResultData abandonBidMorePlan(@RequestBody AbandonBidVO abandonBidVO) {
        return ResultData.status(biddingInfoService.abandonBidMorePlan(abandonBidVO));
    }

    /**
     * 开启评标
     */
    @PostMapping("/startEvaluat")
    public ResultData startEvaluat(@RequestParam Long noticeId) {
        return ResultData.status(biddingInfoService.startEvaluat(noticeId));
    }

    /**
     * 查询专家评标状态
     */
    @PostMapping("/getExpertEvalStatus")
    public ResultData<List<ExpertEvalStatusVO>> getExpertEvalStatus(@RequestParam Long noticeId,
                                                                    @RequestParam(name = "evalStatus", required = false) Integer evalStatus) {
        return ResultData.data(biddingInfoService.getExpertEvalStatus(noticeId, evalStatus));
    }

    /**
     * 评标结束操作
     */
    @PostMapping("/evaluatBid")
    public ResultData evaluatBid(@RequestBody EvaluatBidVO evaluatBidVO) {
        return ResultData.status(biddingInfoService.evaluatBid(evaluatBidVO));
    }


    /**
     * 查询二次报价截止时间配置
     */
    @GetMapping("/getTwiceTime")
    public ResultData getTwiceTime(@ApiParam(value = "招标公告主键id", required = true) @RequestParam Long noticeId) {
        return ResultData.data(biddingInfoService.getTwiceTime(noticeId));
    }

    /**
     * 开始调价（旧：二次洽商配置操作） 采购端二次报价
     */
    @PostMapping("/twiceBidConf")
    public ResultData twiceBidConf(@RequestBody @Valid TwiceBidConfVO twiceBidConfVO) {
        return ResultData.status(biddingInfoService.twiceBidConf(twiceBidConfVO));
    }

    /**
     * 结束调价
     */
    @PostMapping("/twiceBidFinish")
    public ResultData twiceBidFinish(@RequestBody @Valid TwiceBidConOverVO twiceBidConfVO) {
        return ResultData.status(biddingInfoService.twiceBidFinish(twiceBidConfVO));
    }

    /**
     * 评标附件上传
     */
    @PostMapping("/uploadEvalAttach")
    @ApiOperation(value = "评标附件上传", notes = "传入bidVO")
    public ResultData uploadEvalAttach(@Valid @RequestBody EvalAttachUploadVO uploadVO) {
        return ResultData.status(biddingInfoService.uploadEvalAttach(uploadVO));
    }

    /**
     * 获取采购方案的投标供应商信息
     */
    @ApiModelProperty(value = "获取采购方案的投标供应商信息")
    @GetMapping("/listBiddingVendor")
    public ResultData<List<BiddingVendorVO>> listBiddingVendor(@RequestParam Long schemeId) {
        return ResultData.data(biddingInfoService.listBiddingVendor(schemeId));
    }

    /**
     * 查询定标报告列表数据（定标报告）
     */
    @ApiModelProperty(value = "获取定标报告列表数据（定标报告）")
    @GetMapping("/getCalibrationReportList")
    @Deprecated
    public ResultData<List<CalibrationReportListVO>> getCalibrationReportList(@RequestParam Long noticeId) {
        return ResultData.data(biddingInfoService.getCalibrationReportList(noticeId));
    }

    /**
     * 报价汇总
     * @param queryVO
     * @return
     */
    @PostMapping("/getBiddingQuotationSummary")
    @ApiOperation("报价汇总（定标报告）")
    public ResultData<List<BiddingQuotationSummaryListVO>> getBiddingQuotationSummary(@RequestBody BiddingQuotationQueryVO queryVO) {
        List<BiddingQuotationSummaryListVO> list = biddingInfoService.getBiddingQuotationSummary(queryVO);
        return ResultData.data(list);
    }

    @ApiOperation(value = "评标汇总（定标报告）")
    @GetMapping("/getBidEvaluationList")
    public ResultData<List<BidEvaluationVo>> getBidEvaluationList(@ApiParam(value = "招标公告主键id", required = true) @RequestParam Long noticeId,
                                                                  @ApiParam(value = "评标类型(1:技术评分 2:商务评分)", required = true) @RequestParam int scoreType) {
        return ResultData.data(biddingInfoService.getBidEvaluationList(noticeId,scoreType));
    }

    /**
     * 未评标专家催办操作
     */
    @PostMapping("/urgeExpertMes")
    public ResultData<Boolean> urgeExpertMes(@RequestBody UrgeExpertMesVO urgeExpertMesVO) {
        return ResultData.status(biddingInfoService.urgeExpertMes(urgeExpertMesVO));
    }

}
