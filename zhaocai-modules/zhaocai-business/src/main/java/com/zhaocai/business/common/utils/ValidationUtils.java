package com.zhaocai.business.common.utils;


import com.zhaocai.business.common.exception.ParamValidateException;
import org.springframework.util.CollectionUtils;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.HashSet;
import java.util.Set;

/**
 * 参数校验工具类
 *
 * @author chenming
 * @date 2024-07-09
 */
public class ValidationUtils {

    private ValidationUtils() {
    }

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();

    /**
     * 参数校验
     *
     * @param paramObject 需要校验的参数
     */
    public static void validateObject(Object paramObject) {
        Validator validator = VALIDATOR_FACTORY.getValidator();
        Set<ConstraintViolation<Object>> validateResult = validator.validate(paramObject);
        if (CollectionUtils.isEmpty(validateResult)) {
            return;
        }

        StringBuilder errorMessageSb = new StringBuilder();
        for (ConstraintViolation<Object> violation : validateResult) {
            errorMessageSb.append(violation.getMessage()).append(";");
        }

        throw new ParamValidateException(errorMessageSb.toString());
    }

    /**
     * 参数校验【支持分组校验】
     *
     * @param paramObject 需要校验的参数
     * @param classes     具体的分组
     */
    public static void validateObject(Object paramObject, Class<?> classes) {
        Validator validator = VALIDATOR_FACTORY.getValidator();
        Set<ConstraintViolation<Object>> validateResult = validator.validate(paramObject, classes);
        if (CollectionUtils.isEmpty(validateResult)) {
            return;
        }

        Set<String> errorMsgSet = new HashSet<>();
        for (ConstraintViolation<Object> violation : validateResult) {
            errorMsgSet.add(violation.getMessage());
        }
        throw new ParamValidateException(String.join(",", errorMsgSet));
    }

}
