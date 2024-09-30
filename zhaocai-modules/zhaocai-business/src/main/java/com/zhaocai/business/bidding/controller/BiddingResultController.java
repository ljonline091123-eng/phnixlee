package com.zhaocai.business.bidding.controller;

import com.zhaocai.business.bidding.service.IBiddingResultService;
import com.zhaocai.business.bidding.vo.req.CalibrationEntranceVO;
import com.zhaocai.business.bidding.vo.req.CalibrationReleaseVO;
import com.zhaocai.business.bidding.vo.req.CalibrationVO;
import com.zhaocai.business.bidding.vo.req.ResultReleasVO;
import com.zhaocai.business.bidding.vo.res.BidResultVO;
import com.zhaocai.business.bidding.vo.res.BiddingResultDetailVO;
import com.zhaocai.business.bidding.vo.res.BiddingResultListVO;
import com.zhaocai.business.bidding.vo.res.WinningBidResultVO;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 投标结果信息Controller
 *
 * @author WH
 * @date 2024-05-24
 */
@RestController
@RequestMapping("/result")
@Api(value = "投标结果信息", tags = "投标结果信息接口")
public class BiddingResultController extends BladeController {

    @Autowired
    private IBiddingResultService biddingResultService;

    /**
     * 详情
     */
    @GetMapping("/detail")
    @ApiOperation(value = "详情", notes = "传入id")
    public ResultData<BiddingResultDetailVO> detail(@ApiParam(value = "主键", required = true) @RequestParam Long id) {
        return ResultData.data(biddingResultService.detail(id));
    }

    /**
     * 定标
     */
    @PostMapping("/calibration")
    @ApiOperation(value = "定标", notes = "传入tender")
    public ResultData calibration(@Valid @RequestBody CalibrationEntranceVO entranceVO) {
        return ResultData.status(biddingResultService.calibration(entranceVO));
    }

    /**
     * 查询定标供应商数据列表（中标公示）
     */
    @GetMapping("/getBiddingResult")
    @ApiOperation(value = "查询定标供应商数据列表（中标公示）", notes = "传入tender")
    public ResultData<List<BiddingResultListVO>> getBiddingResult(@RequestParam Long noticeId) {
        return ResultData.data(biddingResultService.getBiddingResult(noticeId));
    }

    /**
     * 定标公示发布
     */
    @PostMapping("/calibrationRelease")
    @ApiOperation(value = "定标公示发布", notes = "传入noticeId")
    public ResultData calibrationRelease(@Valid @RequestBody CalibrationReleaseVO calibrationReleaseVO) {
        return ResultData.status(biddingResultService.calibrationRelease(calibrationReleaseVO));
    }

    /**
     * 查询中标结果数据列表（结果发布）
     */
    @GetMapping("/getWinningBidResult")
    @ApiOperation(value = "查询中标结果数据列表（结果发布）", notes = "传入tender")
    public ResultData<List<WinningBidResultVO>> getWinningBidResult(@RequestParam Long noticeId) {
        return ResultData.data(biddingResultService.getWinningBidResult(noticeId));
    }


    /**
     * 结果发布
     */
    @PostMapping("/winningBidResultRelease")
    @ApiOperation(value = "结果发布", notes = "传入noticeId")
    public ResultData winningBidResultRelease(@RequestBody ResultReleasVO resultReleasVO) {
        return ResultData.status(biddingResultService.winningBidResultRelease(resultReleasVO));
    }

    /**
     * 查询中标结果数据列表（招标结果）
     */
    @GetMapping("/getBidResult")
    @ApiOperation(value = "查询中标结果数据列表（招标结果）", notes = "传入tender")
    public ResultData<List<BidResultVO>> getBidResult(@RequestParam Long noticeId) {
        return ResultData.data(biddingResultService.getBidResult(noticeId));
    }

}
