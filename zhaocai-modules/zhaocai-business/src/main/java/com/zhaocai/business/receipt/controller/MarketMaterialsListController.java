package com.zhaocai.business.receipt.controller;


import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.receipt.service.IMarketMaterialsListService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 易料市集物料清单 前端控制器
 * </p>
 *
 * @author susiyuan
 * @since 2024-09-21
 */
@RestController
@RequestMapping("/marketMaterialsList")
@Api(value = "易料市集物料清单信息", tags = "易料市集物料清单信息接口")
public class MarketMaterialsListController extends BladeController {

    @Autowired
    private IMarketMaterialsListService marketMaterialsListService;



}

