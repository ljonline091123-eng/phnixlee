package com.zhaocai.business.bidding.controller;

import com.zhaocai.business.bidding.service.ITenderNoticeService;
import com.zhaocai.business.bidding.vo.req.TenderNoticeVO;
import com.zhaocai.business.bidding.vo.req.query.TenderNoticeQueryVO;
import com.zhaocai.business.bidding.vo.res.TenderNoticeDetailVO;
import com.zhaocai.business.bidding.vo.res.TenderNoticeListVO;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.bean.ValidateGroup;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.log.annotation.Log;
import com.zhaocai.common.log.enums.BusinessType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 招标公告Controller
 *
 * @author WH
 * @date 2024-05-24
 */
@RestController
@RequestMapping("/notice")
@Api(value = "招标公告对象", tags = "招标公告对象接口")
public class TenderNoticeController extends BladeController {
    @Autowired
    private ITenderNoticeService tenderNoticeService;

    /**
     * 分页查询招标公告列表
     */
    @PostMapping("/page")
    @ApiOperation("分页-根据条件查询班次信息列表")
    public ResultData<PageResult<TenderNoticeListVO>> page(@RequestBody TenderNoticeQueryVO queryDTO) {
        return ResultData.data(tenderNoticeService.page(queryDTO));
    }

    /**
     * 发布招标公告
     */
    @Log(title = "发布招标公告", businessType = BusinessType.INSERT)
    @ApiOperation("发布招标公告")
    @PostMapping("/add")
    public ResultData add(@RequestBody @Validated({ValidateGroup.AddGroup.class}) TenderNoticeVO tenderNoticeVO) {
        return ResultData.status(tenderNoticeService.add(tenderNoticeVO));
    }

    /**
     * 重新发布招标公告（重新招标）
     */
    @Log(title = "重新发布招标公告（重新招标）", businessType = BusinessType.INSERT)
    @ApiOperation("重新发布招标公告（重新招标）")
    @PostMapping("/aNewAdd")
    public ResultData aNewAdd(@RequestBody @Validated({ValidateGroup.AddGroup.class}) TenderNoticeVO tenderNoticeVO) {
        return ResultData.status(tenderNoticeService.aNewAdd(tenderNoticeVO));
    }

    /**
     * 查询已发布的招标公告并按照投标截止时间更改状态
     * 定时任务job
     */
    @ApiOperation("查询已发布的招标公告并按照投标截止时间更改状态")
    @GetMapping("/handleTenderNoticeIssueStatus")
    public ResultData<Boolean> handleTenderNoticeIssueStatus(){
        return ResultData.status(tenderNoticeService.handleTenderNoticeIssueStatus());
    }

    /**
     * 查询已公示的招标公告并按照公示期截止时间更改状态
     * 定时任务job
     */
    @ApiOperation("查询已公示的招标公告并按照公示期截止时间更改状态")
    @GetMapping("/handleTenderNoticePublicityStatus")
    public ResultData<Boolean> handleTenderNoticePublicityStatus(){
        return ResultData.status(tenderNoticeService.handleTenderNoticePublicityStatus());
    }

    /**
     * 获取投标单详情信息
     */
    @GetMapping(value = "/getInfo")
    public ResultData<TenderNoticeDetailVO> getInfo(@ApiParam(value = "采购方案id", required = true) @RequestParam Long schemeId,
                                                    @ApiParam(value = "招标公告id") @RequestParam(required = false) Long noticeId)
    {
        return ResultData.data(tenderNoticeService.getInfo(schemeId, noticeId));
    }


}
