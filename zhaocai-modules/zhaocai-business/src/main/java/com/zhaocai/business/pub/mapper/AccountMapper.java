package com.zhaocai.business.pub.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.pub.domain.TAccountInfo;

import java.util.List;

/**
 * 银行账户档案Mapper接口
 *
 * @author cs
 * @date 2024-11-20
 */
public interface AccountMapper extends BaseMapper<TAccountInfo>
{
    /**
     * 查询银行账户档案
     *
     * @param id 银行账户档案主键
     * @return 银行账户档案
     */
    public TAccountInfo selectAccountById(Long id);

    /**
     * 查询银行账户档案列表
     *
     * @param tAccountInfo 银行账户档案
     * @return 银行账户档案集合
     */
    public List<TAccountInfo> selectAccountList(TAccountInfo tAccountInfo);

    /**
     * 新增银行账户档案
     *
     * @param tAccountInfo 银行账户档案
     * @return 结果
     */
    public int insertAccount(TAccountInfo tAccountInfo);

    /**
     * 修改银行账户档案
     *
     * @param tAccountInfo 银行账户档案
     * @return 结果
     */
    public int updateAccount(TAccountInfo tAccountInfo);

    /**
     * 删除银行账户档案

     * @param id 银行账户档案主键
     * @return 结果
     */
    public int deleteAccountById(Long id);

    /**
     * 批量删除银行账户档案
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAccountByIds(Long[] ids);

    @InterceptorIgnore(blockAttack = "true")
    Boolean deleteSyncAccount();
}
