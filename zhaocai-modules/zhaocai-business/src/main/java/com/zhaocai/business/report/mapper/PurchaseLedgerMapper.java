package com.zhaocai.business.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.report.domain.PurchaseLedger;
import com.zhaocai.business.report.vo.req.PurchaseLedgerQueryVo;
import com.zhaocai.business.report.vo.res.PurchaseLedgerListVo;
import com.zhaocai.business.report.vo.res.PurchaseLedgerSummaryVo;

import java.util.List;

/**
 * 采购台账 Mapper
 *
 * @author claude
 */
public interface PurchaseLedgerMapper extends BaseMapper<PurchaseLedger> {

    /** 清空物化表（刷数用） */
    int deleteAll();

    /** 视图全量灌入物化表（刷数用） */
    int insertFromView();

    /** 汇总视图聚合查询 */
    List<PurchaseLedgerSummaryVo> selectSummary(PurchaseLedgerQueryVo queryVo);

    /** 明细宽表分页查询 */
    List<PurchaseLedgerListVo> selectPageList(PurchaseLedgerQueryVo queryVo);
}
