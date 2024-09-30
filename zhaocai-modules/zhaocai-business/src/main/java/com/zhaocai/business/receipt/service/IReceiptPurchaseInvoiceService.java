package com.zhaocai.business.receipt.service;

import java.util.List;
import com.zhaocai.business.receipt.domain.ReceiptPurchaseInvoice;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.receipt.vo.req.ReceiptPurchaseInvoiceVO;
import com.zhaocai.business.receipt.vo.res.query.ReceiptPurchaseInvoiceQueryVO;
import com.zhaocai.common.core.bean.PageResult;

/**
 * 发货单 Service接口
 *
 * @author cff
 * @date 2024-09-13
 */
public interface IReceiptPurchaseInvoiceService  extends IService<ReceiptPurchaseInvoice> {

    /**
     * 分页查询
     * @param queryDTO
     * @return page
     */
    PageResult<ReceiptPurchaseInvoiceVO> invoiceListPage(ReceiptPurchaseInvoiceQueryVO queryDTO);


    /**
     * 二维码详情
     * @param id
     * @return
     */
    ReceiptPurchaseInvoiceVO getQrCodeInfo(Long id);

    /**
     * 详情
     * @param id
     * @return
     */
    ReceiptPurchaseInvoiceVO detail(Long id);

    /**
     * 列表
     * @param pushPurchase
     * @return
     */
    List<ReceiptPurchaseInvoiceVO> selectList(ReceiptPurchaseInvoiceVO pushPurchase);


    /**
     * 新增发货单
     *
     * @param purchaseInvoiceVO 新增发货单
     * @return 结果
     */
    public boolean saveInvoice(ReceiptPurchaseInvoiceVO purchaseInvoiceVO);
}
