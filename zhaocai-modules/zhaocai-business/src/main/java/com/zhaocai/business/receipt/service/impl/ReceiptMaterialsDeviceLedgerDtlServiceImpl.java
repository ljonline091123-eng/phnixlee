package com.zhaocai.business.receipt.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.receipt.domain.ReceiptMaterialsDeviceLedgerDtl;
import com.zhaocai.business.receipt.domain.ReceiptMaterialsDeviceLedgerDtlDtl;
import com.zhaocai.business.receipt.mapper.ReceiptMaterialsDeviceLedgerDtlMapper;
import com.zhaocai.business.receipt.service.IReceiptMaterialsDeviceLedgerDtlDtlService;
import com.zhaocai.business.receipt.service.IReceiptMaterialsDeviceLedgerDtlService;
import com.zhaocai.business.receipt.vo.req.ReceiptMaterialsDeviceLedgerDtlVO;
import com.zhaocai.common.core.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 设备租赁台账详情 Service业务层处理
 *
 * @author cff
 * @date 2024-09-10
 */
@Service
public class ReceiptMaterialsDeviceLedgerDtlServiceImpl extends ServiceImpl<ReceiptMaterialsDeviceLedgerDtlMapper,
        ReceiptMaterialsDeviceLedgerDtl> implements IReceiptMaterialsDeviceLedgerDtlService {
    @Autowired
    private ReceiptMaterialsDeviceLedgerDtlMapper receiptMaterialsDeviceLedgerDtlMapper;

    @Autowired
    private IReceiptMaterialsDeviceLedgerDtlDtlService ledgerDtlDtlService;


    @Override
    public List<ReceiptMaterialsDeviceLedgerDtlVO> selectList(ReceiptMaterialsDeviceLedgerDtlVO deviceLedgerDtlVO) {
        List<ReceiptMaterialsDeviceLedgerDtlVO> list=baseMapper.selectList(deviceLedgerDtlVO);
        return list;
    }

    @Override
    public boolean insertReceiptMaterialsDeviceLedgerDtlSaveBatch(List<ReceiptMaterialsDeviceLedgerDtlVO> ledgerDtlVOList, String id) {
        if (!CollectionUtils.isEmpty(ledgerDtlVOList)) {
            List<ReceiptMaterialsDeviceLedgerDtlDtl> dtlDtlList=new ArrayList<>();
            //设备租赁台账详情列表
            ledgerDtlVOList.stream().map(tem -> {
                        ReceiptMaterialsDeviceLedgerDtl ledgerDtl=new ReceiptMaterialsDeviceLedgerDtl();
                        BeanUtils.copyBeanProp(ledgerDtl,tem);
                        ledgerDtl.setParntId(String.valueOf(id));
                        ledgerDtl.setThirdId(String.valueOf(tem.getId()));
                        ledgerDtl.setId(null);
                        this.save(ledgerDtl);
                        if (!CollectionUtils.isEmpty(tem.getDtlDtlList())){
                            //设备租赁台账详情的明细列表
                            tem.getDtlDtlList().stream().map(tem1 -> {
                                ReceiptMaterialsDeviceLedgerDtlDtl ledgerDtlDtl=new ReceiptMaterialsDeviceLedgerDtlDtl();
                                BeanUtils.copyBeanProp(ledgerDtlDtl,tem1);
                                ledgerDtlDtl.setParntId(String.valueOf(ledgerDtl.getId()));
                                ledgerDtlDtl.setThirdId(String.valueOf(tem1.getId()));
                                ledgerDtlDtl.setId(null);
                                dtlDtlList.add(ledgerDtlDtl);
                                return ledgerDtlDtl;
                            }).collect(Collectors.toList());
                        }
                        return ledgerDtl;
            }).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(dtlDtlList)) {
                //设备租赁台账详情的明细列表
                ledgerDtlDtlService.insertReceiptMaterialsDeviceLedgerDtlDtlSaveBatch(dtlDtlList);
            }
        }
        return true;
    }
}
