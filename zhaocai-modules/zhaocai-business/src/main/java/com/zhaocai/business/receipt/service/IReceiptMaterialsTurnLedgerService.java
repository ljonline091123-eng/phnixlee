package com.zhaocai.business.receipt.service;

import com.zhaocai.business.receipt.domain.ReceiptMaterialsTurnLedger;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.receipt.vo.res.query.ReceiptMaterialsTurnLedgerQueryVO;
import com.zhaocai.business.receipt.vo.req.ReceiptMaterialsTurnLedgerVO;
import com.zhaocai.common.core.bean.PageResult;

/**
 * 租赁周材台账 Service接口
 *
 * @author cff
 * @date 2024-09-10
 */
public interface IReceiptMaterialsTurnLedgerService  extends IService<ReceiptMaterialsTurnLedger> {

    /**
     * 分页查询
     * @param queryDTO
     * @return page
     */
    PageResult<ReceiptMaterialsTurnLedgerVO> materialsTurnLedgerListPage(ReceiptMaterialsTurnLedgerQueryVO queryDTO);

    /**
     * 详情
     * @param id
     * @return
     */
    ReceiptMaterialsTurnLedgerVO detail(Long id);

    /**
     * 新增租赁周材台账
     *
     * @param turnLedgerVO 新增租赁周材台账
     * @return 结果
     */
    public boolean insertMaterialsTurnLedger(ReceiptMaterialsTurnLedgerVO turnLedgerVO);


    /**
     * 修改赁周材台账状态
     *
     * @param turnLedgerVO 修改赁周材台账状态
     * @return 结果
     */
    public boolean modifyMaterialsTurnLedgerState(ReceiptMaterialsTurnLedgerVO turnLedgerVO);
}
