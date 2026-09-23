package com.zhaocai.business.common.aspect;

import com.zhaocai.business.common.annotations.RepeatSubmit;
import com.zhaocai.business.common.exception.BusinessException;
import com.zhaocai.business.common.exception.ResultCode;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.redis.enums.RedisKeyPrefixEnum;
import com.zhaocai.common.redis.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 重复提交切面
 *
 * @author chenming
 * @date 2024-08-10
 */
@Aspect
@Component
@Slf4j
public class RepeatSubmitAspect {

    @Autowired
    private RedisService redisService;

    private final ExpressionParser parser = new SpelExpressionParser();

    @Before("@annotation(repeatSubmit)")
    public void repeatSubmit(JoinPoint joinPoint, RepeatSubmit repeatSubmit) {
        String keyExpression = repeatSubmit.key();
        if (StringUtils.isBlank(keyExpression)) {
            throw new BusinessException("防止重复提交注解必须设置 key");
        }

        // 获取所有参数
        Object[] params = joinPoint.getArgs();

        // 获取方法参数名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();

        /*
         * 使用 EL 表达式解析
         */
        // 创建SpEL上下文
        StandardEvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < params.length; i++) {
            context.setVariable(paramNames[i], params[i]);
        }

        Expression expression = parser.parseExpression(keyExpression);
        String key = expression.getValue(context, String.class);
        log.info("[RepeatSubmitAspect] - redis-key:{}",key);
        key = RedisKeyPrefixEnum.SUBMIT_REPEAT.getKeyPrefix() + key;
        Boolean resultSet = redisService.setNxCacheObject(key,"1",repeatSubmit.expiredTime(), TimeUnit.SECONDS);
        if (!resultSet) {
            throw new BusinessException(ResultCode.FAILURE,"3秒内请勿重复提交表单");
        }
    }

}
