package com.zhaocai.business.bidding.controller;

import com.zhaocai.business.bidding.service.ITenderNoticeChangeRecordService;
import com.zhaocai.business.bidding.vo.req.TenderNoticeChangeRecordVO;
import com.zhaocai.business.bidding.vo.req.query.TenderNoticeChangeRecordQueryVO;
import com.zhaocai.business.bidding.vo.res.TenderNoticeChangeRecordListVO;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.common.core.bean.ValidateGroup;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.log.annotation.Log;
import com.zhaocai.common.log.enums.BusinessType;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 招标公告变更记录Controller
 *
 * @author WH
 * @date 2024-05-24
 */
@RestController
@RequestMapping("/noticeChangeRecord")
public class TenderNoticeChangeRecordController extends BladeController {
    @Autowired
    private ITenderNoticeChangeRecordService tenderNoticeChangeRecordService;

    @Log(title = "新增招标公告变更记录", businessType = BusinessType.INSERT)
    @PostMapping("/addNotice")
    @ApiOperation("新增招标公告变更记录")
    public ResultData addNotice(@RequestBody @Validated({ValidateGroup.AddGroup.class}) TenderNoticeChangeRecordVO noticeChangeRecordVO) {
        return ResultData.status(tenderNoticeChangeRecordService.addNotice(noticeChangeRecordVO));
    }

    @Log(title = "新增招标公告变更记录", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ApiOperation("新增招标公告变更记录")
    public ResultData add(@RequestBody @Validated({ValidateGroup.AddGroup.class}) TenderNoticeChangeRecordVO noticeChangeRecordVO) {
        return ResultData.status(tenderNoticeChangeRecordService.add(noticeChangeRecordVO));
    }

    /**
     * 查询招标公告变更记录
     */
    @PostMapping("/getListNotice")
    public ResultData<List<TenderNoticeChangeRecordListVO>> getListNotice(@RequestBody TenderNoticeChangeRecordQueryVO queryVO) {
        List<TenderNoticeChangeRecordListVO> list = tenderNoticeChangeRecordService.getListNotice(queryVO);
        return ResultData.data(list);
    }

    /**
     * 查询招标公告变更记录
     */
    @PostMapping("/getList")
    public ResultData<List<TenderNoticeChangeRecordListVO>> getList(@RequestBody TenderNoticeChangeRecordQueryVO queryVO) {
        List<TenderNoticeChangeRecordListVO> list = tenderNoticeChangeRecordService.getList(queryVO);
        return ResultData.data(list);
    }

}
