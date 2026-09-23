package com.zhaocai.business.receipt.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhaocai.business.receipt.domain.ReceiptMaterialsDeviceLedger;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.receipt.vo.res.query.ReceiptMaterialsDeviceLedgerQueryVO;
import com.zhaocai.business.receipt.vo.req.ReceiptMaterialsDeviceLedgerVO;
import org.apache.ibatis.annotations.Param;

/**
 * 设备租赁台账 Mapper接口
 *
 * @author cff
 * @date 2024-09-10
 */
public interface ReceiptMaterialsDeviceLedgerMapper extends BaseMapper<ReceiptMaterialsDeviceLedger> {

    /**
     * 列表查询
     * @param mybatisPage
     * @param queryVO
     * @return
     */
    IPage<ReceiptMaterialsDeviceLedgerVO> selectListPge(Page mybatisPage, @Param("queryVO") ReceiptMaterialsDeviceLedgerQueryVO queryVO);

    /**
     * 修改
     * @param ledgerVO
     * @return boolean
     */
    boolean updateReceiptMaterialsDeviceLedger(ReceiptMaterialsDeviceLedgerVO ledgerVO);
}
