package com.zhaocai.business.bidding.controller;

import com.zhaocai.business.bidding.service.ITenderNoticeService;
import com.zhaocai.business.bidding.vo.req.TenderNoticeVO;
import com.zhaocai.business.bidding.vo.req.UnderlingTenderNoticeQueryVO;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.common.core.bean.PageResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

@Api(value = "第三方-招标公告接口")
@RestController
@RequestMapping("/underling/notice")
public class UnderlingTenderNoticeController extends BladeController {

    @Autowired
    private ITenderNoticeService noticeService;

    /**
     * 获取招标公告列表
     * @param queryVO
     * @return
     */
    @GetMapping("/listPage")
    @ApiOperation(value = "获取招标公告列表")
    public PageResult<TenderNoticeVO> listPage(UnderlingTenderNoticeQueryVO queryVO, HttpServletRequest request) {
        System.out.println("[获取招标公告列表]参数打印开始");
        request.getParameterMap().forEach((key, value) -> {
            System.out.println(key + " : " + Arrays.toString(value));
        });
        System.out.println("[获取招标公告列表]参数打印结束");
        return noticeService.listTenderNoticePage(queryVO);
    }

    // 处理参数类型转换异常
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> handleTypeMismatchException(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        System.out.println("[获取招标公告列表-类型转换异常] 参数信息打印开始");
        request.getParameterMap().forEach((key, value) -> {
            System.out.println(key + " : " + Arrays.toString(value));
        });
        System.out.println("[获取招标公告列表-类型转换异常] 参数信息打印结束");
        return ResponseEntity.badRequest().body("参数类型错误: " + ex.getMessage());
    }
}
