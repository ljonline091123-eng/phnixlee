package com.zhaocai.business.bidding.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.bidding.domain.BiddingListQuotation;
import com.zhaocai.business.bidding.mapper.BiddingListQuotationMapper;
import com.zhaocai.business.bidding.service.IBiddingListQuotationService;
import com.zhaocai.business.bidding.vo.res.VendorBiddingListQuotationListVO;
import com.zhaocai.business.bidding.vo.res.VendorBiddingListQuotationVO;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.common.enums.ProcurementPlanTypeEnum;
import com.zhaocai.business.manager.http.service.UnderlingSystemService;
import com.zhaocai.business.procurement.domain.ProcurementScheme;
import com.zhaocai.business.procurement.service.IProcurementSchemeService;
import com.zhaocai.business.pub.service.ISysDictDataService;
import com.zhaocai.common.core.utils.NumberUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 投标清单报价Service业务层处理
 *
 * @author WH
 * @date 2024-05-24
 */
@Service
public class BiddingListQuotationServiceImpl extends ServiceImpl<BiddingListQuotationMapper,BiddingListQuotation> implements IBiddingListQuotationService {

    @Autowired
    private UnderlingSystemService underlingSystemService;

    @Autowired
    private IProcurementSchemeService procurementSchemeService;

    @Autowired
    private ISysDictDataService sysDictDataService;

    /**
     * 获取供应商的报价清单
     */
    @Override
    public VendorBiddingListQuotationVO getVendorBiddingListQuotation(Long schemeId,Long splitId,Long vendorId) {
        ProcurementScheme procurementScheme = procurementSchemeService.getById(schemeId);

        List<VendorBiddingListQuotationListVO> list = baseMapper.selectVendorBiddingListQuotation(schemeId,splitId,vendorId);

        Map<String,String> rentModeMap = sysDictDataService.listDictMap(DictBizEnum.UNDERLING_RENT_MODE.getName());

        for (VendorBiddingListQuotationListVO listVO : list) {
            listVO.setSurplusCount(NumberUtil.subtract(listVO.getCount(),listVO.getUsedCount()));
            // 处理税额
            listVO.setTaxAmount(NumberUtil.subtract(listVO.getTaxPrice(),listVO.getNotTaxPrice()));
            listVO.setRentModeText(rentModeMap.get(listVO.getRentMode()));

            if (ProcurementPlanTypeEnum.isRent(procurementScheme.getProcurementPlanType())) {
                // 处理租赁单位
                if ("1".equals(listVO.getRentMode())) {
                    listVO.setRentalUnit("4");
                }
                if ("2".equals(listVO.getRentMode())) {
                    listVO.setRentalUnit("5");
                }
            }
        }

        String subjectMatterName = list.stream()
                .map(VendorBiddingListQuotationListVO::getSubjectMatterName)
                .distinct()
                .collect(Collectors.joining(","));

        String subjectMatterCode = list.stream()
                .map(VendorBiddingListQuotationListVO::getSubjectMatterCode)
                .distinct()
                .collect(Collectors.joining(","));

        VendorBiddingListQuotationVO listQuotationVO = new VendorBiddingListQuotationVO();
        listQuotationVO.setProcurementType(procurementScheme.getProcurementPlanType());
        listQuotationVO.setSubjectMatterName(subjectMatterName);
        listQuotationVO.setSubjectMatterCode(subjectMatterCode);
        listQuotationVO.setPriceType(procurementScheme.getPriceType());
        listQuotationVO.setVendorBiddingListQuotationList(list);
        return listQuotationVO;
    }

    @Override
    public List<BiddingListQuotation> listVendorBiddingListQuotation(Long schemeId, Long contractSplitId, Long vendorId) {
        return baseMapper.selectVendorBiddingListQuotationList(schemeId,contractSplitId,vendorId);
    }
}
