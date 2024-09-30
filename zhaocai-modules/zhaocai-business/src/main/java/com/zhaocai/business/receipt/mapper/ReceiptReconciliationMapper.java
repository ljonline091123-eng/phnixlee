package com.zhaocai.business.receipt.mapper;

import java.util.List;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhaocai.business.receipt.domain.ReceiptReconciliation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.receipt.vo.res.ReceiptReconciliationDetailPageVO;
import com.zhaocai.business.receipt.vo.res.ReceiptReconciliationListVO;
import com.zhaocai.business.receipt.vo.res.query.ReconciliationQueryVO;
import org.apache.ibatis.annotations.Param;

/**
 * 材料对账单Mapper接口
 *
 * @author zhangxu
 * @date 2024-09-07
 */
public interface ReceiptReconciliationMapper extends BaseMapper<ReceiptReconciliation> {
    /**
     * 查询材料对账单详情
     *
     * @param id 对账单id
     * @return 材料对账单分页查询列表
     */
    ReceiptReconciliationDetailPageVO getDetail(Long id);

    /**
     * 分页查询材料对账单
     *
     * @param queryVO 查询参数
     * @param toMybatisPage
     * @return 材料对账单分页查询列表
     */
    IPage<ReceiptReconciliationListVO> page(Page toMybatisPage, @Param("queryVO")ReconciliationQueryVO queryVO);

    /**
     * 查询材料对账单
     *
     * @param id 材料对账单主键
     * @return 材料对账单
     */
    ReceiptReconciliation selectReceiptReconciliationById(Long id);

    /**
     * 查询材料对账单列表
     *
     * @param receiptReconciliation 材料对账单
     * @return 材料对账单集合
     */
    List<ReceiptReconciliation> selectReceiptReconciliationList(ReceiptReconciliation receiptReconciliation);

    /**
     * 新增材料对账单返回id
     *
     * @param receiptReconciliation 材料对账单
     * @return 结果
     */
    long insertReconciliation(ReceiptReconciliation receiptReconciliation);

    /**
     * 新增材料对账单
     *
     * @param receiptReconciliation 材料对账单
     * @return 结果
     */
    long insertReceiptReconciliation(ReceiptReconciliation receiptReconciliation);

    /**
     * 修改材料对账单
     *
     * @param receiptReconciliation 材料对账单
     * @return 结果
     */
    int updateReceiptReconciliation(ReceiptReconciliation receiptReconciliation);

    /**
     * 删除材料对账单
     *
     * @param id 材料对账单主键
     * @return 结果
     */
    int deleteReceiptReconciliationById(Long id);

    /**
     * 批量删除材料对账单
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    int deleteReceiptReconciliationByIds(Long[] ids);
}
