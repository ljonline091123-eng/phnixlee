package com.zhaocai.business.receipt.service.impl;

import java.util.List;

import com.zhaocai.business.receipt.vo.req.ReceiptContractSettleMechanicalVO;
import com.zhaocai.business.receipt.vo.req.ReceiptContractSettleOtherPaymentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zhaocai.business.receipt.mapper.ReceiptContractSettleOtherPaymentMapper;
import com.zhaocai.business.receipt.domain.ReceiptContractSettleOtherPayment;
import com.zhaocai.business.receipt.service.IReceiptContractSettleOtherPaymentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * 推送结算单 Service业务层处理
 *
 * @author cff
 * @date 2024-09-07
 */
@Service
public class ReceiptContractSettleOtherPaymentServiceImpl extends ServiceImpl<ReceiptContractSettleOtherPaymentMapper, ReceiptContractSettleOtherPayment> implements IReceiptContractSettleOtherPaymentService {
    @Autowired
    private ReceiptContractSettleOtherPaymentMapper pushContractSettleOtherPaymentMapper;


    @Override
    public List<ReceiptContractSettleOtherPaymentVO> selectList(ReceiptContractSettleOtherPaymentVO settleOtherPaymentVO) {
        List<ReceiptContractSettleOtherPaymentVO> list=baseMapper.selectList(settleOtherPaymentVO);
        return list;
    }

    @Override
    public boolean insertOtherPaymentListSaveBatch(List<ReceiptContractSettleOtherPayment> otherPaymentList) {
        return this.saveBatch(otherPaymentList);
    }
}
