package com.zhaocai.business.expert.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.expert.service.IExpertService;
import com.zhaocai.business.expert.vo.req.ExpertVO;
import com.zhaocai.business.expert.vo.req.query.ExpertQueryVO;
import com.zhaocai.business.expert.vo.res.ExpertInfoVO;
import com.zhaocai.business.expert.vo.res.ExpertListVO;
import com.zhaocai.business.expert.vo.res.TPIExpertInfoVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.bean.ValidateGroup;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.log.annotation.Log;
import com.zhaocai.common.log.enums.BusinessType;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 专家Controller
 *
 * @author WH
 * @date 2024-05-24
 */
@RestController
@RequestMapping("/expert")
@Api(value = "专家列表对象", tags = "专家列表对象")
public class ExpertController extends BladeController {
    @Autowired
    private IExpertService expertService;

    /**
     * 获取第三方专家信息
     */
    @GetMapping("/getTPIExpertInfo")
    public ResultData<List<TPIExpertInfoVO>> getTPIExpertInfo() {
        return ResultData.data(expertService.getTPIExpertInfo());
    }

    /**
     * 分页查询专家列表
     */
    @PostMapping("/page")
    @ApiOperation("分页-根据条件查询专家列表")
    public ResultData<PageResult<ExpertListVO>> page(@RequestBody ExpertQueryVO queryDTO) {
        return ResultData.data(expertService.page(queryDTO));
    }

    /**
     * 获取专家详细信息
     */
    @GetMapping("/getInfo")
    public ResultData<ExpertInfoVO> getInfo(@ApiParam(value = "主键", required = true) @RequestParam Long id) {
        return ResultData.data(expertService.getInfo(id));
    }

    /**
     * 新增提交专家
     */
    @Log(title = "新增提交专家", businessType = BusinessType.INSERT)
    @PostMapping("/submit")
    @ApiOperation("新增提交专家")
    public ResultData submit(@RequestBody @Validated({ValidateGroup.AddGroup.class}) ExpertVO expertVO) {
        return ResultData.status(expertService.submit(expertVO));
    }

    /**
     * 保存专家
     */
    @Log(title = "保存专家信息", businessType = BusinessType.INSERT)
    @PostMapping("/save")
    @ApiOperation("保存专家信息")
    public ResultData save(@RequestBody @Validated({ValidateGroup.AddGroup.class}) ExpertVO expertVO) {
        return ResultData.status(expertService.save(expertVO));
    }

    /**
     * 删除专家
     */
    @Log(title = "专家", businessType = BusinessType.DELETE)
	@PostMapping("/delete")
    public ResultData delete(@RequestBody List<Long> ids) {
        return ResultData.status(expertService.delete(ids));
    }

    /**
     * 修改启用状态（1启用|2禁用）
     *
     * @param id
     * @param state
     * @return
     */
    @PostMapping("/updateStatus/{id}/{state}")
    @ApiOperation("修改启用状态")
    @ApiImplicitParams({@ApiImplicitParam(name = "id", value = "主键", required = true, paramType = "path", dataType = "Long"),
            @ApiImplicitParam(name = "state", value = "状态值（1启用|2禁用）", required = true, paramType = "path", dataType = "Integer")})
    public ResultData<Boolean> updateStatus(@PathVariable("id") Long id, @PathVariable("state") Integer state) {
        return ResultData.status(expertService.updateStatus(id, state));
    }

}
