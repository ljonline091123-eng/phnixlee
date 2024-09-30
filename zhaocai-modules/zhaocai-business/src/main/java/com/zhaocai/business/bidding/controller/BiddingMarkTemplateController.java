package com.zhaocai.business.bidding.controller;

import com.zhaocai.business.bidding.service.IBiddingMarkTemplateService;
import com.zhaocai.business.bidding.vo.req.BiddingMarkTemplateVO;
import com.zhaocai.business.bidding.vo.req.query.BiddingMarkTemplateQueryVO;
import com.zhaocai.business.bidding.vo.res.BiddingMarkTemplateDetailVO;
import com.zhaocai.business.bidding.vo.res.BiddingMarkTemplateListVO;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.bean.ValidateGroup;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.log.annotation.Log;
import com.zhaocai.common.log.enums.BusinessType;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 评分模板Controller
 *
 * @author WH
 * @date 2024-06-18
 */
@RestController
@RequestMapping("/markTemplate")
@Api(value = "评分模板", tags = "评分模板接口")
public class BiddingMarkTemplateController extends BladeController {

    @Autowired
    private IBiddingMarkTemplateService biddingMarkTemplateService;

    /**
     * 详情
     */
    @GetMapping("/detail")
    @ApiOperation(value = "详情", notes = "传入id")
    public ResultData<BiddingMarkTemplateDetailVO> detail(@ApiParam(value = "主键", required = true) @RequestParam Long id) {
        BiddingMarkTemplateDetailVO vo = biddingMarkTemplateService.detail(id);
        return ResultData.data(vo);
    }

    /**
     * 范本分页查询评分模板列表
     */
    @PostMapping("/fanListPage")
    @ApiOperation("分页查询评分模板列表")
    public ResultData<PageResult<BiddingMarkTemplateListVO>> fanListPage(@RequestBody BiddingMarkTemplateQueryVO queryVO) {
        return ResultData.data(biddingMarkTemplateService.fanListPage(queryVO));
    }

    /**
     * 分页查询评分模板列表
     */
    @PostMapping("/page")
    @ApiOperation("分页查询评分模板列表")
    public ResultData<PageResult<BiddingMarkTemplateListVO>> page(@RequestBody BiddingMarkTemplateQueryVO queryVO) {
        return ResultData.data(biddingMarkTemplateService.page(queryVO));
    }

    /**
     * 分页查询评分模板列表
     */
    @PostMapping("/switchListPage")
    @ApiOperation("分页查询评分模板列表")
    public ResultData<PageResult<BiddingMarkTemplateListVO>> switchListPage(@RequestBody BiddingMarkTemplateQueryVO queryVO) {
        return ResultData.data(biddingMarkTemplateService.switchListPage(queryVO));
    }

    /**
     * 新增或修改评分模板
     */
    @Log(title = "新增或修改评分模板", businessType = BusinessType.INSERT)
    @ApiOperation("新增或修改评分模板")
    @PostMapping("/addOrUpdate")
    public ResultData addOrUpdate(@RequestBody @Validated({ValidateGroup.AddGroup.class}) BiddingMarkTemplateVO biddingMarkTemplateVO) {
        return ResultData.status(biddingMarkTemplateService.addOrUpdate(biddingMarkTemplateVO));
    }

    /**
     * 删除评分模板
     */
    @PostMapping("/delete")
    @ApiOperation(value = "删除评分模板", notes = "传入id")
    public ResultData delete(@ApiParam(value = "主键集合", required = true) @RequestParam Long id) {
        return ResultData.status(biddingMarkTemplateService.delete(id));
    }

    /**
     * 修改启用状态
     *
     * @param id
     * @param state
     * @return
     */
    @PostMapping("/updateStatus/{id}/{state}")
    @ApiOperation("修改启用状态")
    @ApiImplicitParams({@ApiImplicitParam(name = "id", value = "主键", required = true, paramType = "path", dataType = "Long"),
            @ApiImplicitParam(name = "state", value = "状态值", required = true, paramType = "path", dataType = "Integer")})
    public ResultData<Boolean> updateStatus(@PathVariable("id") Long id, @PathVariable("state") Integer state) {
        return ResultData.status(biddingMarkTemplateService.updateStatus(id, state));
    }


}
