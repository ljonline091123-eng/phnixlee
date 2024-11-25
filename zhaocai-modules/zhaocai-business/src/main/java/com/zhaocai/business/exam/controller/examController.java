package com.zhaocai.business.exam.controller;

import com.zhaocai.business.common.annotations.VendorStateCheck;
import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.common.utils.FileUploadUtils;
import com.zhaocai.business.common.utils.FreeMarkUtils;
import com.zhaocai.business.common.utils.LibToPdf;
import com.zhaocai.business.exam.domain.Examinee;
import com.zhaocai.business.exam.service.IExamineeService;
import com.zhaocai.business.pub.service.ISysFileService;
import com.zhaocai.business.vendor.vo.req.VendorSaveRequestVO;
import com.zhaocai.business.vendor.vo.res.VendorDetailVO;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.file.FileTypeUtils;
import com.zhaocai.common.core.utils.file.MimeTypeUtils;
import com.zhaocai.common.core.utils.uuid.Seq;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.core.web.domain.AjaxResult;
import com.zhaocai.common.core.web.page.TableDataInfo;
import com.zhaocai.common.log.enums.BusinessType;
import com.zhaocai.system.api.model.LoginUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.zhaocai.common.log.annotation.Log;

import javax.validation.Valid;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import com.artofsolving.jodconverter.DefaultDocumentFormatRegistry;
import com.artofsolving.jodconverter.DocumentFormat;
import com.artofsolving.jodconverter.openoffice.connection.SocketOpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.converter.StreamOpenOfficeDocumentConverter;
import com.deepoove.poi.xwpf.NiceXWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import java.io.*;
import java.net.ConnectException;
import java.util.UUID;

import org.apache.poi.xwpf.usermodel.XWPFDocument;


@Api("考生管理")
@RestController
@RequestMapping("/exam/exam")
public class examController extends BladeController {

    @Autowired
    private IExamineeService examineeService;

    @Autowired
    private ISysFileService iSysFileService;

    //保存头像
    @Log(title = "保存考生头像", businessType = BusinessType.UPDATE)
    @PostMapping("/avatar")
    public ResultData<Examinee>  saveAvatar(@RequestBody Examinee exam)
    {
        String pictureUrl = exam.getPictureUrl();
        String identityCardId = exam.getIdentityCardId();
        if (StringUtils.isBlank(pictureUrl) || StringUtils.isBlank(identityCardId)) {
            return ResultData.fail("图片或身份证号为空，无法保存");
        }

        Examinee examinee = examineeService.selectExamineeByIdentityCardId(identityCardId);
        if (examinee == null) {
            return ResultData.fail("不存在该身份证号的考生");
        }

        examinee.setPictureUrl(pictureUrl);
        examineeService.updateExaminee(examinee);

        return ResultData.success();
    }




    //根据输入的身份证号码查询记录是否存在

    @GetMapping("/checkIdentityCardId")
    public ResultData<String>  checkIdentityCardId(@RequestParam("identityCardId") String identityCardId)
    {
        Examinee examinee = examineeService.selectExamineeByIdentityCardId(identityCardId);
        if (examinee != null){
            return ResultData.success("存在该身份证号的用户");
        }
        return ResultData.fail("您输入的身份证码未在参赛人员信息名单中，请检查身份证号码是否输入错误!");

    }

