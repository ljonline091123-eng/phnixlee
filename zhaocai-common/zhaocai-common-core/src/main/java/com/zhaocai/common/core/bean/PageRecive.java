package com.zhaocai.common.core.bean;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * 分页页面接收对象，所有需要分页的DTO继承此对象
 *
 * @author Administrator
 *
 */
public class PageRecive implements Serializable {

    /**
     * 当前页
     */
    @ApiModelProperty(value = "当前页，默认1")
    protected Integer pageNumber = 1;
    /**
     * 页码大小
     */
    @ApiModelProperty(value = "页面大小，默认10")
    protected Integer pageSize = 10;

    /**
     * 结束页
     */
    private Integer pageStart;
    /**
     * 起始页
     */
    private Integer pageEnd;

    @ApiModelProperty(value = "排序，默认升序，如需降序传desc")
    private String order;

    @ApiModelProperty(value = "排序字段名称")
    private String orderName = "";

    public Page toMybatisPage() {
        return new Page(pageNumber, pageSize);
    }

    public PageRecive() {
        pageSize = 10;
    }

    public PageRecive(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(final Integer pageNumber) {
        this.pageNumber = pageNumber;
        if (pageNumber < 1) {
            this.pageNumber = 0;
        }
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(final int pageSize) {
        this.pageSize = pageSize;
        if (pageSize > 200) {
            this.pageSize = 200;
        }
        pageStart = (pageNumber - 1) * this.pageSize;
        pageEnd = pageNumber * this.pageSize;
    }

    public void setMaxExcelPage() {
        this.pageNumber = 1;
        this.pageSize = 65536;
        pageStart = 0;
        pageEnd = pageNumber * this.pageSize;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public Integer getPageStart() {
        return pageStart;
    }

    public void setPageStart(Integer pageStart) {
        this.pageStart = pageStart;
    }

    public Integer getPageEnd() {
        return pageEnd;
    }

    public void setPageEnd(Integer pageEnd) {
        this.pageEnd = pageEnd;
    }

    public String getOrderName() {
        return orderName;
    }

    public void setOrderName(String orderName) {
        this.orderName = orderName;
    }

    public static <T> Page<T> convertToPage(PageRecive pageRecive) {
        return new Page<T>(pageRecive.getPageNumber(), pageRecive.getPageSize());
    }
}
