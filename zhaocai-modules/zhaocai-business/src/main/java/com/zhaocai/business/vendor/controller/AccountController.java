package com.zhaocai.business.vendor.controller;

import cn.hutool.core.util.IdUtil;
import com.zhaocai.business.pub.domain.TAccountInfo;
import com.zhaocai.business.pub.service.IAccountService;
import com.zhaocai.business.pub.vo.req.TAccountInfoVo;
import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.core.web.controller.BaseController;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
    public ResultData<PageResult<TAccountInfoVo>> list(TAccountInfoVo accountInfo)
    {
        PageResult<TAccountInfoVo> list = accountService.selectAccountList(accountInfo);
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
    public ResultData<Boolean> save(@RequestBody TAccountInfoVo accountInfo) {
        String type = Vendor.LOG_TYPE_MODIFY;// 修改
        if(accountInfo.getId()==null){
            type = Vendor.LOG_TYPE_ADD; //新增
        }
        TAccountInfo info = new TAccountInfo();
        info=  BeanCopierUtil.copyBean(accountInfo, TAccountInfo.class);
        if(info!=null&&info.getId()==null) {
           info.setId(IdUtil.getSnowflakeNextId());
        }
            accountService.saveOrUpdate(info);

         accountService.push(info,type);
        return ResultData.success();
    }
    /**
     * 删除
     */
    @PostMapping("/remove")
    @ApiOperation(value = "作废账户")
    public ResultData<Boolean> remove(@RequestParam Long id) {
        TAccountInfo bean = accountService.selectAccountById(id);
        accountService.deleteAccountById(id);
        if(bean != null){
            accountService.push(bean,Vendor.LOG_TYPE_REMOVE);
        }
        return ResultData.success();
    }
}
