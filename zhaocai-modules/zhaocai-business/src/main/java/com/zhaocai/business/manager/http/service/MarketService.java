package com.zhaocai.business.manager.http.service;

import com.alibaba.fastjson2.JSON;
import com.zhaocai.business.agreement.service.IMarketMaterialContractService;
//import com.zhaocai.business.manager.http.common.config.UnderlingPlatformUrlEnum;
import com.zhaocai.business.manager.http.dto.req.MarketMaterialListQuoteRequestDTO;
import com.zhaocai.business.manager.http.dto.req.MarketMaterialListRequestDTO;
import com.zhaocai.business.manager.http.dto.req.MarketQuotePriceRequestDTO;
import com.zhaocai.business.manager.http.dto.res.MarketQuotePriceResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ssy
 * @date 2024/9/20 10:58
 * @description 易料市集接口
 */
@Slf4j
@Service
public class MarketService {

    @Autowired
    private IMarketMaterialContractService marketMaterialContractService;

    public List<MarketQuotePriceResponseDTO> queryMarketQuotePrice(MarketQuotePriceRequestDTO requestDTO) {
//        List<MarketQuotePriceResponseDTO> responseDTO = UnderlingRestTemplateService.postForList(UnderlingPlatformUrlEnum.MARKET_QUOTE_PRICE,
//                MarketQuotePriceResponseDTO.class, requestDTO);

        List<MarketQuotePriceResponseDTO> responseDTO = new ArrayList<>();
        log.info("[查询易料市集清单最新价格] - 请求结果为:{}", JSON.toJSONString(responseDTO));
        return responseDTO;
    }

    public void pushMarketMaterialList(MarketMaterialListRequestDTO requestDTO){
//        UnderlingRestTemplateService.postForObject(UnderlingPlatformUrlEnum.MARKET_MATERIAL_LIST_PUSH,
//                MarketMaterialListRequestDTO.class, requestDTO);
        log.info("[推送采购清单数据] - 请求结果为:{}","success");
    }

    public void receiveMarketMaterialListQuote(MarketMaterialListQuoteRequestDTO requestDTO){
        log.info("[接收采购清单最终报价] - 开始接收:{}", JSON.toJSONString(requestDTO));

        //保存采购清单最终报价信息
        if(null != requestDTO){
            marketMaterialContractService.saveContract(requestDTO);
        }

        log.info("[接收采购清单最终报价] - 接收结束:{}", "success");
    }


}
