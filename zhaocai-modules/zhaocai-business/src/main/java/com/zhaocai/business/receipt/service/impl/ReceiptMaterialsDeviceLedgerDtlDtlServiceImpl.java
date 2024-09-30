package com.zhaocai.business.receipt.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.receipt.domain.ReceiptMaterialsDeviceLedgerDtlDtl;
import com.zhaocai.business.receipt.mapper.ReceiptMaterialsDeviceLedgerDtlDtlMapper;
import com.zhaocai.business.receipt.service.IReceiptMaterialsDeviceLedgerDtlDtlService;
import com.zhaocai.business.receipt.vo.req.ReceiptMaterialsDeviceLedgerDtlDtlVO;
import com.zhaocai.common.core.bean.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 设备租赁台账详情 Service业务层处理
 *
 * @author cff
 * @date 2024-09-10
 */
@Service
public class ReceiptMaterialsDeviceLedgerDtlDtlServiceImpl extends ServiceImpl<ReceiptMaterialsDeviceLedgerDtlDtlMapper,
        ReceiptMaterialsDeviceLedgerDtlDtl> implements IReceiptMaterialsDeviceLedgerDtlDtlService {
    @Autowired
    private ReceiptMaterialsDeviceLedgerDtlDtlMapper receiptMaterialsDeviceLedgerDtlDtlMapper;


    /**
     * 设备租赁台账详情列表
     * @param id
     * @return
     */
    @Override
    public List<ReceiptMaterialsDeviceLedgerDtlDtlVO> getMaterialsDeviceLedgerDtlDtlList(Long id) {
        ReceiptMaterialsDeviceLedgerDtlDtlVO ledgerDtlDtlVO=new ReceiptMaterialsDeviceLedgerDtlDtlVO();
        ledgerDtlDtlVO.setParntId(String.valueOf(id));
        List<ReceiptMaterialsDeviceLedgerDtlDtlVO> list = baseMapper.selectList(ledgerDtlDtlVO);
        return list;
    }


    @Override
    public boolean insertReceiptMaterialsDeviceLedgerDtlDtlSaveBatch(List<ReceiptMaterialsDeviceLedgerDtlDtl> ledgerDtlVOList) {
        return this.saveBatch(ledgerDtlVOList);
    }
}
