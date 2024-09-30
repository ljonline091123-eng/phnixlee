package com.zhaocai.business.receipt.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.receipt.domain.ReceiptMaterialsTurnLedgerDtlDtl;
import com.zhaocai.business.receipt.mapper.ReceiptMaterialsTurnLedgerDtlDtlMapper;
import com.zhaocai.business.receipt.service.IReceiptMaterialsTurnLedgerDtlDtlService;
import com.zhaocai.business.receipt.vo.req.ReceiptMaterialsTurnLedgerDtlDtlVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 周材租赁台账详情 Service业务层处理
 *
 * @author cff
 * @date 2024-09-10
 */
@Service
public class ReceiptMaterialsTurnLedgerDtlDtlServiceImpl extends ServiceImpl<ReceiptMaterialsTurnLedgerDtlDtlMapper,
        ReceiptMaterialsTurnLedgerDtlDtl> implements IReceiptMaterialsTurnLedgerDtlDtlService {
    @Autowired
    private ReceiptMaterialsTurnLedgerDtlDtlMapper receiptMaterialsTurnLedgerDtlDtlMapper;


    /**
     * 设备租赁台账详情列表
     * @param id
     * @return
     */
    @Override
    public List<ReceiptMaterialsTurnLedgerDtlDtlVO> getReceiptMaterialsTurnLedgerDtlDtlList(Long id) {
        ReceiptMaterialsTurnLedgerDtlDtlVO turnLedgerDtlDtlVO=new ReceiptMaterialsTurnLedgerDtlDtlVO();
        turnLedgerDtlDtlVO.setParntId(String.valueOf(id));
        List<ReceiptMaterialsTurnLedgerDtlDtlVO> list = baseMapper.selectList(turnLedgerDtlDtlVO);
        return list;
    }

    @Override
    public boolean insertReceiptMaterialsTurnLedgerDtlDtlSaveBatch(List<ReceiptMaterialsTurnLedgerDtlDtl> turnLedgerDtlDtlList) {
        return this.saveBatch(turnLedgerDtlDtlList);
    }
}
