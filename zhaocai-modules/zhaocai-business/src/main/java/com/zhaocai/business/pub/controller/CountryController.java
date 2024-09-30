package com.zhaocai.business.pub.controller;

import com.zhaocai.business.pub.domain.Country;
import com.zhaocai.business.pub.service.ICountryService;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.core.web.controller.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 国家和地区档案
 *
 * @author WH
 * @date 2024-07-14
 */
@RestController
@RequestMapping("/country")
public class CountryController extends BaseController
{
    @Autowired
    private ICountryService countryService;

    /**
     * 查询国家和地区档案列表
     */
    //@RequiresPermissions("pub:country:list")
    @GetMapping("/list")
    public ResultData<List<Country>> list(Country country)
    {
        List<Country> list = countryService.selectCountryList(country);
        return ResultData.data(list);
    }
}
