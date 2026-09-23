package com.zhaocai.business.demo.app;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DemoTestFile {
	private static final boolean isWin=true;
	/**
	 * 得到测试文件的地址
	 * @param fileName 文件名
	 * @return
	 */
	public static String getFilePath(String fileName) {
        return getFilePath() +fileName;
	}
	private static String getFilePath() {
		String currentPath = DemoTestFile.class.getResource("").getPath();
		if(!currentPath.endsWith("/")) {
			currentPath+="/";
		}
		if(isWin) {
			currentPath=currentPath.substring(1);
		}
		currentPath+="files/";
		return currentPath;
	}
	/**
	 * 下载文件
	 * @param fileUrl
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
	public static String download(String fileUrl,String fileName) throws IOException {
		// 建立链接
        URL httpUrl=new URL(fileUrl);
        HttpURLConnection conn=(HttpURLConnection) httpUrl.openConnection();
        //以Post方式提交表单，默认get方式
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        // post方式不能使用缓存
        conn.setUseCaches(false);
        //连接指定的资源
        conn.connect();
        //获取网络输入流
        InputStream inputStream=conn.getInputStream();
        BufferedInputStream bis = new BufferedInputStream(inputStream);
        String downloadFilePath = getFilePath()+fileName;
        //写入到文件（注意文件保存路径的后面一定要加上文件的名称）
        FileOutputStream fileOut = new FileOutputStream(downloadFilePath);
        BufferedOutputStream bos = new BufferedOutputStream(fileOut);

        byte[] buf = new byte[4096];
        int length = bis.read(buf);
        //保存文件
        while(length != -1)
        {
            bos.write(buf, 0, length);
            length = bis.read(buf);
        }
        bos.close();
        bis.close();
        conn.disconnect();
		return downloadFilePath;
	}
	public static void main(String[] args) {
		String filePath = getFilePath("addpic.docx");
		System.out.println(filePath);
	}
}
