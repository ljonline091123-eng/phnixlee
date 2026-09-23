package com.zhaocai.business.common.aspect;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhaocai.business.bidding.domain.BiddingInfo;
import com.zhaocai.business.bidding.service.IBiddingInfoService;
import com.zhaocai.business.bidding.service.ITenderNoticeService;
import com.zhaocai.business.bidding.vo.res.TenderNoticeSchemeInfoVO;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.common.core.constant.NumberConstant;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author ssy
 * @date 2024/7/24 19:27
 */
@Aspect
@Component
@Slf4j
public class TenderNoticeHandlerAspect {

    @Autowired
    private ITenderNoticeService tenderNoticeService;
    @Autowired
    private IBiddingInfoService biddingInfoService;

    @Pointcut("@annotation(com.zhaocai.business.common.annotations.TenderNoticeHandler)")
    public void logPointCut() {
    }

    @After(value = "logPointCut()")
    public void doAfterCut(JoinPoint joinPoint) {
        log.info("请求开始 : ==========================");
        // 接收到请求，记录请求内容
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
//        EDCBaseRequestBody body = new EDCBaseRequestBody();
        Long noticeId = null;
        log.info("解析请求参数 : =================开始==================");
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        // 只记录post方法 传json格式的数据
        ;
        if (HttpMethod.POST.name().equals(request.getMethod())) {
            try {
                LocalVariableTableParameterNameDiscoverer u = new LocalVariableTableParameterNameDiscoverer();
                String[] paramNames = u.getParameterNames(method);
                //方法 1 请求的方法参数值 JSON 格式 null不显示
                if (joinPoint.getArgs().length > 0) {
                    Object[] args = joinPoint.getArgs();
                    for (int i = 0; i < args.length; i++) {
                        //请求参数类型判断过滤，防止JSON转换报错
                        if (args[i] instanceof HttpServletRequest || args[i] instanceof HttpServletResponse || args[i] instanceof MultipartFile) {
                            continue;
                        }
                        String jsonstr = JSON.toJSONString(args[i]);
                        Map<String, Object> map = JSONObject.parseObject(jsonstr);
                        if (map.containsKey("noticeId")) {
                            noticeId = ((map.get("noticeId")).getClass().equals(String.class)) ?
                                    Long.parseLong((String) map.get("noticeId")) : (Long) map.get("noticeId");
                        }
                        log.info("请求参数名称 :" + paramNames[i] + ", 内容 :" + JSON.toJSONString(args[i]));
                    }
                }
                log.info("noticeId={}", noticeId);
                //处理noticeId
                handleTenderNoticeStatus(noticeId);
            } catch (Exception ex) {
                log.info("切面处理招标公告状态失败，错误为{}", ex.getMessage());
            }
        } else if (HttpMethod.GET.name().equals(request.getMethod())) {
            throw new ParamValidateException("GET请求参数解析异常");
        } else {
            throw new ParamValidateException("请求参数异常");
        }
    }

    /**
     * 处理招标公告状态
     * */
    private void handleTenderNoticeStatus(Long noticeId){
        //处理noticeId
        TenderNoticeSchemeInfoVO detailVO = tenderNoticeService.getTenderNoticeSchemeInfo(noticeId);
        //如果该公告属于采购方案类型中的单一来源
        if (NumberConstant.FOUR == detailVO.getSchemeType()){
            long count = biddingInfoService.count(new LambdaQueryWrapper<BiddingInfo>()
                    .eq(BiddingInfo::getNoticeId, noticeId));
            if (count > 0){
                //如果产生了供应商投标数据
                Integer nextNoticeStatus = tenderNoticeService.nextTenderNoticeStatus(detailVO.getSchemeType(), detailVO.getNoticeStatus());
                tenderNoticeService.updateStatus(noticeId, nextNoticeStatus);
            }
        }
    }

}
