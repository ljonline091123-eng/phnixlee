package com.zhaocai.business.agreement.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.agreement.domain.AgreementMaterialsList;
import com.zhaocai.business.agreement.mapper.AgreementMaterialsListMapper;
import com.zhaocai.business.agreement.service.IAgreementMaterialsListService;
import com.zhaocai.business.agreement.vo.res.AgreementMaterialsListVO;
import com.zhaocai.business.agreement.vo.res.AgreementUnderlingMaterialsVO;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.common.utils.AmountCalUtil;
import com.zhaocai.business.manager.http.service.UnderlingSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 合同物料清单Service业务层处理
 *
 * @author chenming
 * @date 2024-06-19
 */
@Service
public class AgreementMaterialsListServiceImpl extends ServiceImpl<AgreementMaterialsListMapper, AgreementMaterialsList> implements IAgreementMaterialsListService {

    @Autowired
    private UnderlingSystemService underlingSystemService;

    @Override
    public void saveAgreementMaterialsList(List<AgreementMaterialsList> agreementMaterialsLists, Long agreementId) {
        if (CollectionUtil.isNotEmpty(agreementMaterialsLists)) {
            for (AgreementMaterialsList agreementMaterialsList : agreementMaterialsLists) {
                agreementMaterialsList.setAgreementId(agreementId);
            }
            super.saveBatch(agreementMaterialsLists);
        }
    }

    @Override
    public List<AgreementMaterialsListVO> listAgreementMaterials(Long agreementId) {
        List<AgreementMaterialsListVO> resultList = baseMapper.selectAgreementMaterialsList(agreementId);

        Map<String,String> rentalTypeMap = underlingSystemService.listDictMap(DictBizEnum.UNDERLING_RENT_MODE.getName());
        Map<String,String> rentalUnitMap = underlingSystemService.listDictMap(DictBizEnum.UNDERLING_RENT_UNIT.getName());
        for (AgreementMaterialsListVO materials : resultList) {
            materials.setSignTaxAmount(AmountCalUtil.calTaxAmount(materials.getSignAmountInclTax(),materials.getSignAmountExclTax()));
            materials.setTaxAmount(AmountCalUtil.calTaxAmount(materials.getTaxPrice(),materials.getNotTaxPrice()));
            materials.setRentModeText(rentalTypeMap.get(materials.getRentMode()));
            materials.setRentalUnitText(rentalUnitMap.get(materials.getRentalUnit()));
        }

        return resultList;
    }

    @Override
    public List<AgreementUnderlingMaterialsVO> listUnderlingMaterials(Long agreementId) {
        return baseMapper.selectUnderlingMaterialsList(agreementId);
    }

    @Override
    public void updateAgreementMaterialsList(List<AgreementMaterialsList> agreementMaterialsLists, Long agreementId) {
        baseMapper.deleteByAgreementId(agreementId);

        this.saveAgreementMaterialsList(agreementMaterialsLists,agreementId);
    }

    @Override
    public List<AgreementMaterialsList> listByContractSplitId(Long contractSplitId) {
        return baseMapper.selectMaterialsByContractSplitId(contractSplitId);
    }

    @Override
    public List<AgreementMaterialsList> listByAgreementId(Long agreementId) {
        return this.list(new LambdaQueryWrapper<AgreementMaterialsList>()
                .eq(AgreementMaterialsList::getAgreementId,agreementId));
    }

    @Override
    public List<AgreementMaterialsListVO> listAgreementMaterialsByMarket(Long id) {
        List<AgreementMaterialsListVO> resultList = baseMapper.listAgreementMaterialsByMarket(id);

        Map<String,String> rentalTypeMap = underlingSystemService.listDictMap(DictBizEnum.UNDERLING_RENT_MODE.getName());
        Map<String,String> rentalUnitMap = underlingSystemService.listDictMap(DictBizEnum.UNDERLING_RENT_UNIT.getName());
        for (AgreementMaterialsListVO materials : resultList) {
            BigDecimal taxUnitPrice = materials.getNotTaxUnitPrice().multiply(materials.getTaxRate().divide(BigDecimal.valueOf(100))).add(materials.getNotTaxUnitPrice());
            materials.setTaxUnitPrice(taxUnitPrice);
            materials.setSignUnitPriceInclTax(taxUnitPrice);
            // 含税金额 = 含税单价 * 数量
            BigDecimal taxPrice = AmountCalUtil.calTotalAmountInclTax(materials.getCount(), materials.getTaxUnitPrice());
            materials.setTaxPrice(taxPrice);
            materials.setSignAmountInclTax(taxPrice);
            // 不含税金额 = 含税金额 / (1 * 税率%)
            BigDecimal notTaxPrice = AmountCalUtil.calTotalAmountExclTax(taxPrice, materials.getTaxRate());
            materials.setNotTaxPrice(notTaxPrice);
            materials.setSignAmountExclTax(notTaxPrice);
            materials.setSignTaxAmount(AmountCalUtil.calTaxAmount(materials.getSignAmountInclTax(),materials.getSignAmountExclTax()));
            materials.setTaxAmount(AmountCalUtil.calTaxAmount(materials.getTaxPrice(),materials.getNotTaxPrice()));
            materials.setRentModeText(rentalTypeMap.get(materials.getRentMode()));
            materials.setRentalUnitText(rentalUnitMap.get(materials.getRentalUnit()));
        }

        return resultList;
    }
}
