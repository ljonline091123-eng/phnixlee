package com.zhaocai.common.security.handler;


import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.security.utils.SecurityUtils;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 自动填充处理类
 *
 * @author jishanfeng
 * @date 2024-01-12
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // 获取当前登录用户
        String userName = SecurityUtils.getLoginUserNickName();
        if (StringUtils.isBlank(userName)) {
            userName = SecurityUtils.getUsername();
        }
        Long userId = SecurityUtils.getUserId();
        fillValue("createBy", userName, metaObject);
        fillValue("createTime", DateUtils.getNowDate(), metaObject);
        fillValue("createId", userId, metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 获取当前登录用户
        String userName = SecurityUtils.getLoginUserNickName();
        if (StringUtils.isBlank(userName)) {
            userName = SecurityUtils.getUsername();
        }
        Long userId = SecurityUtils.getUserId();
        fillValue("updateBy", userName, metaObject);
        fillValue("updateTime", DateUtils.getNowDate(), metaObject);
        fillValue("updateId", userId, metaObject);
    }

    private void fillValue(String fieldName, Object data, MetaObject metaObject) {
        if (metaObject.hasSetter(fieldName)) {
            // 值为空时设置默认值
            Object sidObj = getFieldValByName(fieldName, metaObject);
            if (sidObj == null || "updateBy".equals(fieldName) || "updateTime".equals(fieldName)) {
                setFieldValByName(fieldName, data, metaObject);
            }
        }
    }
}

