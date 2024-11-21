package com.zhaocai.business.pub.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.pub.domain.DwCdBank;

import java.util.List;

/**
 * 支行档案Mapper接口
 *
 * @author cs
 * @date 2024-11-20
 */
public interface BankMapper extends BaseMapper<DwCdBank>
{
    /**
     * 查询支行档案
     *
     * @param id 支行档案主键
     * @return 支行档案
     */
    public DwCdBank selectBankById(Long id);

    /**
     * 查询支行档案列表
     *
     * @param dwCdBank 支行档案
     * @return 支行档案集合
     */
    public List<DwCdBank> selectBankList(DwCdBank dwCdBank);

    /**
     * 新增支行档案
     *
     * @param dwCdBank 支行档案
     * @return 结果
     */
    public int insertBank(DwCdBank dwCdBank);

    /**
     * 修改支行档案
     *
     * @param dwCdBank 支行档案
     * @return 结果
     */
    public int updateBank(DwCdBank dwCdBank);

    /**
     * 删除支行档案

     * @param id 支行档案主键
     * @return 结果
     */
    public int deleteBankById(Long id);

    /**
     * 批量删除支行档案
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteBankByIds(Long[] ids);

    @InterceptorIgnore(blockAttack = "true")
    Boolean deleteSyncBank();
}
