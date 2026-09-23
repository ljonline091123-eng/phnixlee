package com.zhaocai.business.receipt.mapper;

import java.util.List;
import com.zhaocai.business.receipt.domain.ReceiptMaterialsDeviceLedgerDtl;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.receipt.vo.req.ReceiptMaterialsDeviceLedgerDtlVO;

/**
 * 设备租赁台账详情 Mapper接口
 *
 * @author cff
 * @date 2024-09-10
 */
public interface ReceiptMaterialsDeviceLedgerDtlMapper extends BaseMapper<ReceiptMaterialsDeviceLedgerDtl> {

    /**
     * 查询设备租赁台账详情
     * @param pushPurchase
     * @return
     */
    List<ReceiptMaterialsDeviceLedgerDtlVO> selectList(ReceiptMaterialsDeviceLedgerDtlVO pushPurchase);

}
