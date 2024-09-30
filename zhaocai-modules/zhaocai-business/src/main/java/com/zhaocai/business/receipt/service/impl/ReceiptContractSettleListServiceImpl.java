package com.zhaocai.business.receipt.service.impl;

import java.util.List;

import com.zhaocai.business.receipt.vo.req.ReceiptContractSettleListVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zhaocai.business.receipt.mapper.ReceiptContractSettleListMapper;
import com.zhaocai.business.receipt.domain.ReceiptContractSettleList;
import com.zhaocai.business.receipt.service.IReceiptContractSettleListService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * 推送结算单 Service业务层处理
 *
 * @author cff
 * @date 2024-09-07
 */
@Service
public class ReceiptContractSettleListServiceImpl extends ServiceImpl<ReceiptContractSettleListMapper, ReceiptContractSettleList> implements IReceiptContractSettleListService {
    @Autowired
    private ReceiptContractSettleListMapper pushContractSettleListMapper;

    @Override
    public List<ReceiptContractSettleListVO> selectList(ReceiptContractSettleListVO settleListVO) {
        List<ReceiptContractSettleListVO> list=baseMapper.selectList(settleListVO);
        return list;
    }
    @Override
    public boolean insertPushContractSettleListSaveBatch(List<ReceiptContractSettleList> pushContractSettleLists) {
        return this.saveBatch(pushContractSettleLists);
    }
}
