package com.zhaocai.business.pub.controller;

import com.zhaocai.business.pub.domain.Message;
import com.zhaocai.business.pub.service.IMessageService;
import com.zhaocai.common.core.utils.poi.ExcelUtil;
import com.zhaocai.common.core.web.controller.BaseController;
import com.zhaocai.common.core.web.domain.AjaxResult;
import com.zhaocai.common.core.web.page.TableDataInfo;
import com.zhaocai.common.log.annotation.Log;
import com.zhaocai.common.log.enums.BusinessType;
import com.zhaocai.common.security.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 消息中心Controller
 * 
 * @author WH
 * @date 2024-05-24
 */
@RestController
@RequestMapping("/message")
public class MessageController extends BaseController
{
    @Autowired
    private IMessageService messageService;

    /**
     * 查询消息中心列表
     */
    @RequiresPermissions("pub:message:list")
    @GetMapping("/list")
    public TableDataInfo list(Message message)
    {
        startPage();
        List<Message> list = messageService.selectMessageList(message);
        return getDataTable(list);
    }

    /**
     * 导出消息中心列表
     */
    @RequiresPermissions("pub:message:export")
    @Log(title = "消息中心", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Message message)
    {
        List<Message> list = messageService.selectMessageList(message);
        ExcelUtil<Message> util = new ExcelUtil<Message>(Message.class);
        util.exportExcel(response, list, "消息中心数据");
    }

    /**
     * 获取消息中心详细信息
     */
    @RequiresPermissions("pub:message:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(messageService.selectMessageById(id));
    }

    /**
     * 新增消息中心
     */
    @RequiresPermissions("pub:message:add")
    @Log(title = "消息中心", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Message message)
    {
        return toAjax(messageService.insertMessage(message));
    }

    /**
     * 修改消息中心
     */
    @RequiresPermissions("pub:message:edit")
    @Log(title = "消息中心", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Message message)
    {
        return toAjax(messageService.updateMessage(message));
    }

    /**
     * 删除消息中心
     */
    @RequiresPermissions("pub:message:remove")
    @Log(title = "消息中心", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(messageService.deleteMessageByIds(ids));
    }
}
