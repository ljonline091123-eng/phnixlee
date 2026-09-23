package com.zhaocai.business.receipt.mapper;

import java.util.List;
import com.zhaocai.business.receipt.domain.ReceiptMaterialsTurnLedgerDtl;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.receipt.vo.req.ReceiptMaterialsTurnLedgerDtlVO;

/**
 * 周材租赁台账详情 Mapper接口
 *
 * @author cff
 * @date 2024-09-10
 */
public interface ReceiptMaterialsTurnLedgerDtlMapper extends BaseMapper<ReceiptMaterialsTurnLedgerDtl> {
    /**
     * 周材租赁台账列表
     * @param pushPurchase
     * @return
     */
    List<ReceiptMaterialsTurnLedgerDtlVO> selectList(ReceiptMaterialsTurnLedgerDtlVO pushPurchase);
}
