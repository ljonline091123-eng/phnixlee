package com.zhaocai.business.vendor.controller;

import com.zhaocai.business.pub.domain.TAccountInfo;
import com.zhaocai.business.pub.service.IAccountService;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.core.web.controller.BaseController;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 银行账户信息
 *
 * @author cs
 * @date 2024-10-20
 */
@RestController
@RequestMapping("/account")
public class AccountController extends BaseController
{
    @Autowired
    private IAccountService accountService;

    /**
     * 查询银行账户信息
     */
    @GetMapping("/list")
    public ResultData<List<TAccountInfo>> list(TAccountInfo accountInfo)
    {
        List<TAccountInfo> list = accountService.selectAccountList(accountInfo);
        return ResultData.data(list);
    }
    /**
     * 银行详情
     */
    @GetMapping("/detail")
    @ApiOperation("账户详情")
    public ResultData<TAccountInfo> detail(Long id) {
        TAccountInfo accountInfo = accountService.selectAccountById(id);
        return ResultData.data(accountInfo);
    }
    /**
     * 保存银行详情
     */
    @PostMapping("/save")
    @ApiOperation(value = "保存账户详情")
    public ResultData<Boolean> save(@RequestBody TAccountInfo accountInfo) {
        Integer type =1;// 修改
        if(accountInfo.getId()==null){
            type =2; //新增
        }
        accountService.saveOrUpdate(accountInfo);
        accountService.push(accountInfo,type);
        return ResultData.success();
    }
    /**
     * 删除
     */
    @PostMapping("/remove")
    @ApiOperation(value = "作废账户")
    public ResultData<Boolean> remove(@RequestParam Long id) {
        accountService.deleteAccountById(id);
        return ResultData.success();
    }
}
