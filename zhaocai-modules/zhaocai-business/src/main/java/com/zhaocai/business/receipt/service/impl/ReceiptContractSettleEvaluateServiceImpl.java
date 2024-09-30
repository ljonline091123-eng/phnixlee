package com.zhaocai.business.receipt.service.impl;

import java.util.List;

import com.zhaocai.business.receipt.vo.req.ReceiptContractSettleEvaluateVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zhaocai.business.receipt.mapper.ReceiptContractSettleEvaluateMapper;
import com.zhaocai.business.receipt.domain.ReceiptContractSettleEvaluate;
import com.zhaocai.business.receipt.service.IReceiptContractSettleEvaluateService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * 推送结算单 Service业务层处理
 *
 * @author cff
 * @date 2024-09-07
 */
@Service
public class ReceiptContractSettleEvaluateServiceImpl extends ServiceImpl<ReceiptContractSettleEvaluateMapper, ReceiptContractSettleEvaluate> implements IReceiptContractSettleEvaluateService {
    @Autowired
    private ReceiptContractSettleEvaluateMapper pushContractSettleEvaluateMapper;

    @Override
    public List<ReceiptContractSettleEvaluateVO> selectList(ReceiptContractSettleEvaluateVO settleEvaluateVO) {
        List<ReceiptContractSettleEvaluateVO> list=baseMapper.selectList(settleEvaluateVO);
        return list;
    }
    @Override
    public boolean insertPushContractSettleEvaluateSaveBatch(List<ReceiptContractSettleEvaluate> pushContractSettleEvaluateList) {
        return this.saveBatch(pushContractSettleEvaluateList);
    }
}
