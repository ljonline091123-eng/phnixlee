package com.zhaocai.business.receipt.service;

import com.zhaocai.business.receipt.domain.ReceiptMaterialsDeviceLedger;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.receipt.vo.res.query.ReceiptMaterialsDeviceLedgerQueryVO;
import com.zhaocai.business.receipt.vo.req.ReceiptMaterialsDeviceLedgerVO;
import com.zhaocai.common.core.bean.PageResult;

/**
 * 设备租赁台账 Service接口
 *
 * @author cff
 * @date 2024-09-10
 */
public interface IReceiptMaterialsDeviceLedgerService  extends IService<ReceiptMaterialsDeviceLedger> {


    /**
     * 分页查询
     * @param queryDTO
     * @return page
     */
    PageResult<ReceiptMaterialsDeviceLedgerVO> materialsDeviceLedgerListPage(ReceiptMaterialsDeviceLedgerQueryVO queryDTO);

    /**
     * 详情
     * @param id
     * @return
     */
    ReceiptMaterialsDeviceLedgerVO detail(Long id);


    /**
     * 新增设备租赁台账
     *
     * @param pushPurchaseVO 新增设备租赁台账
     * @return 结果
     */
    public boolean insertMaterialsDeviceLedger(ReceiptMaterialsDeviceLedgerVO pushPurchaseVO);


    /**
     * 修改设备租赁台账状态
     *
     * @param pushPurchaseVO 修改设备租赁台账状态
     * @return 结果
     */
    public boolean modifyDeviceLedgerState(ReceiptMaterialsDeviceLedgerVO pushPurchaseVO);
}
