package com.zhaocai.business.receipt.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhaocai.business.receipt.domain.ReceiptMaterialsTurnLedger;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.receipt.vo.res.query.ReceiptMaterialsTurnLedgerQueryVO;
import com.zhaocai.business.receipt.vo.req.ReceiptMaterialsTurnLedgerVO;
import org.apache.ibatis.annotations.Param;

/**
 * 租赁周材台账 Mapper接口
 *
 * @author cff
 * @date 2024-09-10
 */
public interface ReceiptMaterialsTurnLedgerMapper extends BaseMapper<ReceiptMaterialsTurnLedger> {

    /**
     * 列表查询
     * @param mybatisPage
     * @param queryVO
     * @return
     */
    IPage<ReceiptMaterialsTurnLedgerVO> selectListPge(Page mybatisPage, @Param("queryVO") ReceiptMaterialsTurnLedgerQueryVO queryVO);


    /**
     * 修改
     * @param turnLedgerVO
     * @return
     */
    boolean updateReceiptMaterialsTurnLedger(ReceiptMaterialsTurnLedgerVO turnLedgerVO);
}
