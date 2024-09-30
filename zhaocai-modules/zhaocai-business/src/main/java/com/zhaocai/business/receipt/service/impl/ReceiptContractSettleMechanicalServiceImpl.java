package com.zhaocai.business.receipt.service.impl;

import java.util.List;

import com.zhaocai.business.receipt.vo.req.ReceiptContractSettleMechanicalVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zhaocai.business.receipt.mapper.ReceiptContractSettleMechanicalMapper;
import com.zhaocai.business.receipt.domain.ReceiptContractSettleMechanical;
import com.zhaocai.business.receipt.service.IReceiptContractSettleMechanicalService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * 推送结算单 Service业务层处理
 *
 * @author cff
 * @date 2024-09-07
 */
@Service
public class ReceiptContractSettleMechanicalServiceImpl extends ServiceImpl<ReceiptContractSettleMechanicalMapper, ReceiptContractSettleMechanical> implements IReceiptContractSettleMechanicalService {
    @Autowired
    private ReceiptContractSettleMechanicalMapper pushContractSettleMechanicalMapper;

    @Override
    public List<ReceiptContractSettleMechanicalVO> selectList(ReceiptContractSettleMechanicalVO settleMechanicalVO) {
        List<ReceiptContractSettleMechanicalVO> list=baseMapper.selectList(settleMechanicalVO);
        return list;
    }

    @Override
    public boolean insertPushContractSettleMechanicalSaveBatch(List<ReceiptContractSettleMechanical> pushContractSettleMechanicalList) {
        return this.saveBatch(pushContractSettleMechanicalList);
    }
}
