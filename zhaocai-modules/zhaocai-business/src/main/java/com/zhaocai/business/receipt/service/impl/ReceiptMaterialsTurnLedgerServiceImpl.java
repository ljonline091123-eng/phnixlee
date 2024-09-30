package com.zhaocai.business.receipt.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.common.utils.ValidateUtils;
import com.zhaocai.business.manager.http.service.UnderlingSystemService;
import com.zhaocai.business.receipt.domain.ReceiptMaterialsTurnLedger;
import com.zhaocai.business.receipt.enums.OperationalStateEnum;
import com.zhaocai.business.receipt.mapper.ReceiptMaterialsTurnLedgerMapper;
import com.zhaocai.business.receipt.service.IReceiptMaterialsTurnLedgerDtlService;
import com.zhaocai.business.receipt.service.IReceiptMaterialsTurnLedgerService;
import com.zhaocai.business.receipt.vo.req.ReceiptMaterialsTurnLedgerDtlVO;
import com.zhaocai.business.receipt.vo.res.query.ReceiptMaterialsTurnLedgerQueryVO;
import com.zhaocai.business.receipt.vo.req.ReceiptMaterialsTurnLedgerVO;
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
 * 租赁周材台账 Service业务层处理
 *
 * @author cff
 * @date 2024-09-10
 */
@Service
public class ReceiptMaterialsTurnLedgerServiceImpl extends
        ServiceImpl<ReceiptMaterialsTurnLedgerMapper, ReceiptMaterialsTurnLedger> implements IReceiptMaterialsTurnLedgerService {
    @Autowired
    private ReceiptMaterialsTurnLedgerMapper receiptMaterialsTurnLedgerMapper;


    @Autowired
    private IReceiptMaterialsTurnLedgerDtlService turnLedgerDtlService;

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
    public PageResult<ReceiptMaterialsTurnLedgerVO> materialsTurnLedgerListPage(ReceiptMaterialsTurnLedgerQueryVO queryDTO) {
        queryDTO.setSupplierId(String.valueOf(getVendor(SecurityUtils.getUserId()).getId()));
        IPage<ReceiptMaterialsTurnLedgerVO> pages = baseMapper.selectListPge(queryDTO.toMybatisPage(),queryDTO);
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
    public ReceiptMaterialsTurnLedgerVO detail(Long id) {
        ReceiptMaterialsTurnLedger materialsTurnLedger = super.getById(id);
        ValidateUtils.isNullException(materialsTurnLedger,"该租赁周材台账不存在");
        ReceiptMaterialsTurnLedgerDtlVO materialsTurnLedgerDtlVO=new ReceiptMaterialsTurnLedgerDtlVO();
        materialsTurnLedgerDtlVO.setParntId(String.valueOf(materialsTurnLedger.getId()));
        List<ReceiptMaterialsTurnLedgerDtlVO> selectList=turnLedgerDtlService.selectList(materialsTurnLedgerDtlVO);
        ReceiptMaterialsTurnLedgerVO turnLedgerVO=new ReceiptMaterialsTurnLedgerVO();
        BeanUtils.copyBeanProp(turnLedgerVO,materialsTurnLedger);
        //计租方式
        turnLedgerVO.setRentType(StringUtils.isNotEmpty(turnLedgerVO.getRentType())?
                underlingSystemService.listDictMap(DictBizEnum.UNDERLING_RENT_TYPE.getName()).get(turnLedgerVO.getRentType()):null);
        //结算状态
        turnLedgerVO.setSettleStatus(StringUtils.isNotEmpty(turnLedgerVO.getSettleStatus())?
                underlingSystemService.listDictMap(DictBizEnum.UNDERLING_SETTLE_STATUS.getName()).get(turnLedgerVO.getSettleStatus()):null);
        //操作状态
        turnLedgerVO.setStateName(StringUtils.isNotEmpty(turnLedgerVO.getState())? OperationalStateEnum.getValueByCode(turnLedgerVO.getState())!=null
                ?OperationalStateEnum.getValueByCode(turnLedgerVO.getState()):turnLedgerVO.getState():null);
        if(!CollectionUtils.isEmpty(selectList)) {
            selectList.stream().map(tem -> {
                //台班类型
                tem.setMachineType(StringUtils.isNotEmpty(tem.getMachineType())?
                        underlingSystemService.listDictMap(DictBizEnum.UNDERLING_MTR_MACH_TYPE.getName()).get(tem.getMachineType()):null);
                return tem;
            }).collect(Collectors.toList());
            turnLedgerVO.setDtlList(selectList);
        }
        return turnLedgerVO;
    }

    /**
     *
     * @param turnLedgerVO 新增设备租赁台账
     * @return
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW,rollbackFor = Exception.class)
    public boolean insertMaterialsTurnLedger(ReceiptMaterialsTurnLedgerVO turnLedgerVO) {
        turnLedgerVO.setThirdId(String.valueOf(turnLedgerVO.getId()));
        turnLedgerVO.setId(null);
        boolean flat=this.save(turnLedgerVO);
        if(flat){
            String id=String.valueOf(turnLedgerVO.getId());
            List<ReceiptMaterialsTurnLedgerDtlVO> detail=turnLedgerVO.getDtlList();
            if (!CollectionUtils.isEmpty(detail)){
                //设备租赁台账明细插入
                turnLedgerDtlService.insertReceiptMaterialsDeviceLedgerDtlSaveBatch(detail,id);
            }
        }
        return flat;
    }


    /**
     * 修改租赁周材台账状态
     *
     * @param turnLedgerVO 修改租赁周材台账状态
     * @return 结果
     */
    @Override
    public boolean modifyMaterialsTurnLedgerState(ReceiptMaterialsTurnLedgerVO turnLedgerVO) {
        return baseMapper.updateReceiptMaterialsTurnLedger(turnLedgerVO);
    }
}
