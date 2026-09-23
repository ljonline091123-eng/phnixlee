package com.zhaocai.business.receipt.controller;

import com.zhaocai.business.manager.http.dto.req.MarketMaterialListQuoteRequestDTO;
import com.zhaocai.business.manager.http.dto.req.MarketMaterialListRequestDTO;
import com.zhaocai.business.manager.http.dto.req.MarketQuotePriceRequestDTO;
import com.zhaocai.business.manager.http.dto.res.MarketQuotePriceResponseDTO;
import com.zhaocai.business.manager.http.service.MarketService;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author ssy
 * @date 2024/9/20 10:48
 */
@Api(value = "易料市集接口")
@RestController
@RequestMapping("/market")
public class MarketController {

    @Autowired
    private MarketService marketService;

    /**
     * 查询易料市集清单最新价格
     */
    @PostMapping(value = "/queryMarketQuotePrice")
    @ApiOperation(value = "查询易料市集清单最新价格")
    public ResultData<List<MarketQuotePriceResponseDTO>> queryMarketQuotePrice(@RequestBody MarketQuotePriceRequestDTO marketQuotePriceRequestDTO) {
        return ResultData.data(marketService.queryMarketQuotePrice(marketQuotePriceRequestDTO));
    }

    /**
     * 推送采购清单数据
     */
    @PostMapping(value = "/pushMarketMaterialList")
    @ApiOperation(value = "推送采购清单数据")
    public ResultData<Boolean> pushMarketMaterialList(@RequestBody MarketMaterialListRequestDTO marketMaterialListRequestDTO) {
        marketService.pushMarketMaterialList(marketMaterialListRequestDTO);
        return ResultData.success();
    }

    /**
     * 接收采购清单最终报价
     */
    @PostMapping(value = "/receiveMarketMaterialListQuote")
    @ApiOperation(value = "接收采购清单最终报价")
    public ResultData<Boolean> receiveMarketMaterialListQuote(@RequestBody MarketMaterialListQuoteRequestDTO quoteRequestDTO) {
        marketService.receiveMarketMaterialListQuote(quoteRequestDTO);
        return ResultData.success();
    }

}
