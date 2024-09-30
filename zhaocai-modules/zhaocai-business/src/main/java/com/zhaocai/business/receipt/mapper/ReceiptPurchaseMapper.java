package com.zhaocai.business.receipt.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhaocai.business.receipt.domain.ReceiptPurchase;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.receipt.vo.res.query.ReceiptPurchaseQueryVO;
import com.zhaocai.business.receipt.vo.req.ReceiptPurchaseVO;
import org.apache.ibatis.annotations.Param;

/**
 * 推送采购订单 Mapper接口
 *
 * @author CFF
 * @date 2024-09-06
 */
public interface ReceiptPurchaseMapper extends BaseMapper<ReceiptPurchase> {

    /**
     * 查询推送采购订单
     * @param pushPurchase
     * @return
     */
    List<ReceiptPurchaseVO> selectList(ReceiptPurchaseVO pushPurchase);

    /**
     * 列表查询
     * @param mybatisPage
     * @param queryVO
     * @return
     */
    IPage<ReceiptPurchaseVO> selectListPge(Page mybatisPage, @Param("queryVO") ReceiptPurchaseQueryVO queryVO);
    /**
     * 新增 推送采购订单
     *
     * @param pushPurchase 推送采购订单
     * @return 结果
     */
    public int insertPushPurchase(ReceiptPurchase pushPurchase);


    /**
     * 修改
     * @param settlementVO
     * @return boolean
     */
    boolean updatePushPurchase(ReceiptPurchaseVO settlementVO);

}
