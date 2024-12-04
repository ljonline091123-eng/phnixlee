package com.zhaocai.business.demo.servlet;

import com.zhaocai.business.demo.app.DemoTestFile;
import com.zhaocai.business.demo.app.DpDemo;
import org.json.JSONException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;



/**
 * Servlet implementation class OpenFile
 */
@WebServlet("/openFile")
public class OpenFile extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public OpenFile() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//获取文档名称
		String fileName = request.getParameter("file");
		if(fileName==null || "".equals(fileName)) {
			fileName="抢抓机遇期培育新动能.docx";
		}
		String domain = request.getParameter("h");
		//获取类型
//		String type = request.getParameter("type");
		String filePath= DemoTestFile.getFilePath(fileName);
		try {
			String url= DpDemo.webEditDocument(filePath, domain);
			response.sendRedirect(url);
		} catch (NoSuchAlgorithmException | JSONException e) {
			response.addHeader("Content-Type", "text/html;charset=UTF-8");
			response.setContentType("text/html;charset=UTF-8");
        	PrintWriter out = null;
        	try {
				out = response.getWriter();
				out.write(e.getMessage());
			} finally {
				if (out != null) {
					out.close();
				}
			}
			e.printStackTrace();
		}
	}

}
