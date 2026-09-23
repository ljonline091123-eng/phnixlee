package com.zhaocai.business.pub.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.manager.http.service.UnderlingSystemService;
import com.zhaocai.business.pub.service.ISysDictDataService;
import com.zhaocai.business.pub.vo.res.DictListVO;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dict")
@Api(value = "业务字典")
public class SysDictController extends BladeController {

    @Autowired
    private ISysDictDataService sysDictDataService;

    @Autowired
    private UnderlingSystemService underlingSystemService;

    /**
     * 获取业务字典
     */
    @GetMapping("/listDict")
    @ApiOperation(value = "获取业务字典")
    public ResultData<List<DictListVO>> listDict(@RequestParam String type) {
        List<DictListVO> list = sysDictDataService.listDictByType(type);
        return ResultData.data(list);
    }

    /**
     * 获取底层逻辑字典
     */
    @GetMapping("/listUnderlingDict")
    @ApiOperation(value = "获取底层逻辑字典")
    public ResultData<List<DictListVO>> listUnderlingDict(@RequestParam String type) {
        return ResultData.data(underlingSystemService.listDict(type));
    }
}
