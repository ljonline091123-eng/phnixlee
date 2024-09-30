package com.zhaocai.business.receipt.service;

import cn.hutool.core.io.resource.InputStreamResource;
import com.google.zxing.WriterException;
import com.zhaocai.business.receipt.domain.ReceiptPurchase;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.receipt.vo.res.query.ReceiptPurchaseQueryVO;
import com.zhaocai.business.receipt.vo.req.ReceiptPurchaseVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.web.bean.ResultData;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 推送采购订单 Service接口
 *
 * @author CFF
 * @date 2024-09-06
 */
public interface IReceiptPurchaseService extends IService<ReceiptPurchase> {


    /**
     * 分页查询
     * @param queryDTO
     * @return page
     */
    PageResult<ReceiptPurchaseVO> purchaseListPage(ReceiptPurchaseQueryVO queryDTO);

    /**
     * 详情
     * @param id
     * @return
     */
    ReceiptPurchaseVO detail(Long id);

    /**
     * 发货单详情
     * @param id
     * @return
     */
    ReceiptPurchaseVO getInvoiceInfo(Long id);
    /**
     * 新增推送采购订单
     *
     * @param pushPurchaseVO 新增推送采购订单
     * @return 结果
     */
    public boolean insertPushPurchase(ReceiptPurchaseVO pushPurchaseVO);


    /**
     * 修改采购订单状态
     *
     * @param purchaseVO 修改采购订单状态
     * @return 结果
     */
    public boolean modifyPurchaseState(ReceiptPurchaseVO purchaseVO);

    /**
     * 推送采购订单二维码
     * @param id
     * @param response
     * @return
     * @throws WriterException
     * @throws IOException
     */
    String generateQrCodeImage(Long id, HttpServletResponse response) throws WriterException, IOException ;

}
