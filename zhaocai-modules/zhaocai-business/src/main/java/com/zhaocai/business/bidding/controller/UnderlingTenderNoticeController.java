package com.zhaocai.business.bidding.controller;

import com.zhaocai.business.bidding.service.ITenderNoticeService;
import com.zhaocai.business.bidding.vo.req.TenderNoticeVO;
import com.zhaocai.business.bidding.vo.req.UnderlingTenderNoticeQueryVO;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.common.core.bean.PageResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(value = "第三方-招标公告接口")
@RestController
@RequestMapping("/underling/notice")
public class UnderlingTenderNoticeController extends BladeController {

    @Autowired
    private ITenderNoticeService noticeService;

    /**
     * 获取招标公告列表
     * @param queryVO
     * @return
     */
    @GetMapping("/listPage")
    @ApiOperation(value = "获取招标公告列表")
    public PageResult<TenderNoticeVO> listPage(UnderlingTenderNoticeQueryVO queryVO) {
        return noticeService.listTenderNoticePage(queryVO);
    }
}
