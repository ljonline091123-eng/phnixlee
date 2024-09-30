package com.zhaocai.business.receipt.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhaocai.business.common.utils.ValidateUtils;
import com.zhaocai.business.receipt.domain.ReceiptPurchaseDetail;
import com.zhaocai.business.receipt.domain.ReceiptPurchaseInvoiceDetail;
import com.zhaocai.business.receipt.mapper.ReceiptPurchaseMapper;
import com.zhaocai.business.receipt.service.*;
import com.zhaocai.business.receipt.vo.req.ReceiptPurchaseDetailVO;
import com.zhaocai.business.receipt.vo.req.ReceiptPurchaseInvoiceDetailVO;
import com.zhaocai.business.receipt.vo.req.ReceiptPurchaseInvoiceVO;
import com.zhaocai.business.receipt.vo.req.ReceiptPurchaseVO;
import com.zhaocai.business.receipt.vo.res.ReceiptThirdRequestVO;
import com.zhaocai.business.receipt.vo.res.query.ReceiptPurchaseInvoiceQueryVO;
import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.business.vendor.service.IVendorService;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.bean.BeanUtils;
import com.zhaocai.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zhaocai.business.receipt.mapper.ReceiptPurchaseInvoiceMapper;
import com.zhaocai.business.receipt.domain.ReceiptPurchaseInvoice;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * 发货单 Service业务层处理
 *
 * @author cff
 * @date 2024-09-13
 */
