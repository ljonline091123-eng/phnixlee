package com.zhaocai.business.receipt.mapper;

import java.util.List;
import com.zhaocai.business.receipt.domain.ReceiptPurchaseInvoiceDetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.receipt.vo.req.ReceiptPurchaseInvoiceDetailVO;

/**
 * 发货单 详情 Mapper接口
 *
 * @author cff
 * @date 2024-09-13
 */
public interface ReceiptPurchaseInvoiceDetailMapper extends BaseMapper<ReceiptPurchaseInvoiceDetail> {
    /**
     * 查询发货单详情
     * @param pushPurchase
     * @return
     */
    List<ReceiptPurchaseInvoiceDetailVO> selectList(ReceiptPurchaseInvoiceDetailVO pushPurchase);
}
