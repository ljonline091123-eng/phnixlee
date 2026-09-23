package com.zhaocai.business.receipt.service.impl;

import java.util.List;

import com.zhaocai.business.receipt.vo.req.ReceiptContractSettleDepositVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zhaocai.business.receipt.mapper.ReceiptContractSettleDepositMapper;
import com.zhaocai.business.receipt.domain.ReceiptContractSettleDeposit;
import com.zhaocai.business.receipt.service.IReceiptContractSettleDepositService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * 推送结算单 Service业务层处理
 *
 * @author cff
 * @date 2024-09-07
 */
@Service
public class ReceiptContractSettleDepositServiceImpl extends ServiceImpl<ReceiptContractSettleDepositMapper, ReceiptContractSettleDeposit> implements IReceiptContractSettleDepositService {
    @Autowired
    private ReceiptContractSettleDepositMapper pushContractSettleDepositMapper;

    @Override
    public List<ReceiptContractSettleDepositVO> selectList(ReceiptContractSettleDepositVO settleDepositVO) {
        List<ReceiptContractSettleDepositVO> list=baseMapper.selectList(settleDepositVO);
        return list;
    }

    @Override
    public boolean insertPushContractSettleDepositSaveBatch(List<ReceiptContractSettleDeposit> settleDepositList) {
        return this.saveBatch(settleDepositList);
    }
}
