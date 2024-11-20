package com.zhaocai.business.pub.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.pub.domain.DwCdBank;
import com.zhaocai.business.pub.mapper.BankMapper;
import com.zhaocai.business.pub.service.IBankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 支行档案Service业务层处理
 *
 * @author cs
 * @date 2024-11-20
 */
@Service
public class BankServiceImpl extends ServiceImpl<BankMapper, DwCdBank> implements IBankService
        {
    @Autowired
    private BankMapper bankMapper;


            /**
             * 同步支行档案
             *
             * @return 支行档案
             */
            @Override
            public List<DwCdBank> getBankList() {
                return null;
            }

            /**
     * 查询支行档案
     *
     * @param id 支行档案主键
     * @return 支行档案
     */
    @Override
    public DwCdBank selectBankById(Long id)
    {
        return bankMapper.selectBankById(id);
    }

    /**
     * 查询支行档案列表
     *
     * @param dwCdBank 支行档案
     * @return 支行档案
     */
    @Override
    public List<DwCdBank> selectBankList(DwCdBank dwCdBank)
    {
        return bankMapper.selectBankList(dwCdBank);
    }

    /**
     * 新增支行档案
     *
     * @param dwCdBank 支行档案
     * @return 结果
     */
    @Override
    public int insertBank(DwCdBank dwCdBank)
    {
        return bankMapper.insertBank(dwCdBank);
    }

    /**
     * 修改支行档案
     *
     * @param dwCdBank 支行档案
     * @return 结果
     */
    @Override
    public int updateBank(DwCdBank dwCdBank)
    {
        return bankMapper.updateBank(dwCdBank);
    }

    /**
     * 批量删除支行档案
     *
     * @param ids 需要删除的支行档案主键
     * @return 结果
     */
    @Override
    public int deleteBankByIds(Long[] ids)
    {
        return bankMapper.deleteBankByIds(ids);
    }

    /**
     * 删除支行档案信息
     *
     * @param id 支行档案主键
     * @return 结果
     */
    @Override
    public int deleteBankById(Long id)
    {
        return bankMapper.deleteBankById(id);
    }

    @Override
    public Boolean deleteSyncBank() {
        return baseMapper.deleteSyncBank();
    }
}
