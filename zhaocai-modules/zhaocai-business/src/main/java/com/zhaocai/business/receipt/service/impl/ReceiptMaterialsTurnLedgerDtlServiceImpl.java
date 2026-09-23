package com.zhaocai.business.receipt.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.receipt.domain.ReceiptMaterialsTurnLedgerDtl;
import com.zhaocai.business.receipt.domain.ReceiptMaterialsTurnLedgerDtlDtl;
import com.zhaocai.business.receipt.mapper.ReceiptMaterialsTurnLedgerDtlMapper;
import com.zhaocai.business.receipt.service.IReceiptMaterialsTurnLedgerDtlDtlService;
import com.zhaocai.business.receipt.service.IReceiptMaterialsTurnLedgerDtlService;
import com.zhaocai.business.receipt.vo.req.ReceiptMaterialsTurnLedgerDtlVO;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 周材租赁台账详情 Service业务层处理
 *
 * @author cff
 * @date 2024-09-10
 */
@Service
public class ReceiptMaterialsTurnLedgerDtlServiceImpl extends ServiceImpl<ReceiptMaterialsTurnLedgerDtlMapper,
        ReceiptMaterialsTurnLedgerDtl> implements IReceiptMaterialsTurnLedgerDtlService {
    @Autowired
    private ReceiptMaterialsTurnLedgerDtlMapper receiptMaterialsTurnLedgerDtlMapper;

    @Autowired
    private IReceiptMaterialsTurnLedgerDtlDtlService ledgerDtlDtlService;

    @Override
    public List<ReceiptMaterialsTurnLedgerDtlVO> selectList(ReceiptMaterialsTurnLedgerDtlVO turnLedgerDtlVO) {
        List<ReceiptMaterialsTurnLedgerDtlVO> list=baseMapper.selectList(turnLedgerDtlVO);
        return list;
    }

    @Override
    public boolean insertReceiptMaterialsDeviceLedgerDtlSaveBatch(List<ReceiptMaterialsTurnLedgerDtlVO> ledgerDtlVOList, String id) {
        if (!CollectionUtils.isEmpty(ledgerDtlVOList)) {
            List<ReceiptMaterialsTurnLedgerDtlDtl> dtlDtlList=new ArrayList<>();
            //设备租赁台账详情列表
            ledgerDtlVOList.stream().map(tem -> {
                ReceiptMaterialsTurnLedgerDtl turnLedgerDtl=new ReceiptMaterialsTurnLedgerDtl();
                BeanUtils.copyBeanProp(turnLedgerDtl,tem);
                turnLedgerDtl.setParntId(String.valueOf(id));
                turnLedgerDtl.setThirdId(String.valueOf(tem.getId()));
                turnLedgerDtl.setId(null);
                this.save(turnLedgerDtl);
                if (!CollectionUtils.isEmpty(tem.getDtlDtlList())){
                    //设备租赁台账详情的明细列表
                    tem.getDtlDtlList().stream().map(tem1 -> {
                        ReceiptMaterialsTurnLedgerDtlDtl ledgerDtlDtl=new ReceiptMaterialsTurnLedgerDtlDtl();
                        BeanUtils.copyBeanProp(ledgerDtlDtl,tem1);
                        ledgerDtlDtl.setParntId(String.valueOf(turnLedgerDtl.getId()));
                        ledgerDtlDtl.setThirdId(String.valueOf(tem1.getId()));
                        ledgerDtlDtl.setId(null);
                        dtlDtlList.add(ledgerDtlDtl);
                        return ledgerDtlDtl;
                    }).collect(Collectors.toList());
                }
                return turnLedgerDtl;
            }).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(dtlDtlList)) {
                //设备租赁台账详情的明细列表
                ledgerDtlDtlService.insertReceiptMaterialsTurnLedgerDtlDtlSaveBatch(dtlDtlList);
            }
        }
        return true;
    }
}
