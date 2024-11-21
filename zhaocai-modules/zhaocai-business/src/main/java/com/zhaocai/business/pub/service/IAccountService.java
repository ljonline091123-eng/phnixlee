package com.zhaocai.business.pub.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.pub.domain.TAccountInfo;

import java.util.List;

/**
 * 银行账户Service接口
 *
 * @author cs
 * @date 2024-11-20
 */
public interface IAccountService extends IService<TAccountInfo>
{
    /**
     * 查询银行
     * @param id 主键
     * @return 银行信息
     */
    public TAccountInfo selectAccountById(Long id);

    /**
     * 查询列表
     *
     * @param tAccountInfo 银行账户信息
     * @return 银行账户列表
     */
    public List<TAccountInfo> selectAccountList(TAccountInfo tAccountInfo);

    /**
     * 新增银行账户信息
     *
     * @param tAccountInfo 银行账户信息
     * @return 结果
     */
    public int insertAccount(TAccountInfo tAccountInfo);

    /**
     * 修改银行账户信息
     *
     * @param tAccountInfo 银行账户信息
     * @return 结果
     */
    public int updateAccount(TAccountInfo tAccountInfo);

    /**
     * 批量删除
     *
     * @param ids 删除银行账户信息主键集合
     * @return 结果
     */
    public int deleteAccountByIds(Long[] ids);

    /**
     * 删除银行账户信息信息
     *
     * @param id 银行账户信息主键
     * @return 结果
     */
    public int deleteAccountById(Long id);

    Boolean deleteSyncAccount();
}
