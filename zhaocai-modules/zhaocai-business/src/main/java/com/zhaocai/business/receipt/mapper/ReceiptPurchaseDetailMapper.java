package com.zhaocai.business.receipt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.receipt.domain.ReceiptPurchaseDetail;
import com.zhaocai.business.receipt.vo.req.ReceiptPurchaseDetailVO;

import java.util.List;

/**
 * 推送采购订单明细 Mapper接口
 *
 * @author CFF
 * @date 2024-09-06
 */
public interface ReceiptPurchaseDetailMapper extends BaseMapper<ReceiptPurchaseDetail> {
    /**
     * 查询推送采购订单
     * @param pushPurchase
     * @return
     */
    List<ReceiptPurchaseDetailVO> selectList(ReceiptPurchaseDetailVO pushPurchase);
}
