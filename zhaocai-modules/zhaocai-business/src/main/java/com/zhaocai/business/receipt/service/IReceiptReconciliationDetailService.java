package com.zhaocai.business.receipt.service;

import com.zhaocai.business.receipt.domain.ReceiptReconciliationDetail;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.receipt.vo.res.ReceiptReconciliationDetailListVO;
import java.util.ArrayList;

/**
 * 材料对账单详情Service接口
 * 
 * @author zhagnxu
 * @date 2024-09-07
 */
public interface IReceiptReconciliationDetailService  extends IService<ReceiptReconciliationDetail> {

    /**
     * 查询材料对账单详情列表
     *
     * @param id 对账单id
     * @return 结果
     */
    ArrayList<ReceiptReconciliationDetailListVO> getDetailList(Long id);
}
