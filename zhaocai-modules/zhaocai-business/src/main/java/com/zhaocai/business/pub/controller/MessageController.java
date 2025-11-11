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
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.system.api.domain.SysRole;
import com.zhaocai.system.api.domain.SysUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.stream.Collectors;

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
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(messageService.selectMessageById(id));
    }

    /**
     * 新增消息中心
     */
    @Log(title = "消息中心", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Message message)
    {
        return toAjax(messageService.insertMessage(message));
    }

    /**
     * 修改消息中心
     */
    @Log(title = "消息中心", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody Message message)
    {
        return toAjax(messageService.updateMessage(message));
    }

    /**
     * 删除消息中心
     */
    @Log(title = "消息中心", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(messageService.deleteMessageByIds(ids));
    }

    @GetMapping("/getMyList")
    public TableDataInfo getMyList(Message message)
    {
        startPage();
        message.setReadFlag("0");
        message.setMsgMan(SecurityUtils.getUserId());
        List<Message> list = messageService.selectMessageList(message);
        return getDataTable(list);
    }

    @PostMapping("/messageRead/{id}")
    public AjaxResult authRole(@PathVariable("id") String id)
    {
        AjaxResult ajax = AjaxResult.success();
        Message message = messageService.selectMessageById(id);
        if(message != null){
            message.setReadFlag("1");
            return success(messageService.saveOrUpdate(message));
        }
        return ajax;
    }
}
