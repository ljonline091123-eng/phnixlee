package com.zhaocai.business.vendor.controller;

import com.alibaba.csp.sentinel.util.StringUtil;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.procurement.vo.res.MinProjectDetailVO;
import com.zhaocai.business.pub.service.ISystemUserService;
import com.zhaocai.business.utils.KeyUtils;
import com.zhaocai.business.vendor.domain.TbVendorEvaluate;
import com.zhaocai.business.vendor.service.ITbVendorEvaluateService;
import com.zhaocai.business.vendor.vo.req.TbVendorEvaluateQueryVo;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.system.api.domain.SysUser;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * 供应商评价Controller
 *
 * @author xw
 * @date 2025-10-11
 */
@RestController
@RequestMapping("/vendor/evaluate")
public class TbVendorEvaluateController extends BladeController {

    @Autowired
    private ITbVendorEvaluateService tbVendorEvaluateService;

    @Autowired
    private ISystemUserService userService;

/**
 * 查询供应商评价列表
 */
//@PreAuthorize("@ss.hasPermi('modelname:tbVendorEvaluate:list')")
    @GetMapping("/list")
    public ResultData<PageResult<TbVendorEvaluate>> list(TbVendorEvaluateQueryVo queryVO) {
        PageResult<TbVendorEvaluate> list = tbVendorEvaluateService.listPage(queryVO);
        return ResultData.data(list);
    }

    /**
     * 导出供应商评价列表
     */
    //@PreAuthorize("@ss.hasPermi('modelname:tbVendorEvaluate:export')")
    @GetMapping("/export")
    public void export(HttpServletResponse response, TbVendorEvaluate tbVendorEvaluate) {

        /*public void export(HttpServletResponse response, ExpertScoreDetail expertScoreDetail)
    {
        List<ExpertScoreDetail> list = expertScoreDetailService.selectExpertScoreDetailList(expertScoreDetail);
        ExcelUtil<ExpertScoreDetail> util = new ExcelUtil<ExpertScoreDetail>(ExpertScoreDetail.class);
        util.exportExcel(response, list, "专家评分明细数据");
    }*/

    }

    /**
     * 获取供应商评价详细信息
     */
    //@PreAuthorize("@ss.hasPermi('modelname:tbVendorEvaluate:query')")
    @GetMapping(value = "/{id}")
    public ResultData<TbVendorEvaluate> getInfo(@PathVariable("id") Long id) {

        return ResultData.data(tbVendorEvaluateService.getById(id));
    }

    @GetMapping("/getById")
    public ResultData<TbVendorEvaluate> getById(@RequestParam Long id) {
        TbVendorEvaluate bean = tbVendorEvaluateService.getById(id);
        return ResultData.data(bean);
    }


    @PostMapping("add")
    public ResultData<TbVendorEvaluate> add() {
        TbVendorEvaluate bean = new TbVendorEvaluate();
        bean.setId(KeyUtils.generateId());
        bean.setEvaluateStatus("0");
        return ResultData.data(bean);
    }

    @PostMapping("/save")
    public ResultData<Boolean> save(@RequestBody TbVendorEvaluate tbVendorEvaluate) {
        if (tbVendorEvaluate.getCreateId() == null) {
            if(tbVendorEvaluate.getId() == null){
                tbVendorEvaluate.setId(KeyUtils.generateId());
            }
            tbVendorEvaluate.setCreateTime(DateUtils.getNowDate());
            tbVendorEvaluate.setCreateId(SecurityUtils.getUserId());
            tbVendorEvaluate.setCreateBy(SecurityUtils.getUsername());
        }
        if(StringUtil.isEmpty(tbVendorEvaluate.getEvaluateCode())){
            LocalDateTime now = LocalDateTime.now(); // 获取当前时间
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss"); // 设置时间格式
            String formattedTime = now.format(formatter); // 格式化时间
            tbVendorEvaluate.setEvaluateCode(formattedTime);
        }
        tbVendorEvaluate.setEvaluateStatus("0");
        tbVendorEvaluate.setUpdateTime(DateUtils.getNowDate());
        tbVendorEvaluate.setUpdateId(SecurityUtils.getUserId());
        SysUser user = userService.getUserById(SecurityUtils.getUserId());
        if(user != null){
            tbVendorEvaluate.setUpdateBy(user.getNickName());
        }else{
            tbVendorEvaluate.setUpdateBy(SecurityUtils.getUsername());
        }
        if(tbVendorEvaluate.getEvaluateFraction() != null && tbVendorEvaluate.getEvaluateFraction().compareTo(new BigDecimal(60)) >= 0){
            tbVendorEvaluate.setIsQualified("Y");
        }else {
            tbVendorEvaluate.setIsQualified("N");
        }
        tbVendorEvaluate.setUpdateBy("111");
        return ResultData.data(tbVendorEvaluateService.saveOrUpdate(tbVendorEvaluate));
    }

    @PostMapping("/submit")
    public ResultData<Boolean> submit(@RequestBody TbVendorEvaluate tbVendorEvaluate) {
        if (tbVendorEvaluate.getCreateId() == null) {
            if(tbVendorEvaluate.getId() == null){
                tbVendorEvaluate.setId(KeyUtils.generateId());
            }
            tbVendorEvaluate.setCreateTime(DateUtils.getNowDate());
            tbVendorEvaluate.setCreateId(SecurityUtils.getUserId());
            tbVendorEvaluate.setCreateBy(SecurityUtils.getUsername());
        }
        if(StringUtil.isEmpty(tbVendorEvaluate.getEvaluateCode())){
            LocalDateTime now = LocalDateTime.now(); // 获取当前时间
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss"); // 设置时间格式
            String formattedTime = now.format(formatter); // 格式化时间
            tbVendorEvaluate.setEvaluateCode(formattedTime);
        }
        tbVendorEvaluate.setEvaluateStatus("1");
        tbVendorEvaluate.setUpdateTime(DateUtils.getNowDate());
        tbVendorEvaluate.setUpdateId(SecurityUtils.getUserId());
        SysUser user = userService.getUserById(SecurityUtils.getUserId());
        if(user != null){
            tbVendorEvaluate.setUpdateBy(user.getNickName());
        }else{
            tbVendorEvaluate.setUpdateBy(SecurityUtils.getUsername());
        }
        if(tbVendorEvaluate.getEvaluateFraction() != null && tbVendorEvaluate.getEvaluateFraction().compareTo(new BigDecimal(60)) >= 0){
            tbVendorEvaluate.setIsQualified("Y");
        }else {
            tbVendorEvaluate.setIsQualified("N");
        }
        return ResultData.data(tbVendorEvaluateService.saveOrUpdate(tbVendorEvaluate));
    }


    /**
     * 删除供应商评价
     */
    //@PreAuthorize("@ss.hasPermi('modelname:tbVendorEvaluate:remove')")
    //@Log(title = "供应商评价", businessType = BusinessType.DELETE)
    @PostMapping("delete/{ids}")
    public ResultData<Boolean> remove(@PathVariable Long[] ids) {

        return ResultData.data(tbVendorEvaluateService.removeByIds(Arrays.asList(ids)));
    }

    @PostMapping("/deleteById")
    @ApiOperation(value = "删除项目")
    public ResultData<Boolean> deleteById(@RequestParam Long id) {
        tbVendorEvaluateService.removeById(id);
        return ResultData.success();
    }
}

