package com.zhaocai.business.demo.app;

public class DemoTestFile {
	private static final boolean isWin=true;
	/**
	 * 得到测试文件的地址
	 * @param fileName 文件名
	 * @return
	 */
	public static String getFilePath(String fileName) {
		String currentPath = DemoTestFile.class.getResource("").getPath();
		if(!currentPath.endsWith("/")) {
			currentPath+="/";
		}
		if(isWin) {
			currentPath=currentPath.substring(1);
		}
		currentPath+="files/";
        return currentPath +fileName;
	}
	public static void main(String[] args) {
		String filePath = getFilePath("addpic.docx");
		System.out.println(filePath);
	}
}
