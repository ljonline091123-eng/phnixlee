
package com.zhaocai.common.core.bean;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * 分页结果对象
 *
 * @param <T>
 * @author Administrator
 */
@Data
@NoArgsConstructor
public class PageResult<T> implements Serializable {


	private static final long serialVersionUID = 1L;
	/**
	 *
	 */
	@ApiModelProperty(value = "返回分页列表")
	private List<T> rows = new ArrayList<>();
	/**
	 * 总行数
	 */
	@ApiModelProperty(value = "返回总行数")
	private Integer total = 0;


	public PageResult(List<T> rows, Integer total) {
		this.total = total;
		this.rows = rows;
	}

	/**
	 * @param pages
	 */
	public PageResult(IPage<T> pages) {
		if (pages != null) {
			String total = pages.getTotal() + "";
			this.total = Integer.valueOf(total);
			this.rows = pages.getRecords();
		} else{
			this.total = 0;
			this.rows = new ArrayList<>();
		}

	}

    public static <E> PageResult<E> emptyPage() {
        return new PageResult<>(Collections.emptyList(), 0);
    }
}
