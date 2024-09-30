package com.zhaocai.business.receipt.service.impl;

import java.io.*;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.io.resource.InputStreamResource;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.zhaocai.business.common.config.EnvironmentUtil;
import com.zhaocai.business.common.utils.ValidateUtils;
import com.zhaocai.business.receipt.domain.ReceiptPurchase;
import com.zhaocai.business.receipt.domain.ReceiptPurchaseDetail;
import com.zhaocai.business.receipt.enums.InvoiceEnum;
import com.zhaocai.business.receipt.service.IReceiptPurchaseDetailService;
import com.zhaocai.business.receipt.service.IReceiptPurchaseInvoiceService;
import com.zhaocai.business.receipt.vo.req.ReceiptPurchaseDetailVO;
import com.zhaocai.business.receipt.vo.req.ReceiptPurchaseInvoiceVO;
import com.zhaocai.business.receipt.vo.res.query.ReceiptPurchaseQueryVO;
import com.zhaocai.business.receipt.vo.req.ReceiptPurchaseVO;
import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.business.vendor.service.IVendorService;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.bean.BeanUtils;
import com.zhaocai.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.zhaocai.business.receipt.mapper.ReceiptPurchaseMapper;
import com.zhaocai.business.receipt.service.IReceiptPurchaseService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 推送采购订单 Service业务层处理
 *
 * @author CFF
 * @date 2024-09-06
 */
@Service
public class ReceiptPurchaseServiceImpl extends ServiceImpl<ReceiptPurchaseMapper, ReceiptPurchase> implements IReceiptPurchaseService {
    @Autowired
    private IReceiptPurchaseDetailService iPushPurchaseDetailService;


    @Autowired
    private ReceiptPurchaseMapper pushPurchaseMapper;


    @Autowired
    private IReceiptPurchaseInvoiceService purchaseInvoiceService;


    @Autowired
    private IVendorService vendorService;

    /**
     * 修改采购订单状态
     *
     * @param settlementVO 修改采购订单状态
     * @return 结果
     */
    @Override
    public boolean modifyPurchaseState(ReceiptPurchaseVO settlementVO) {
        return baseMapper.updatePushPurchase(settlementVO);
    }

    /**
     * 采购订单列表
     * @param queryDTO
     * @return
     */
    @Override
    public PageResult<ReceiptPurchaseVO> purchaseListPage(ReceiptPurchaseQueryVO queryDTO) {
       queryDTO.setSupplierId(String.valueOf(getVendor(SecurityUtils.getUserId()).getId()));
       System.out.println(String.valueOf(getVendor(SecurityUtils.getUserId()).getId()));
        IPage<ReceiptPurchaseVO> pages = baseMapper.selectListPge(queryDTO.toMybatisPage(),queryDTO);
        if(!CollectionUtils.isEmpty(pages.getRecords())){
            pages.getRecords().stream()
                    .map(tem -> {
                        //判断当前可下订单数量为0了并且明细生成发货单状态必须是1 默认状态才是已生成
                        int count=iPushPurchaseDetailService.purchaseDetailSum(String.valueOf(tem.getId()));
                        if(count==0){
                            tem.setState(StringUtils.isNotEmpty(tem.getState())&&tem.getState()=="3"?"3":"0");
                        }else {
                            tem.setState("2");
                        }
                        tem.setStateName(StringUtils.isNotEmpty(tem.getState())? InvoiceEnum.getValueByCode(tem.getState())!=null
                                ?InvoiceEnum.getValueByCode(tem.getState()):tem.getState():null);
                        return tem;
                    })
                    .collect(Collectors.toList());
        }
        return new PageResult<>(pages);
    }

    private Vendor getVendor(Long userId){
        return vendorService.getByLoginUser(userId);
    }

