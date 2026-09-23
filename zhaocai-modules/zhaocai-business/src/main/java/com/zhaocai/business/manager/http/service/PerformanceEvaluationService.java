package com.zhaocai.business.manager.http.service;

import cn.hutool.core.collection.CollectionUtil;
import com.zhaocai.business.manager.http.common.config.RestTemplateUtils;
import com.zhaocai.business.manager.http.common.config.UnderlingPlatformUrlEnum;
import com.zhaocai.business.manager.http.dto.req.ContractListRequestDTO;
import com.zhaocai.business.manager.http.dto.req.PerformanceEvaluationRequestDTO;
import com.zhaocai.business.manager.http.dto.res.ContractListDTO;
import com.zhaocai.business.manager.http.dto.res.PerformanceEvaluationDTO;
import com.zhaocai.business.manager.http.dto.res.UnderlingResultData;
import com.zhaocai.business.manager.template.config.UnderlingPlatformConfig;
import com.zhaocai.business.procurement.service.IMinProjectService;
import com.zhaocai.business.vendor.domain.VendorPerformanceEvaluation;
import com.zhaocai.common.core.utils.NumberUtil;
import com.zhaocai.common.core.web.bean.thrid.ThridResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 履约评价
 *
 * @author chenming
 * @date 2024-07-12
 */
@Service
public class PerformanceEvaluationService {
    private static final Logger log = LoggerFactory.getLogger(PerformanceEvaluationService.class);

    @Autowired
    private UnderlingPlatformConfig underlingPlatformConfig;

    /**
     * 查看最小核算项目数
     *
     * @return
     */
    public String selectPrgAmount() {
//        UriComponents uriComponents = UriComponentsBuilder.fromUriString(underlingPlatformConfig.getBaseUrl() + UnderlingPlatformUrlEnum.GET_PRG_AMOUNT.getUrl())
//                .queryParam("authCode", underlingPlatformConfig.getAuthCode())
//                .build();
        UriComponents uriComponents = null;
        try {
            UnderlingResultData<List<LinkedHashMap>> response = RestTemplateUtils.getForObject(uriComponents.toString(), UnderlingResultData.class);
            //如果成功返回
            if (response.getCode() == ThridResultCode.SUCCESS.getCode()) {
                return String.valueOf(response.getData());
            }
        } catch (Exception ex) {
//            log.error("第三方用户列表接口获取失败:{}", ex.getMessage());
        }
        return null;
    }

    /**
     * 获取履约评价
     *
     * @param vendorId
     * @return
     */
    public List<VendorPerformanceEvaluation> listPerformanceEvaluation(Long vendorId) {
        PerformanceEvaluationRequestDTO requestDTO = new PerformanceEvaluationRequestDTO();
        if (NumberUtil.isNotNullAndZero(vendorId)) {
            requestDTO.setPartbId(vendorId.toString());
        }

//        List<PerformanceEvaluationDTO> list = UnderlingRestTemplateService.listForObject(UnderlingPlatformUrlEnum.VENDOR_EVALUATE_SUMMARIZE,
//                PerformanceEvaluationDTO.class,requestDTO);
        List<PerformanceEvaluationDTO> list = new ArrayList<>();

        if (CollectionUtil.isNotEmpty(list)) {
            return list.stream()
                    .map(dto -> {
                        VendorPerformanceEvaluation evaluation = new VendorPerformanceEvaluation();
                        evaluation.setAgreementCode(dto.getConCode());
                        evaluation.setAgreementName(dto.getConName());
                        evaluation.setPartyAId(dto.getPartaId());
                        evaluation.setPartyAName(dto.getPartaName());
                        evaluation.setPartyBId(dto.getPartaId());
                        evaluation.setPartyBName(dto.getPartbName());
                        evaluation.setProjectId(dto.getProjectId());
                        evaluation.setExcellentNum(dto.getExcellentNum());
                        evaluation.setGoodNum(dto.getGoodNum());
                        evaluation.setQualifiedNum(dto.getQualifiedNum());
                        evaluation.setBadNum(dto.getBadNum());

                        return evaluation;
                    })
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }


    /**
     * 获取合同列表
     *
     * @param vendorId
     * @return
     */
    public List<ContractListDTO> listContractList(Long vendorId, Integer expenditureBusinessType) {
        ContractListRequestDTO requestDTO = new ContractListRequestDTO(vendorId, expenditureBusinessType);

//        return UnderlingRestTemplateService.listForObject(UnderlingPlatformUrlEnum.CONTRACT_LIST,
//                ContractListDTO.class,requestDTO);
        return new ArrayList<>();
    }

}
