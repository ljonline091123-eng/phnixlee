package com.zhaocai.business.bidding.controller;

import com.zhaocai.business.bidding.service.IVendorBidService;
import com.zhaocai.business.bidding.vo.req.BidVO;
import com.zhaocai.business.bidding.vo.req.query.VendorNoticePageQueryVO;
import com.zhaocai.business.bidding.vo.req.query.WinningNotifiPageQueryVO;
import com.zhaocai.business.bidding.vo.res.TenderNoticeDetailVO;
import com.zhaocai.business.bidding.vo.res.VendorNoticeListVO;
import com.zhaocai.business.bidding.vo.res.WinningNotifiListVO;
import com.zhaocai.business.common.annotations.VendorStateCheck;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

/**
 * @author ssy
 * @date 2024/5/27 18:00
 */
@RestController
@AllArgsConstructor
@RequestMapping("vendorBid")
@Api(value = "供应商投标", tags = "供应商投标接口")
public class VendorBidController {

    @Autowired
    private IVendorBidService vendorBidService;

    /**
     * 在线投标报名列表-分页
     */
    @GetMapping("/pageNotice")
    @ApiOperation(value = "在线投标列表-分页（投标展示招标公告列表）", notes = "传入queryDTO")
    public ResultData<PageResult<VendorNoticeListVO>> pageNotice(VendorNoticePageQueryVO queryDTO) {
        PageResult<VendorNoticeListVO> pages = vendorBidService.pageNotice(queryDTO);
        return ResultData.data(pages);
    }


    /**
     * 在线投标报名报名情况
     */
    @GetMapping("/numNotice")
    @ApiOperation(value = "在线投标报名报名情况", notes = "传入queryDTO")
    public ResultData<Map<String,Integer>> numNotice(VendorNoticePageQueryVO queryDTO) {
       Map<String,Integer> map= vendorBidService.numNotice(queryDTO);
        return ResultData.data(map);
    }

    /**
     * 在线投标文件列表-分页（投标展示招标公告列表）
     */
    @GetMapping("/page")
    @ApiOperation(value = "在线投标列表-分页（投标展示招标公告列表）", notes = "传入queryDTO")
    public ResultData<PageResult<VendorNoticeListVO>> page(VendorNoticePageQueryVO queryDTO) {
        PageResult<VendorNoticeListVO> pages = vendorBidService.page(queryDTO);
        return ResultData.data(pages);
    }

    /**
     * 投标数据详情
     */
    @GetMapping("/detail")
    @ApiOperation(value = "投标数据详情", notes = "传入noticeId")
    public ResultData<TenderNoticeDetailVO> detail(@ApiParam(value = "招标公告主键id", required = true) @RequestParam("noticeId") Long noticeId) {
        return ResultData.data(vendorBidService.detail(noticeId));
    }

    /**
     * 供应商投标 招标报名 清单附件上传
     */
    @VendorStateCheck
    @PostMapping("/bidNotice")
    @ApiOperation(value = "报名", notes = "传入bidVO")
//    @TenderNoticeHandler
    public ResultData bidNotice(@Valid @RequestBody BidVO bidVO) {
        return ResultData.status(vendorBidService.bidNotice(bidVO));
    }

    /**
     * 供应商投标 清单附件上传
     */
    @VendorStateCheck
    @PostMapping("/bid")
    @ApiOperation(value = "投标", notes = "传入bidVO")
//    @TenderNoticeHandler
    public ResultData bid(@Valid @RequestBody BidVO bidVO) {
        return ResultData.status(vendorBidService.bid(bidVO));
    }

    /**
     * 撤回投标
     */
    @PostMapping("/withdrawBid")
    @ApiOperation(value = "撤回投标", notes = "传入bidVO")
    public ResultData withdrawBid(@RequestParam Long biddingInfoId) {
        return ResultData.status(vendorBidService.withdrawBid(biddingInfoId));
    }


    /**
     * 二次报价列表-分页（投标展示招标公告列表）
     */
//    @GetMapping("/twiceBidPage")
//    @ApiOperation(value = "二次报价列表-分页（投标展示招标公告列表）", notes = "传入queryDTO")
//    public ResultData<PageResult<TwiceBidListVO>> twiceBidPage(TwiceBidPageQueryVO queryDTO) {
//        PageResult<TwiceBidListVO> pages = vendorBidService.twiceBidPage(queryDTO);
//        return ResultData.data(pages);
//    }

    /**
     * 二次报价 (弃用)
     */
    @PostMapping("/twiceBid")
    @ApiOperation(value = "二次报价", notes = "传入bidVO")
    public ResultData twiceBid(@Valid @RequestBody BidVO bidVO) {
        return ResultData.status(vendorBidService.twiceBid(bidVO));
    }

    /**
     * 中标通知分页列表
     */
    @GetMapping("/winningNotifiPage")
    @ApiOperation(value = "中标通知分页列表", notes = "传入queryDTO")
    public ResultData<PageResult<WinningNotifiListVO>> winningNotifiPage(WinningNotifiPageQueryVO queryDTO) {
        PageResult<WinningNotifiListVO> pages = vendorBidService.winningNotifiPage(queryDTO);
        return ResultData.data(pages);
    }

}