    @Override
    public String generateQrCodeImage(Long id, HttpServletResponse response) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        String codePrefixUrl = EnvironmentUtil.getProperty("underling.code-brefix-url");
        BitMatrix bitMatrix = qrCodeWriter.encode(codePrefixUrl+"/business/invoice/getQrCodeInfo/"+id, BarcodeFormat.QR_CODE, 350, 350);
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        String base64String = Base64.getEncoder().encodeToString(pngOutputStream.toByteArray());
        // 输出Base64字符串
        return base64String;
    }

    @Override
    public ReceiptPurchaseVO detail(Long id) {
        ReceiptPurchase purchase = super.getById(id);
        ValidateUtils.isNullException(purchase,"该采购订单不存在");
        ReceiptPurchaseDetailVO purchaseVO=new ReceiptPurchaseDetailVO();
        purchaseVO.setParntId(String.valueOf(purchase.getId()));
        List<ReceiptPurchaseDetailVO> selectList=iPushPurchaseDetailService.selectList(purchaseVO);
        ReceiptPurchaseVO receiptPurchaseVO=new ReceiptPurchaseVO();
        BeanUtils.copyBeanProp(receiptPurchaseVO,purchase);
        receiptPurchaseVO.setDtlRespList(selectList);
        //操作状态
        receiptPurchaseVO.setStateName(StringUtils.isNotEmpty(receiptPurchaseVO.getState())? InvoiceEnum.getValueByCode(receiptPurchaseVO.getState())!=null
                ?InvoiceEnum.getValueByCode(receiptPurchaseVO.getState()):receiptPurchaseVO.getState():null);
        return receiptPurchaseVO;
    }

    @Override
    public ReceiptPurchaseVO getInvoiceInfo(Long id) {
        ReceiptPurchase purchase = super.getById(id);
        ValidateUtils.isNullException(purchase,"该采购订单不存在");
        ReceiptPurchaseDetailVO purchaseVO=new ReceiptPurchaseDetailVO();
        purchaseVO.setParntId(String.valueOf(purchase.getId()));
        List<ReceiptPurchaseDetailVO> selectList=iPushPurchaseDetailService.selectList(purchaseVO);
        ReceiptPurchaseVO receiptPurchaseVO=new ReceiptPurchaseVO();
        BeanUtils.copyBeanProp(receiptPurchaseVO,purchase);
        if (!CollectionUtils.isEmpty(selectList)) {
            //发货单名称
            String brand=StringUtils.isNotNull(selectList.get(0).getBrand())?selectList.get(0).getBrand():"";
            String materialName=StringUtils.isNotNull(selectList.get(0).getMaterialName())?selectList.get(0).getMaterialName():"";
            receiptPurchaseVO.setBrandMaterialName(brand+materialName+"发货单");
            //发货单编码
            ReceiptPurchaseInvoiceVO purchaseInvoiceVO=new ReceiptPurchaseInvoiceVO();
            purchaseInvoiceVO.setReceiptPurchaseId(purchase.getId());
            List<ReceiptPurchaseInvoiceVO> list=purchaseInvoiceService.selectList(purchaseInvoiceVO);
            if(!CollectionUtils.isEmpty(list)&&list.size()>0){
                receiptPurchaseVO.setInvoiceCode("FHSG"+ DateUtils.dateTime()+formatNumber(list.get(0).getCodeSerialNumber(),1));
                receiptPurchaseVO.setCodeSerialNumber(formatNumber(list.get(0).getCodeSerialNumber(),1));
            }else {
                receiptPurchaseVO.setInvoiceCode("FHSG"+ DateUtils.dateTime()+"0001");
                receiptPurchaseVO.setCodeSerialNumber("0001");
            }
            receiptPurchaseVO.setDtlRespList(selectList);
        }
        //操作状态
        receiptPurchaseVO.setStateName(StringUtils.isNotEmpty(receiptPurchaseVO.getState())? InvoiceEnum.getValueByCode(receiptPurchaseVO.getState())!=null
                ?InvoiceEnum.getValueByCode(receiptPurchaseVO.getState()):receiptPurchaseVO.getState():null);
        return receiptPurchaseVO;
    }

    /**
     * 使用特定的格式化字符串
     * @param original
     * @param increment
     * @return
     */
    public static String formatNumber(String original, int increment) {
        int number = Integer.parseInt(original);
        number += increment;
        // 使用特定的格式化字符串，%04d意味着至少4位宽度，不足部分用前导零填充
        return String.format("%04d", number);
    }

    /**
     *
     * @param pushPurchaseVO 新增推送采购订单
     * @return
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW,rollbackFor = Exception.class)
    public boolean insertPushPurchase(ReceiptPurchaseVO pushPurchaseVO) {
        pushPurchaseVO.setThirdId(String.valueOf(pushPurchaseVO.getId()));
        pushPurchaseVO.setId(null);
        boolean flat=this.save(pushPurchaseVO);
        if(flat){
            String id=String.valueOf(pushPurchaseVO.getId());
            List<ReceiptPurchaseDetailVO> detailList=pushPurchaseVO.getDtlRespList();
            if (!CollectionUtils.isEmpty(detailList)){
                    List<ReceiptPurchaseDetail> purchaseDetailList = detailList.stream()
                            .map(tem -> {
                                ReceiptPurchaseDetail detail=new ReceiptPurchaseDetail();
                                BeanUtils.copyBeanProp(detail,tem);
                                detail.setParntId(String.valueOf(id));
                                return detail;
                            })
                            .collect(Collectors.toList());
                    //采购订单明细插入
                    iPushPurchaseDetailService.insertPushPurchaseSaveBatch(purchaseDetailList);
            }
        }
        return flat;
    }

}
