package com.zhaocai.business.receipt.service.impl;

import java.util.List;

import com.zhaocai.business.receipt.vo.req.ReceiptContractSettleOtherPaymentVO;
import com.zhaocai.business.receipt.vo.req.ReceiptContractSettleSporadicWorkVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zhaocai.business.receipt.mapper.ReceiptContractSettleSporadicWorkMapper;
import com.zhaocai.business.receipt.domain.ReceiptContractSettleSporadicWork;
import com.zhaocai.business.receipt.service.IReceiptContractSettleSporadicWorkService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 *  推送结算单 Service业务层处理
 *
 * @author cff
 * @date 2024-09-07
 */
@Service
public class ReceiptContractSettleSporadicWorkServiceImpl extends ServiceImpl<ReceiptContractSettleSporadicWorkMapper, ReceiptContractSettleSporadicWork> implements IReceiptContractSettleSporadicWorkService {
    @Autowired
    private ReceiptContractSettleSporadicWorkMapper pushContractSettleSporadicWorkMapper;

    @Override
    public List<ReceiptContractSettleSporadicWorkVO> selectList(ReceiptContractSettleSporadicWorkVO settleSporadicWorkVO) {
        List<ReceiptContractSettleSporadicWorkVO> list=baseMapper.selectList(settleSporadicWorkVO);
        return list;
    }

    @Override
    public boolean insertSporadicWorksSaveBatch(List<ReceiptContractSettleSporadicWork> sporadicWorksList) {
        return this.saveBatch(sporadicWorksList);
    }
}
