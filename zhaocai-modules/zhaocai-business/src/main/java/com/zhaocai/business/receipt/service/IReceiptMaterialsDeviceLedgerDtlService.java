package com.zhaocai.business.receipt.service;

import java.util.List;
import com.zhaocai.business.receipt.domain.ReceiptMaterialsDeviceLedgerDtl;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.receipt.domain.ReceiptMaterialsDeviceLedgerDtlDtl;
import com.zhaocai.business.receipt.vo.req.ReceiptMaterialsDeviceLedgerDtlVO;

/**
 * 设备租赁台账详情 Service接口
 *
 * @author cff
 * @date 2024-09-10
 */
public interface IReceiptMaterialsDeviceLedgerDtlService  extends IService<ReceiptMaterialsDeviceLedgerDtl> {


    /**
     * 列表
     * @param deviceLedgerDtlVO
     * @return
     */
    List<ReceiptMaterialsDeviceLedgerDtlVO> selectList(ReceiptMaterialsDeviceLedgerDtlVO deviceLedgerDtlVO);

    /**
     * 新增设备租赁台账详情
     *
     * @param ledgerDtlVOList 新增设备租赁台账详情
     * @param id 父节点id
     * @return 结果
     */
    public boolean insertReceiptMaterialsDeviceLedgerDtlSaveBatch(List<ReceiptMaterialsDeviceLedgerDtlVO> ledgerDtlVOList,String id);
}
