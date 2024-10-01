package com.zhaocai.business.bidding.controller;

import com.zhaocai.business.bidding.service.ITenderNoticeAnswerService;
import com.zhaocai.business.bidding.vo.req.TenderNoticeAnswerVO;
import com.zhaocai.business.bidding.vo.req.query.TenderNoticeAnswerQueryVO;
import com.zhaocai.business.bidding.vo.res.TenderNoticeAnswerListVO;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.common.core.bean.ValidateGroup;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.log.annotation.Log;
import com.zhaocai.common.log.enums.BusinessType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 招标公告答疑Controller
 *
 * @author WH
 * @date 2024-05-24
 */
@RestController
@RequestMapping("/answer")
@Api(value = "招标公告答疑对象", tags = "招标公告答疑对象接口")
public class TenderNoticeAnswerController extends BladeController {
    @Autowired
    private ITenderNoticeAnswerService tenderNoticeAnswerService;

    /**
     * 查询招标公告答疑列表
     */
    @PostMapping("/getList")
    public ResultData<List<TenderNoticeAnswerListVO>> getList(@RequestBody TenderNoticeAnswerQueryVO queryVO) {
        List<TenderNoticeAnswerListVO> list = tenderNoticeAnswerService.getList(queryVO);
        return ResultData.data(list);
    }

    /**
     * 新增招标公告答疑
     */
    @Log(title = "新增招标公告答疑", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ApiOperation("新增招标公告答疑")
    public ResultData add(@RequestBody @Validated({ValidateGroup.AddGroup.class}) TenderNoticeAnswerVO tenderNoticeAnswerVO) {
        return ResultData.status(tenderNoticeAnswerService.add(tenderNoticeAnswerVO));
    }

    /**
     * 修改招标公告答疑
     */
    @Log(title = "修改招标公告答疑", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ApiOperation("修改招标公告答疑")
    public ResultData edit(@RequestBody @Validated({ValidateGroup.UpdateGroup.class}) TenderNoticeAnswerVO tenderNoticeAnswerVO)
    {
        return ResultData.status(tenderNoticeAnswerService.edit(tenderNoticeAnswerVO));
    }

}
