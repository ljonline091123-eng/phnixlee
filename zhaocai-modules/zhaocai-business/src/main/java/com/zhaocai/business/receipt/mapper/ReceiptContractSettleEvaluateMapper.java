package com.zhaocai.business.receipt.mapper;

import com.zhaocai.business.receipt.domain.ReceiptContractSettleEvaluate;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.receipt.vo.req.ReceiptContractSettleEvaluateVO;

import java.util.List;

/**
 * 推送结算单 Mapper接口
 *
 * @author cff
 * @date 2024-09-07
 */
public interface ReceiptContractSettleEvaluateMapper extends BaseMapper<ReceiptContractSettleEvaluate> {
    /**
     * 查询结算单
     * @param pushPurchase
     * @return
     */
    List<ReceiptContractSettleEvaluateVO> selectList(ReceiptContractSettleEvaluateVO pushPurchase);
}
