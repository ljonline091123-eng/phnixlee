package com.zhaocai.business.receipt.service.impl;

import java.util.List;

import com.zhaocai.business.receipt.vo.req.ReceiptContractSettleDatallVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zhaocai.business.receipt.mapper.ReceiptContractSettleDatallMapper;
import com.zhaocai.business.receipt.domain.ReceiptContractSettleDatall;
import com.zhaocai.business.receipt.service.IReceiptContractSettleDatallService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * 推送结算单 Service业务层处理
 *
 * @author cff
 * @date 2024-09-07
 */
@Service
public class ReceiptContractSettleDatallServiceImpl extends ServiceImpl<ReceiptContractSettleDatallMapper, ReceiptContractSettleDatall> implements IReceiptContractSettleDatallService {
    @Autowired
    private ReceiptContractSettleDatallMapper pushContractSettleDatallMapper;

    @Override
    public List<ReceiptContractSettleDatallVO> selectList(ReceiptContractSettleDatallVO settleDatallVO) {
        List<ReceiptContractSettleDatallVO> list=baseMapper.selectList(settleDatallVO);
        return list;
    }

    @Override
    public boolean insertPushContractSettleDatallSaveBatch(List<ReceiptContractSettleDatall> pushContractSettleDatallList) {
        return this.saveBatch(pushContractSettleDatallList);
    }
}
