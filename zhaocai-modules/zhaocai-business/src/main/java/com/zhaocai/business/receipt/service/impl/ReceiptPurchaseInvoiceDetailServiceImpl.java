package com.zhaocai.business.receipt.service.impl;

import java.util.List;

import com.zhaocai.business.receipt.vo.req.ReceiptPurchaseInvoiceDetailVO;
import com.zhaocai.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zhaocai.business.receipt.mapper.ReceiptPurchaseInvoiceDetailMapper;
import com.zhaocai.business.receipt.domain.ReceiptPurchaseInvoiceDetail;
import com.zhaocai.business.receipt.service.IReceiptPurchaseInvoiceDetailService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * 发货单详情 Service业务层处理
 *
 * @author WH
 * @date 2024-09-13
 */
@Service
public class ReceiptPurchaseInvoiceDetailServiceImpl extends ServiceImpl<ReceiptPurchaseInvoiceDetailMapper,
        ReceiptPurchaseInvoiceDetail> implements IReceiptPurchaseInvoiceDetailService {
    @Autowired
    private ReceiptPurchaseInvoiceDetailMapper receiptPurchaseInvoiceDetailMapper;

    @Override
    public List<ReceiptPurchaseInvoiceDetailVO> selectList(ReceiptPurchaseInvoiceDetailVO pushPurchase) {
        List<ReceiptPurchaseInvoiceDetailVO> list=baseMapper.selectList(pushPurchase);
        return list;
    }

    @Override
    public boolean insertPurchaseInvoiceDetailSaveBatch(List<ReceiptPurchaseInvoiceDetail> purchaseDetailList) {
        return this.saveBatch(purchaseDetailList);
    }
}
