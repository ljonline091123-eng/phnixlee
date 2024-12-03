//package com.zhaocai.business.exam.controller;
//
//import java.util.List;
//import javax.servlet.http.HttpServletResponse;
//
//import com.zhaocai.business.common.base.BladeController;
//import com.zhaocai.common.core.utils.poi.ExcelUtil;
//import com.zhaocai.common.core.web.bean.ResultData;
//import com.zhaocai.common.core.web.domain.AjaxResult;
//import com.zhaocai.common.core.web.page.TableDataInfo;
//import com.zhaocai.common.log.enums.BusinessType;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.PutMapping;
//import org.springframework.web.bind.annotation.DeleteMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//import com.zhaocai.business.exam.domain.Examinee;
//import com.zhaocai.business.exam.service.IExamineeService;
//import com.zhaocai.common.log.annotation.Log;
//
//
///**
// * 考生管理Controller
// *
// * @author xwj
// * @date 2024-11-21
// */
//@RestController
//@RequestMapping("/exam/examinee")
//public class ExamineeController extends BladeController
//{
//    @Autowired
//    private IExamineeService examineeService;
//
//    /**
//     * 查询考生管理列表
//     */
//
//    //@GetMapping("/list")
//    //public TableDataInfo list(Examinee examinee)
//    //{
//    //    startPage();
//    //    List<Examinee> list = examineeService.selectExamineeList(examinee);
//    //    return getDataTable(list);
//    //}
//
//    /**
//     * 导出考生管理列表
//     */
//
//    @Log(title = "考生管理", businessType = BusinessType.EXPORT)
//    @PostMapping("/export")
//    public void export(HttpServletResponse response, Examinee examinee)
//    {
//        List<Examinee> list = examineeService.selectExamineeList(examinee);
//        ExcelUtil<Examinee> util = new ExcelUtil<Examinee>(Examinee.class);
//        util.exportExcel(response, list, "考生管理数据");
//    }
//
//    /**
//     * 获取考生管理详细信息
//     */
//
//    @GetMapping(value = "/{id}")
//    public ResultData getInfo(@PathVariable("id") Long id)
//    {
//        return ResultData.data(examineeService.selectExamineeById(id));
//    }
//
//    /**
//     * 新增考生管理
//     */
//
//    //@Log(title = "考生管理", businessType = BusinessType.INSERT)
//    //@PostMapping
//    //public ResultData add(@RequestBody Examinee examinee)
//    //{
//    //    return ResultData.data(examineeService.insertExaminee(examinee));
//    //}
//
//    /**
//     * 修改考生管理
//     */
//
//    @Log(title = "考生管理", businessType = BusinessType.UPDATE)
//    @PutMapping
//    public ResultData edit(@RequestBody Examinee examinee)
//    {
//        return ResultData.data(examineeService.updateExaminee(examinee));
//    }
//
//    /**
//     * 删除考生管理
//     */
//
//    @Log(title = "考生管理", businessType = BusinessType.DELETE)
//	@DeleteMapping("/{ids}")
//    public ResultData remove(@PathVariable Long[] ids)
//    {
//        return ResultData.data(examineeService.deleteExamineeByIds(ids));
//    }
//}
