package com.zhaocai.archives.main.vo.req;

import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 物料详情查询请求参数
 *
 */
@Data
public class ArchivesDetailQueryVO {

    //1:'材料类'; 2:'设备类'; 3:'劳务类'; 4:'专业分包类';
    @ApiModelProperty(value = "物料分类树-类别")
    private String type;

    @ApiModelProperty(value = "选择的分类id")
    private List<String> classId;

}
