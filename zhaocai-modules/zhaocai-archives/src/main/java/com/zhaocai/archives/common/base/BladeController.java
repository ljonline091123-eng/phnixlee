package com.zhaocai.archives.common.base;

import org.apache.poi.util.IOUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

/**
 * 基本 Controller
 *
 * @author chenming
 * @date 2024/05/27
 */
public class BladeController extends WebBaseController{

    /**
     * 下载文件
     * @param inputStream
     * @param fileName
     */
    protected void download(InputStream inputStream,String fileName) {
        HttpServletResponse response = getResponse();

        try (ServletOutputStream outputStream = response.getOutputStream();){
            response.reset();
            response.setContentType("application/octet-stream");
            response.setCharacterEncoding("utf-8");

            // 文件名
            fileName = URLEncoder.encode(fileName, "UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

            // 将文件流写入到 response 中
            byte[] b = new byte[1024];
            int length = 0;
            while ((length = inputStream.read(b)) > 0) {
                outputStream.write(b, 0, length);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /**
     * 获取 cookie 的值
     * @param request
     * @param name
     * @return
     */
    protected String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}
