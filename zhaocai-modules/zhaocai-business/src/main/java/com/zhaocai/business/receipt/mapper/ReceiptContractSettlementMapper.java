package com.zhaocai.business.receipt.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhaocai.business.receipt.domain.ReceiptContractSettlement;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.receipt.vo.res.query.ReceiptContractSettlementQueryVO;
import com.zhaocai.business.receipt.vo.req.ReceiptContractSettlementVO;
import org.apache.ibatis.annotations.Param;

/**
 * 推送结算单 Mapper接口
 *
 * @author cff
 * @date 2024-09-07
 */
public interface ReceiptContractSettlementMapper extends BaseMapper<ReceiptContractSettlement> {



    /**
     * 列表查询
     * @param mybatisPage
     * @param queryVO
     * @return
     */
    IPage<ReceiptContractSettlementVO> selectListPge(Page mybatisPage, @Param("queryVO") ReceiptContractSettlementQueryVO queryVO);

    /**
     * 查询推送结算单
     * @param pushPurchase
     * @return
     */
    List<ReceiptContractSettlementVO> selectList(ReceiptContractSettlementVO pushPurchase);


    /**
     * 修改
     * @param settlementVO
     * @return
     */
    boolean modifyContractsettlementState(ReceiptContractSettlementVO settlementVO);

}
