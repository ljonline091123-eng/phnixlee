package com.zhaocai.business.pub.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.pub.domain.DwCdBank;
import com.zhaocai.common.core.bean.PageResult;

import java.util.List;

/**
 * 支行Service接口
 *
 * @author cs
 * @date 2024-11-20
 */
public interface IBankService extends IService<DwCdBank>
{
    /**
     * 查询银行
     *
     * @param id 主键
     * @return 银行信息
     */
    public DwCdBank selectBankById(Long id);

    /**
     * 查询列表
     *
     * @param dwCdBank 支行信息
     * @return 支行列表
     */
    public PageResult<DwCdBank> selectBankList(DwCdBank dwCdBank);

    /**
     * 新增支行信息
     *
     * @param dwCdBank 支行信息
     * @return 结果
     */
    public int insertBank(DwCdBank dwCdBank);

    /**
     * 修改支行信息
     *
     * @param dwCdBank 支行信息
     * @return 结果
     */
    public int updateBank(DwCdBank dwCdBank);

    /**
     * 批量删除
     *
     * @param ids 删除支行信息主键集合
     * @return 结果
     */
    public int deleteBankByIds(Long[] ids);

    /**
     * 删除支行信息信息
     *
     * @param id 支行信息主键
     * @return 结果
     */
    public int deleteBankById(Long id);

    Boolean deleteSyncBank();

    List<DwCdBank> getBankList();
}
