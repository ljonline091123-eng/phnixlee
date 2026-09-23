package com.zhaocai.business.receipt.service;

import java.util.List;
import com.zhaocai.business.receipt.domain.ReceiptMaterialsTurnLedgerDtlDtl;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.receipt.vo.req.ReceiptMaterialsTurnLedgerDtlDtlVO;

/**
 * 周材租赁台账详情 Service接口
 *
 * @author cff
 * @date 2024-09-10
 */
public interface IReceiptMaterialsTurnLedgerDtlDtlService  extends IService<ReceiptMaterialsTurnLedgerDtlDtl> {

    /**
     * 查询列表
     * @param id
     * @return page
     */
    List<ReceiptMaterialsTurnLedgerDtlDtlVO> getReceiptMaterialsTurnLedgerDtlDtlList(Long id);
    /**
     * 新增设备租赁台账详情
     *
     * @param turnLedgerDtlDtlList 新增设备租赁台账详情
     * @return 结果
     */
    public boolean insertReceiptMaterialsTurnLedgerDtlDtlSaveBatch(List<ReceiptMaterialsTurnLedgerDtlDtl> turnLedgerDtlDtlList);
}
