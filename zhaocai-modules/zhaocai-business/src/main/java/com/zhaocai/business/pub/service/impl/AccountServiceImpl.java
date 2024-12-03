package com.zhaocai.business.pub.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.pub.domain.TAccountInfo;
import com.zhaocai.business.pub.mapper.AccountMapper;
import com.zhaocai.business.pub.service.IAccountService;
import com.zhaocai.business.pub.vo.req.TAccountInfoVo;
import com.zhaocai.common.core.bean.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 银行账户Service业务层处理
 *
 * @author cs
 * @date 2024-11-20
 */
@Service
public class AccountServiceImpl extends ServiceImpl<AccountMapper, TAccountInfo> implements IAccountService
        {
    @Autowired
    private AccountMapper accountMapper;




            /**
     * 查询银行账户档案
     *
     * @param id 银行账户档案主键
     * @return 银行账户档案
     */
    @Override
    public TAccountInfo selectAccountById(Long id)
    {
        return accountMapper.selectAccountById(id);
    }

    /**
     * 查询银行账户档案列表
     *
     * @param tAccountInfo 银行账户档案
     * @return 银行账户档案
     */
    @Override
    public PageResult<TAccountInfoVo> selectAccountList(TAccountInfoVo tAccountInfo)
    {

        IPage<TAccountInfoVo> list = accountMapper.selectAccountList(tAccountInfo.toMybatisPage(),tAccountInfo);
        return new  PageResult<>(list);
    }

    /**
     * 新增银行账户档案
     *
     * @param tAccountInfo 银行账户档案
     * @return 结果
     */
    @Override
    public int insertAccount(TAccountInfo tAccountInfo)
    {
        return accountMapper.insertAccount(tAccountInfo);
    }

    /**
     * 修改银行账户档案
     *
     * @param tAccountInfo 银行账户档案
     * @return 结果
     */
    @Override
    public int updateAccount(TAccountInfo tAccountInfo)
    {
        return accountMapper.updateAccount(tAccountInfo);
    }

    /**
     * 批量删除银行账户档案
     *
     * @param ids 需要删除的银行账户档案主键
     * @return 结果
     */
    @Override
    public int deleteAccountByIds(Long[] ids)
    {
        return accountMapper.deleteAccountByIds(ids);
    }


            /**
             * 推送银行账户档案信息
             *
             * @param accountInfo 银行账户档案
             * @return 结果
             */
            @Override
            public void push(TAccountInfo accountInfo,Integer type) {

            }

            /**
     * 删除银行账户档案信息
     *
     * @param id 银行账户档案主键
     * @return 结果
     */
    @Override
    public int deleteAccountById(Long id)
    {
        return accountMapper.deleteAccountById(id);
    }

    @Override
    public Boolean deleteSyncAccount() {
        return baseMapper.deleteSyncAccount();
    }

        }
