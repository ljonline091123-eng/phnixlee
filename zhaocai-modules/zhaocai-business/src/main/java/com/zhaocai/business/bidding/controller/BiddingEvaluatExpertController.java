package com.zhaocai.business.bidding.controller;

import com.zhaocai.business.bidding.service.IBiddingEvaluatExpertService;
import com.zhaocai.business.bidding.vo.req.BiddingEvaluatExpertVO;
import com.zhaocai.business.bidding.vo.req.query.BiddingEvaluatExpertQueryVO;
import com.zhaocai.business.bidding.vo.res.BiddingEvaluatExpertListVO;
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
 * 评标专家人员信息Controller
 *
 * @author WH
 * @date 2024-05-24
 */
@RestController
@RequestMapping("/evaluatExpert")
@Api(value = "评标专家人员信息", tags = "评标专家人员信息接口")
public class BiddingEvaluatExpertController extends BladeController {
    @Autowired
    private IBiddingEvaluatExpertService biddingEvaluatExpertService;

    /**
     * 新增评标专家人员信息
     */
    @Log(title = "新增评标专家人员信息", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ApiOperation("新增评标专家人员信息")
    public ResultData add(@RequestBody @Validated({ValidateGroup.AddGroup.class}) BiddingEvaluatExpertVO biddingEvaluatExpertVO) {
        return ResultData.status(biddingEvaluatExpertService.add(biddingEvaluatExpertVO));
    }

    /**
     * 查询评标专家人员信息列表
     */
    @PostMapping("/getList")
    public ResultData<List<BiddingEvaluatExpertListVO>> getList(@RequestBody BiddingEvaluatExpertQueryVO queryVO) {
        List<BiddingEvaluatExpertListVO> list = biddingEvaluatExpertService.getList(queryVO);
        return ResultData.data(list);
    }

}
