package com.zhaocai.business.receipt.service;

import java.util.List;
import com.zhaocai.business.receipt.domain.ReceiptPurchaseInvoiceDetail;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.receipt.vo.req.ReceiptPurchaseInvoiceDetailVO;

/**
 * 发货单详情 Service接口
 *
 * @author cff
 * @date 2024-09-13
 */
public interface IReceiptPurchaseInvoiceDetailService  extends IService<ReceiptPurchaseInvoiceDetail> {

    /**
     * 列表
     * @param pushPurchase
     * @return
     */
    List<ReceiptPurchaseInvoiceDetailVO> selectList(ReceiptPurchaseInvoiceDetailVO pushPurchase);
    /**
     * 新增发货单详情
     *
     * @param purchaseDetailList 新增发货单详情
     * @return 结果
     */
    public boolean insertPurchaseInvoiceDetailSaveBatch(List<ReceiptPurchaseInvoiceDetail> purchaseDetailList);
}
