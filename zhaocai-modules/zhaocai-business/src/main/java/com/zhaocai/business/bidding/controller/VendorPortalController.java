package com.zhaocai.business.bidding.controller;

import com.zhaocai.business.bidding.service.IVendorPortalService;
import com.zhaocai.business.bidding.vo.req.query.VendorPortalDataStatQueryVO;
import com.zhaocai.business.bidding.vo.req.query.VendorPortalNoticePageQueryVO;
import com.zhaocai.business.bidding.vo.req.query.VendorPortalPublicityPageQueryVO;
import com.zhaocai.business.bidding.vo.res.VendorPortalDataStatVO;
import com.zhaocai.business.bidding.vo.res.VendorPortalNoticeListVO;
import com.zhaocai.business.bidding.vo.res.VendorPortalPublicityListVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ssy
 * @date 2024/6/25 9:49
 */
@RestController
@AllArgsConstructor
@RequestMapping("vendorPortal")
@Api(value = "门户端", tags = "门户端接口")
public class VendorPortalController {

    @Autowired
    private IVendorPortalService vendorPortalService;

    /**
     * 招标公告列表分页
     */
    @GetMapping("/notice")
    @ApiOperation(value = "招标公告列表分页", notes = "传入queryDTO")
    public ResultData<PageResult<VendorPortalNoticeListVO>> getNotice(VendorPortalNoticePageQueryVO queryDTO) {
        PageResult<VendorPortalNoticeListVO> pages = vendorPortalService.getNotice(queryDTO);
        return ResultData.data(pages);
    }

    /**
     * 招标公告列表分页（已登录）
     */
    @GetMapping("/noticeLogin")
    @ApiOperation(value = "招标公告列表分页（已登录）", notes = "传入queryDTO")
    public ResultData<PageResult<VendorPortalNoticeListVO>> getNoticeLogin(VendorPortalNoticePageQueryVO queryDTO) {
        PageResult<VendorPortalNoticeListVO> pages = vendorPortalService.getNoticeLogin(queryDTO);
        return ResultData.data(pages);
    }

    /**
     * 工作台首页 消息列表（已登录）
     */
    @GetMapping("/msgList")
    @ApiOperation(value = "工作台首页消息列表（已登录）", notes = "传入queryDTO")
    public ResultData<PageResult<VendorPortalNoticeListVO>> msgList(VendorPortalNoticePageQueryVO queryDTO) {
        PageResult<VendorPortalNoticeListVO> pages = vendorPortalService.msgList(queryDTO);
        return ResultData.data(pages);
    }

    /**
     * 中标公示列表分页
     */
    @GetMapping("/publicity")
    @ApiOperation(value = "中标公示列表分页", notes = "传入queryDTO")
    public ResultData<PageResult<VendorPortalPublicityListVO>> getPublicity(VendorPortalPublicityPageQueryVO queryDTO) {
        PageResult<VendorPortalPublicityListVO> pages = vendorPortalService.getPublicity(queryDTO);
        return ResultData.data(pages);
    }

    /**
     * 门户端数据统计接口
     */
    @GetMapping("/dataStat")
    @ApiOperation(value = "门户端数据统计接口", notes = "传入queryDTO")
    public ResultData<VendorPortalDataStatVO> dataStat(VendorPortalDataStatQueryVO queryDTO) {
        return ResultData.data(vendorPortalService.dataStat(queryDTO));
    }

}