    //根据输入的身份证号码替换word模板，并生成pdf，然后在浏览器打开
    @GetMapping("/repaceWord")
    public ResultData repaceWord(@RequestParam("identityCardId") String identityCardId) throws IOException {
        Examinee examinee =  examineeService.selectExamineeByIdentityCardId(identityCardId);
        Map<String, Object> params = new HashMap<>();
        if (examinee != null){
            params.put("name",examinee.getExamineeName());
            params.put("card",identityCardId);
            params.put("dep",examinee.getWorkUnit());
            params.put("no",examinee.getEntryCardNumber());
            params.put("no1",examinee.getExaminationRoom());
            params.put("no2",examinee.getSeatNumber());
            //创建results文件夹存生成的文件
            File resultsDir = new File("D:\\results");
            if (!resultsDir.exists()) {
                resultsDir.mkdirs();
            }
            //生成没有头像的文档
            String outPath= FreeMarkUtils.createDocx(params, "templates/2.docx","D:\\results\\");
            //判断图片是否存在
            String picture = examinee.getPictureUrl();
            if (Objects.isNull(picture) || picture.isEmpty()) {
                return ResultData.fail("请先上传头像图片");
            }
            //下载头像头像图片到本地
            String pictureUrl = FreeMarkUtils.downloadFileFromUrl(examinee.getPictureUrl());
            //添加头像图片到word文档中,
            try {
                String outPutfileName = UUID.randomUUID().toString() + ".docx";
                String wordTargetPath = "D:\\results\\" +"examFileWithPicture"+ outPutfileName;
                FreeMarkUtils.sealInWord(outPath,
                        wordTargetPath,
                        pictureUrl, "参赛人员须知", 80, 100,
                        370, -190, false);

                //将word文档转换为pdf格式
                InputStream input=new FileInputStream(wordTargetPath);
                NiceXWPFDocument doc=new NiceXWPFDocument(input);
                String PDFfileName = UUID.randomUUID().toString() + ".pdf";
                String PDFtargetPath = "D:\\results\\" +"examPDF"+ PDFfileName;
                OutputStream outputStream = new FileOutputStream(PDFtargetPath);
                ByteArrayInputStream inputStream = LibToPdf.getNiceXWPFDocByInputStream(doc);
                try {
                    LibToPdf.setLibreoffceLocation("192.168.30.240");
                    LibToPdf.setLibreoffceProt(8989);
                    LibToPdf.doDocumentConvert(inputStream,outputStream, "docx","pdf");
                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        if(outputStream != null){
                            outputStream.close();
                        }
                    }catch (Exception ex){
                        ex.printStackTrace();
                    }
                }finally {
                }

                //读取生成的pdf文件
                File fileWithImg = new File(PDFtargetPath);
                if (!fileWithImg.exists()) {
                    throw new IOException("生成的带有头像图片的Word文档不存在: " + PDFtargetPath);
                }
                // 将带有头像的word文档转换为InputStream
                FileInputStream fis = new FileInputStream(fileWithImg);
                String imgFileName = StringUtils.format("{}/{}_{}.{}", DateUtils.datePath(),
                        FilenameUtils.getBaseName(fileWithImg.getName()), Seq.getId(Seq.uploadSeqType), FileTypeUtils.getFileType(fileWithImg));

                //获取文件类型
//                Path path = Paths.get(PDFtargetPath);
//                String contentType = Files.probeContentType(path);
                // 将带有头像的word文档上传到MinIO
//                String fileWithImgUrl = iSysFileService.uploadFile(fis, imgFileName,contentType);
                String fileWithImgUrl = iSysFileService.uploadFile(fis, imgFileName);
                return ResultData.success(fileWithImgUrl);

            } catch (Exception e) {
                e.printStackTrace();
                return ResultData.fail("生成带图片的准考证文档失败");
            }

        }
        return ResultData.fail("未查询到该身份证的用户");
    }


    /**
     * 新增考生信息
     */
    @Log(title = "考生管理", businessType = BusinessType.INSERT)
    @PostMapping
    public ResultData add(@RequestBody Examinee examinee)
    {
        examineeService.insertExaminee(examinee);
        return ResultData.success();
    }

    /**
     * 查询考生管理列表
     */

    @GetMapping("/list")
    public ResultData<List<Examinee>> Examineerlist(Examinee examinee)
    {

        List<Examinee> list = examineeService.selectExamineeList(examinee);
        return ResultData.data(list);
    }

}