@Service
public class ReceiptPurchaseInvoiceServiceImpl extends ServiceImpl<ReceiptPurchaseInvoiceMapper,
        ReceiptPurchaseInvoice> implements IReceiptPurchaseInvoiceService {
    @Autowired
    private ReceiptPurchaseInvoiceMapper receiptPurchaseInvoiceMapper;

    @Autowired
    private IReceiptPurchaseInvoiceDetailService invoiceDetailService;

    @Autowired
    private ReceiptPurchaseMapper receiptPurchaseMapper;

    @Autowired
    private IReceiptPurchaseDetailService receiptPurchaseDetailService;

    @Autowired
    private IVendorService vendorService;


    /**
     * 采购订单列表
     * @param queryDTO
     * @return
     */
    @Override
    public PageResult<ReceiptPurchaseInvoiceVO> invoiceListPage(ReceiptPurchaseInvoiceQueryVO queryDTO) {
        queryDTO.setSupplierId(String.valueOf(getVendor(SecurityUtils.getUserId()).getId()));
        IPage<ReceiptPurchaseInvoiceVO> pages = baseMapper.selectListPge(queryDTO.toMybatisPage(),queryDTO);
        return new PageResult<>(pages);
    }

    private Vendor getVendor(Long userId){
        return vendorService.getByLoginUser(userId);
    }


    @Override
    public List<ReceiptPurchaseInvoiceVO> selectList(ReceiptPurchaseInvoiceVO pushPurchase) {
        List<ReceiptPurchaseInvoiceVO> list=baseMapper.selectList(pushPurchase);
        return list;
    }

    @Override
    public ReceiptPurchaseInvoiceVO detail(Long id) {
        ReceiptPurchaseInvoice purchaseInvoice = super.getById(id);
        ValidateUtils.isNullException(purchaseInvoice,"该发货单不存在");
        ReceiptPurchaseInvoiceDetailVO purchaseVO=new ReceiptPurchaseInvoiceDetailVO();
        purchaseVO.setParntId(String.valueOf(purchaseInvoice.getId()));
        List<ReceiptPurchaseInvoiceDetailVO> selectList=invoiceDetailService.selectList(purchaseVO);
        ReceiptPurchaseInvoiceVO receiptPurchaseVO=new ReceiptPurchaseInvoiceVO();
        BeanUtils.copyBeanProp(receiptPurchaseVO,purchaseInvoice);
        receiptPurchaseVO.setDtlRespList(selectList);
        return receiptPurchaseVO;
    }


    @Override
    public ReceiptPurchaseInvoiceVO getQrCodeInfo(Long id) {
        ReceiptPurchaseInvoice purchaseInvoice = super.getById(id);
        ValidateUtils.isNullException(purchaseInvoice,"该发货单不存在");
        ReceiptPurchaseInvoiceDetailVO purchaseVO=new ReceiptPurchaseInvoiceDetailVO();
        purchaseVO.setParntId(String.valueOf(purchaseInvoice.getId()));
        List<ReceiptPurchaseInvoiceDetailVO> selectList=invoiceDetailService.selectList(purchaseVO);
        List<ReceiptPurchaseInvoiceDetailVO> result = selectList.stream().map((tem -> {
                    ReceiptPurchaseInvoiceDetailVO receiptPurchaseVO=new ReceiptPurchaseInvoiceDetailVO();
                    ReceiptPurchaseDetail detail=receiptPurchaseDetailService.getById(tem.getDetailId());
                    BeanUtils.copyBeanProp(receiptPurchaseVO,tem);
                    receiptPurchaseVO.setSubjectDtlUniqueId(detail.getSubjectDtlUniqueId());
                    return receiptPurchaseVO;
        })).collect(Collectors.toList());
        ReceiptPurchaseInvoiceVO receiptPurchaseVO=new ReceiptPurchaseInvoiceVO();
        BeanUtils.copyBeanProp(receiptPurchaseVO,purchaseInvoice);
        receiptPurchaseVO.setDtlRespList(result);
        return receiptPurchaseVO;
    }


    /**
     *
     * @param purchaseInvoiceVO 新增发货单
     * @return
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW,rollbackFor = Exception.class)
    public boolean saveInvoice(ReceiptPurchaseInvoiceVO purchaseInvoiceVO) {
        purchaseInvoiceVO.setSupplierId(String.valueOf(getVendor(SecurityUtils.getUserId()).getId()));
        purchaseInvoiceVO.setSupplierName(String.valueOf(getVendor(SecurityUtils.getUserId()).getEnterpriseName()));
        purchaseInvoiceVO.setReceiptPurchaseId(purchaseInvoiceVO.getId());
        purchaseInvoiceVO.setId(null);
        boolean flat=this.save(purchaseInvoiceVO);
        if(flat){
            String id=String.valueOf(purchaseInvoiceVO.getId());
            List<ReceiptPurchaseInvoiceDetailVO> detailList=purchaseInvoiceVO.getDtlRespList();
            List<ReceiptPurchaseDetail> detailVOList=new ArrayList<>();
            if (!CollectionUtils.isEmpty(detailList)){
                List<ReceiptPurchaseInvoiceDetail> purchaseDetailList = detailList.stream()
                        .map(tem -> {
                            ReceiptPurchaseInvoiceDetail detail=new ReceiptPurchaseInvoiceDetail();
                            BeanUtils.copyBeanProp(detail,tem);
                            detail.setDetailId(String.valueOf(detail.getId()));
                            detail.setId(null);
                            detail.setParntId(String.valueOf(id));
                            detail.setTaxMoney(String.valueOf(detail.getUnitNoTax().multiply(detail.getTaxRate())));
                            ReceiptPurchaseDetail detailVO=new ReceiptPurchaseDetail();
                            detailVO.setId(Long.valueOf(detail.getDetailId()));
                            detailVO.setCanOrderNumber(tem.getResidueOrderNumber());
                            detailVOList.add(detailVO);
                            return detail;
                        })
                        .collect(Collectors.toList());
                //新增发货单详情
                invoiceDetailService.insertPurchaseInvoiceDetailSaveBatch(purchaseDetailList);
            }
            if (!CollectionUtils.isEmpty(detailVOList)){
                //修改采购订单明细
                receiptPurchaseDetailService.updatePushPurchaseBatch(detailVOList);
            }
            //修改采购订单状态设为已生成
            ReceiptPurchaseVO purchaseVO=new ReceiptPurchaseVO();
            purchaseVO.setId(purchaseInvoiceVO.getReceiptPurchaseId());
            Long count = detailList.stream().mapToLong(ReceiptPurchaseInvoiceDetailVO::getResidueOrderNumber).summaryStatistics().getSum();
            if(count==0){
                purchaseVO.setState("3");
            }else {
                purchaseVO.setState("2");
            }
            receiptPurchaseMapper.updatePushPurchase(purchaseVO);
        }
        return flat;
    }
}
