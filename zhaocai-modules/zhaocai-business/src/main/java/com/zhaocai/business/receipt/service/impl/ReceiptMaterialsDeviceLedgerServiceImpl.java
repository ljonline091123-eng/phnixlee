package com.zhaocai.business.receipt.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.common.utils.ValidateUtils;
import com.zhaocai.business.manager.http.service.UnderlingSystemService;
import com.zhaocai.business.receipt.domain.ReceiptMaterialsDeviceLedger;
import com.zhaocai.business.receipt.enums.OperationalStateEnum;
import com.zhaocai.business.receipt.mapper.ReceiptMaterialsDeviceLedgerMapper;
import com.zhaocai.business.receipt.service.IReceiptMaterialsDeviceLedgerDtlService;
import com.zhaocai.business.receipt.service.IReceiptMaterialsDeviceLedgerService;
import com.zhaocai.business.receipt.vo.req.ReceiptMaterialsDeviceLedgerDtlVO;
import com.zhaocai.business.receipt.vo.res.query.ReceiptMaterialsDeviceLedgerQueryVO;
import com.zhaocai.business.receipt.vo.req.ReceiptMaterialsDeviceLedgerVO;
import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.business.vendor.service.IVendorService;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.bean.BeanUtils;
import com.zhaocai.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 设备租赁台账 Service业务层处理
 *
 * @author cff
 * @date 2024-09-10
 */
@Service
public class ReceiptMaterialsDeviceLedgerServiceImpl extends ServiceImpl<ReceiptMaterialsDeviceLedgerMapper,
        ReceiptMaterialsDeviceLedger> implements IReceiptMaterialsDeviceLedgerService {
    @Autowired
    private ReceiptMaterialsDeviceLedgerMapper receiptMaterialsDeviceLedgerMapper;

    @Autowired
    private IReceiptMaterialsDeviceLedgerDtlService ledgerDtlService;

    @Autowired
    private UnderlingSystemService underlingSystemService;

    @Autowired
    private IVendorService vendorService;
    /**
     * 采购订单列表
     * @param queryDTO
     * @return
     */
    @Override
    public PageResult<ReceiptMaterialsDeviceLedgerVO> materialsDeviceLedgerListPage(ReceiptMaterialsDeviceLedgerQueryVO queryDTO) {
        queryDTO.setSupplierId(String.valueOf(getVendor(SecurityUtils.getUserId()).getId()));
        IPage<ReceiptMaterialsDeviceLedgerVO> pages = baseMapper.selectListPge(queryDTO.toMybatisPage(),queryDTO);
        if(!CollectionUtils.isEmpty(pages.getRecords())) {
            pages.getRecords().stream().map(tem -> {
                //操作状态
                tem.setStateName(StringUtils.isNotEmpty(tem.getState())?OperationalStateEnum.getValueByCode(tem.getState())!=null
                        ?OperationalStateEnum.getValueByCode(tem.getState()):tem.getState():null);
                return tem;
            }).collect(Collectors.toList());
        }
        return new PageResult<>(pages);
    }

    private Vendor getVendor(Long userId){
        return vendorService.getByLoginUser(userId);
    }

    /**
     * 根据id查询设备租赁台账详细信息
     * @param id
     * @return
     */
    @Override
    public ReceiptMaterialsDeviceLedgerVO detail(Long id) {
        ReceiptMaterialsDeviceLedger materialsDeviceLedger = super.getById(id);
        ValidateUtils.isNullException(materialsDeviceLedger,"该设备租赁台账不存在");
        ReceiptMaterialsDeviceLedgerDtlVO materialsDeviceLedgerDtlVO=new ReceiptMaterialsDeviceLedgerDtlVO();
        materialsDeviceLedgerDtlVO.setParntId(String.valueOf(materialsDeviceLedger.getId()));
        List<ReceiptMaterialsDeviceLedgerDtlVO> selectList=ledgerDtlService.selectList(materialsDeviceLedgerDtlVO);
        ReceiptMaterialsDeviceLedgerVO deviceLedgerVO=new ReceiptMaterialsDeviceLedgerVO();
        BeanUtils.copyBeanProp(deviceLedgerVO,materialsDeviceLedger);
        //计租方式
        deviceLedgerVO.setRentType(StringUtils.isNotEmpty(deviceLedgerVO.getRentType())?
                underlingSystemService.listDictMap(DictBizEnum.UNDERLING_RENT_TYPE.getName()).get(deviceLedgerVO.getRentType()):null);
        //结算状态
        deviceLedgerVO.setSettleStatus(StringUtils.isNotEmpty(deviceLedgerVO.getSettleStatus())?
                underlingSystemService.listDictMap(DictBizEnum.UNDERLING_SETTLE_STATUS.getName()).get(deviceLedgerVO.getSettleStatus()):null);
        //操作状态
        deviceLedgerVO.setStateName(StringUtils.isNotEmpty(deviceLedgerVO.getState())?OperationalStateEnum.getValueByCode(deviceLedgerVO.getState())!=null
                ?OperationalStateEnum.getValueByCode(deviceLedgerVO.getState()):deviceLedgerVO.getState():null);
        if(!CollectionUtils.isEmpty(selectList)) {
            selectList.stream().map(tem -> {
                        //台班类型
                        tem.setMachineType(StringUtils.isNotEmpty(tem.getMachineType())?
                        underlingSystemService.listDictMap(DictBizEnum.UNDERLING_MTR_MACH_TYPE.getName()).get(tem.getMachineType()):null);
                        return tem;
            }).collect(Collectors.toList());
            deviceLedgerVO.setDtlList(selectList);
        }
        return deviceLedgerVO;
    }



    /**
     *
     * @param deviceLedgerVO 新增设备租赁台账
     * @return
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW,rollbackFor = Exception.class)
    public boolean insertMaterialsDeviceLedger(ReceiptMaterialsDeviceLedgerVO deviceLedgerVO) {
        deviceLedgerVO.setThirdId(String.valueOf(deviceLedgerVO.getId()));
        deviceLedgerVO.setId(null);
        boolean flat=this.save(deviceLedgerVO);
        if(flat){
            String id=String.valueOf(deviceLedgerVO.getId());
            List<ReceiptMaterialsDeviceLedgerDtlVO> detail=deviceLedgerVO.getDtlList();
            if (!CollectionUtils.isEmpty(detail)){
                //设备租赁台账明细插入
                ledgerDtlService.insertReceiptMaterialsDeviceLedgerDtlSaveBatch(detail,id);
            }
        }
        return flat;
    }

    /**
     * 修改设备租赁台账状态
     *
     * @param pushPurchaseVO 修改设备租赁台账状态
     * @return 结果
     */
    @Override
    public boolean modifyDeviceLedgerState(ReceiptMaterialsDeviceLedgerVO pushPurchaseVO) {
        return baseMapper.updateReceiptMaterialsDeviceLedger(pushPurchaseVO);
    }

}
