package com.zhaocai.business.receipt.service;

import com.zhaocai.business.receipt.domain.ReceiptReconciliation;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.receipt.vo.req.ReceiptReconciliationVO;
import com.zhaocai.business.receipt.vo.res.ReceiptReconciliationDetailPageVO;
import com.zhaocai.business.receipt.vo.res.ReceiptReconciliationListVO;
import com.zhaocai.business.receipt.vo.res.query.ReconciliationQueryVO;
import com.zhaocai.common.core.bean.PageResult;

/**
 * 材料对账单Service接口
 * 
 * @author zhangxu
 * @date 2024-09-06
 */
public interface IReceiptReconciliationService  extends IService<ReceiptReconciliation> {

    /**
     * 查询材料对账单详情列表
     *
     * @param id 材料对账单id
     * @return 结果
     */
    ReceiptReconciliationDetailPageVO getDetail(Long id);

    /**
     * 分页查询材料对账单列表
     *
     * @param queryVO 材料对账单查询条件
     * @return 结果
     */
    PageResult<ReceiptReconciliationListVO> page(ReconciliationQueryVO queryVO);

    /**
     * 新增材料对账单
     *
     * @param reconciliationVO 材料对账单
     * @return 结果
     */
    boolean saveReconciliation(ReceiptReconciliationVO reconciliationVO);
}
