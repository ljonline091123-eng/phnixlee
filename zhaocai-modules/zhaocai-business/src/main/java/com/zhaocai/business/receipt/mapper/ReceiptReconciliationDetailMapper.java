package com.zhaocai.business.receipt.mapper;

import java.util.ArrayList;
import java.util.List;
import com.zhaocai.business.receipt.domain.ReceiptReconciliationDetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.receipt.vo.res.ReceiptReconciliationDetailListVO;

/**
 * 材料对账单详情Mapper接口
 * 
 * @author zhagnxu
 * @date 2024-09-07
 */
public interface ReceiptReconciliationDetailMapper extends BaseMapper<ReceiptReconciliationDetail> {
    /**
     * 查询材料对账单详情
     *
     * @param id 材料对账单详情主键
     * @return 材料对账单详情
     */
    ArrayList<ReceiptReconciliationDetailListVO> getDetailList(Long id);

    /**
     * 查询材料对账单详情
     *
     * @param id 材料对账单详情主键
     * @return 材料对账单详情
     */
    ReceiptReconciliationDetail selectReceiptReconciliationDetailById(Long id);

    /**
     * 查询材料对账单详情列表
     * 
     * @param receiptReconciliationDetail 材料对账单详情
     * @return 材料对账单详情集合
     */
    List<ReceiptReconciliationDetail> selectReceiptReconciliationDetailList(ReceiptReconciliationDetail receiptReconciliationDetail);

    /**
     * 新增材料对账单详情
     * 
     * @param receiptReconciliationDetail 材料对账单详情
     * @return 结果
     */
    int insertReceiptReconciliationDetail(ReceiptReconciliationDetail receiptReconciliationDetail);

    /**
     * 修改材料对账单详情
     * 
     * @param receiptReconciliationDetail 材料对账单详情
     * @return 结果
     */
    int updateReceiptReconciliationDetail(ReceiptReconciliationDetail receiptReconciliationDetail);

    /**
     * 删除材料对账单详情
     * 
     * @param id 材料对账单详情主键
     * @return 结果
     */
    int deleteReceiptReconciliationDetailById(Long id);

    /**
     * 批量删除材料对账单详情
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    int deleteReceiptReconciliationDetailByIds(Long[] ids);
}
