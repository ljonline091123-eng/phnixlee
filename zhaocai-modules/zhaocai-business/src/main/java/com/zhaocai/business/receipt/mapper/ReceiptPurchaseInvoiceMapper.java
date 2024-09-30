package com.zhaocai.business.receipt.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhaocai.business.receipt.domain.ReceiptPurchaseInvoice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.receipt.vo.req.ReceiptPurchaseInvoiceVO;
import com.zhaocai.business.receipt.vo.res.query.ReceiptPurchaseInvoiceQueryVO;
import org.apache.ibatis.annotations.Param;

/**
 * 发货单 Mapper接口
 *
 * @author cff
 * @date 2024-09-13
 */
public interface ReceiptPurchaseInvoiceMapper extends BaseMapper<ReceiptPurchaseInvoice> {

    /**
     * 列表查询
     * @param mybatisPage
     * @param queryVO
     * @return
     */
    IPage<ReceiptPurchaseInvoiceVO> selectListPge(Page mybatisPage, @Param("queryVO") ReceiptPurchaseInvoiceQueryVO queryVO);
    /**
     * 查询推送采购订单
     * @param pushPurchase
     * @return
     */
    List<ReceiptPurchaseInvoiceVO> selectList(ReceiptPurchaseInvoiceVO pushPurchase);
}
