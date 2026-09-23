package com.zhaocai.business.bidding.controller;

import com.zhaocai.business.bidding.service.IBiddingOpenPeopleService;
import com.zhaocai.business.bidding.vo.req.BiddingOpenPeopleVO;
import com.zhaocai.business.bidding.vo.req.OpenBidVO;
import com.zhaocai.business.bidding.vo.req.query.BiddingOpenPeopleQueryVO;
import com.zhaocai.business.bidding.vo.res.BiddingOpenPeopleListVO;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.common.core.bean.ValidateGroup;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.log.annotation.Log;
import com.zhaocai.common.log.enums.BusinessType;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 开标人员信息Controller
 *
 * @author WH
 * @date 2024-05-24
 */
@RestController
@RequestMapping("/people")
@Api(value = "开标人员信息对象", tags = "开标人员信息对象接口")
public class BiddingOpenPeopleController extends BladeController {
    @Autowired
    private IBiddingOpenPeopleService biddingOpenPeopleService;

    /**
     * 新增开标人员信息
     */
    @Log(title = "新增开标人员信息", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public ResultData add(@RequestBody @Validated({ValidateGroup.AddGroup.class}) List<BiddingOpenPeopleVO> biddingOpenPeopleVos) {
        return ResultData.status(biddingOpenPeopleService.add(biddingOpenPeopleVos));
    }

    /**
     * 提交
     */
    @PostMapping("/submit")
    public ResultData submit(@RequestParam Long noticeId) {
        return ResultData.status(biddingOpenPeopleService.submit(noticeId));
    }

    /**
     * 查询开标人员信息列表
     */
    @PostMapping("/getList")
    public ResultData<List<BiddingOpenPeopleListVO>> getList(@RequestBody BiddingOpenPeopleQueryVO queryVO) {
        return ResultData.data(biddingOpenPeopleService.getList(queryVO));
    }

    /**
     * 查询开标人员待办信息列表
     */
    @PostMapping("/getTodoBiddingOpenList")
    public ResultData<List<BiddingOpenPeopleListVO>> getTodoBiddingOpenList(@RequestBody BiddingOpenPeopleQueryVO queryVO) {
        return ResultData.data(biddingOpenPeopleService.getTodoBiddingOpenList(queryVO));
    }

    /**
     * 开标人员开标
     */
    @PostMapping("/openBid")
    public ResultData<Boolean> openBid(@RequestBody OpenBidVO openBidVO) {
        return ResultData.status(biddingOpenPeopleService.openBid(openBidVO));
    }

}
