package com.zhaocai.business.receipt.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.receipt.domain.ReceiptMaterialsDeviceLedgerDtlDtl;
import com.zhaocai.business.receipt.vo.req.ReceiptMaterialsDeviceLedgerDtlDtlVO;
import com.zhaocai.common.core.bean.PageResult;

import java.util.List;

/**
 * 设备租赁台账详情 Service接口
 *
 * @author cff
 * @date 2024-09-10
 */
public interface IReceiptMaterialsDeviceLedgerDtlDtlService  extends IService<ReceiptMaterialsDeviceLedgerDtlDtl> {

    /**
     * 查询列表
     * @param id
     * @return page
     */
    List<ReceiptMaterialsDeviceLedgerDtlDtlVO> getMaterialsDeviceLedgerDtlDtlList(Long id);

    /**
     * 新增设备租赁台账详情
     *
     * @param ledgerDtlVOList 新增设备租赁台账详情
     * @return 结果
     */
    public boolean insertReceiptMaterialsDeviceLedgerDtlDtlSaveBatch(List<ReceiptMaterialsDeviceLedgerDtlDtl> ledgerDtlVOList);

}
