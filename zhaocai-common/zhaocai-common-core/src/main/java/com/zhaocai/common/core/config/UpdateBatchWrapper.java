package com.zhaocai.common.core.config;

import com.baomidou.mybatisplus.core.conditions.AbstractLambdaWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author ssy
 * @date 2024/8/22 20:56
 */
public class UpdateBatchWrapper<T> extends AbstractLambdaWrapper<T, UpdateBatchWrapper<T>> {
    private static final long serialVersionUID = 114684162001472707L;

    /**
     * 需要更新的字段
     */
    private List<String> updateFields = null;

    @Override
    protected UpdateBatchWrapper<T> instance() {
        this.updateFields = new ArrayList<>();
        return this;
    }

    /**
     * 关键代码,为属性设置值
     */
    @SafeVarargs
    public final UpdateBatchWrapper<T> setUpdateFields(SFunction<T, ?>... columns) {
        this.updateFields = Arrays.asList(columnsToString(columns).split(","));
        return this;
    }

    public List<String> getUpdateFields() {
        return updateFields;
    }
}
