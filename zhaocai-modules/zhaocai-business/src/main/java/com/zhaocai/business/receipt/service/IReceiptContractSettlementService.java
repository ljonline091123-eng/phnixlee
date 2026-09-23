package com.zhaocai.business.receipt.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.receipt.domain.ReceiptContractSettlement;
import com.zhaocai.business.receipt.vo.res.query.ReceiptContractSettlementQueryVO;
import com.zhaocai.business.receipt.vo.req.ReceiptContractSettlementVO;
import com.zhaocai.common.core.bean.PageResult;

/**
 * 推送结算单 Service接口
 *
 * @author cff
 * @date 2024-09-07
 */
public interface IReceiptContractSettlementService extends IService<ReceiptContractSettlement> {


    /**
     * 分页查询
     * @param queryDTO
     * @return page
     */
    PageResult<ReceiptContractSettlementVO> contractsettlementListPage(ReceiptContractSettlementQueryVO queryDTO);

    /**
     * 新增推送结算单
     *
     * @param pushContractSettlementVO 新增推送结算单
     * @return 结果
     */
    public boolean insertPushContractSettlement(ReceiptContractSettlementVO pushContractSettlementVO);

    /**
     * 详情
     * @param id
     * @return
     */
    ReceiptContractSettlementVO detail(Long id);


    /**
     * 修改结算单状态
     *
     * @param settlementVO 修改结算单状态
     * @return 结果
     */
    public boolean modifyContractsettlementState(ReceiptContractSettlementVO settlementVO);

}
