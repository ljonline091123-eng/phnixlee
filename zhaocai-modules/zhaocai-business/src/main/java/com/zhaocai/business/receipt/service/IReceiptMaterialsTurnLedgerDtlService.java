package com.zhaocai.business.receipt.service;

import java.util.List;
import com.zhaocai.business.receipt.domain.ReceiptMaterialsTurnLedgerDtl;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.receipt.vo.req.ReceiptMaterialsTurnLedgerDtlVO;

/**
 * 周材租赁台账详情 Service接口
 *
 * @author cff
 * @date 2024-09-10
 */
public interface IReceiptMaterialsTurnLedgerDtlService  extends IService<ReceiptMaterialsTurnLedgerDtl> {

    /**
     * 列表
     * @param turnLedgerDtlVO
     * @return
     */
    List<ReceiptMaterialsTurnLedgerDtlVO> selectList(ReceiptMaterialsTurnLedgerDtlVO turnLedgerDtlVO);

    /**
     * 新增设备租赁台账详情
     *
     * @param ledgerDtlVOList 新增设备租赁台账详情
     * @param id 父节点id
     * @return 结果
     */
    public boolean insertReceiptMaterialsDeviceLedgerDtlSaveBatch(List<ReceiptMaterialsTurnLedgerDtlVO> ledgerDtlVOList, String id);
}
