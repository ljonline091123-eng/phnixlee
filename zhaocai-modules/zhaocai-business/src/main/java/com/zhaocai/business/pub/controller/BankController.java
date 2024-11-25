package com.zhaocai.business.pub.controller;

import com.zhaocai.business.pub.domain.DwCdBank;
import com.zhaocai.business.pub.service.IBankService;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.core.web.controller.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 支行信息
 *
 * @author cs
 * @date 2024-10-21
 */
@RestController
@RequestMapping("/bank")
public class BankController extends BaseController
{
    @Autowired
    private IBankService bankService;

    /**
     * 查询支行信息
     */
    @GetMapping("/list")
    public ResultData<PageResult<DwCdBank>> list(@Valid DwCdBank dwCdBank)
    {
        PageResult<DwCdBank> list = bankService.selectBankList(dwCdBank);
        return ResultData.data(list);
    }
}
