package com.zhaocai.business.sdk.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

/**
 * 用来加载类，classpath下的资源文件，属性文件等。
 * @author lizb <br>
 *         getExtendResource(StringrelativePath)方法，可以使用../符号来加载classpath外部的资源。
 */
public class UtilClassLoader {

	/**
	 * Thread.currentThread().getContextClassLoader().getResource("")
	 */

	/**
	 * 加载Java类。 使用全限定类名
	 * 
	 * @param className
	 * @return Class
	 */
	public static Class<?> loadClass(String className) {
		try {
			return getClassLoader().loadClass(className);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("class not found '" + className + "'", e);
		}
	}
	/**
	 * 加载Java类。 使用全限定类名
	 * @param className
	 * @return Object
	 */
	public static Object getClass(String className){
		try {
			 return Class.forName(className).newInstance();
		}  catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	/**
	 * 得到类加载器
	 * 
	 * @return ClassLoader
	 */
	public static ClassLoader getClassLoader() {

		return UtilClassLoader.class.getClassLoader();
	}

	/**
	 * 提供相对于classpath的资源路径，返回文件的输入流
	 * 
	 * @param relativePath
	 *            必须传递资源的相对路径。是相对于classpath的路径。如果需要查找classpath外部的资源，需要使用../来查找
	 * @return 文件输入流
	 */
	public static InputStream getStream(String relativePath)
			throws MalformedURLException, IOException {
		if (!relativePath.contains("../")) {
			return getClassLoader().getResourceAsStream(relativePath);

		} else {
			return UtilClassLoader.getStreamByExtendResource(relativePath);
		}

	}

	/**
	 * 
	 * @param url
	 * @return InputStream
	 */
	public static InputStream getStream(URL url) throws IOException {
		if (url != null) {

			return url.openStream();

		} else {
			return null;
		}
	}

	/**
	 * 
	 * @param relativePath
	 *            必须传递资源的相对路径。是相对于classpath的路径。如果需要查找classpath外部的资源，需要使用../来查找
	 * @return InputStream
	 */
	public static InputStream getStreamByExtendResource(String relativePath)
			throws MalformedURLException, IOException {
		return UtilClassLoader.getStream(UtilClassLoader
				.getExtendResource(relativePath));

	}

	/**
	 * 提供相对于classpath的资源路径，返回属性对象，它是一个散列表
	 * 
	 * @param resource
	 * @return Properties
	 */
	public static Properties getProperties(String resource) {
		Properties properties = new Properties();
		try {
			properties.load(getStream(resource));
		} catch (IOException e) {
			throw new RuntimeException("couldn't load properties file '"
					+ resource + "'", e);
		}
		return properties;
	}

	/**
	 * 得到本Class所在的ClassLoader的Classpat的绝对路径。 URL形式的
	 * 
	 * @return String
	 */
	public static String getAbsolutePathOfClassLoaderClassPath() {
		return UtilClassLoader.getClassLoader().getResource("").toString();

	}

	/**
	 * 
	 * @param relativePath
	 *            必须传递资源的相对路径。是相对于classpath的路径。如果需要查找classpath外部的资源，需要使用../来查找
	 * @return 资源的绝对URL
	 */
	public static URL getExtendResource(String relativePath)
			throws MalformedURLException {

		// ClassLoaderUtil.log.info(Integer.valueOf(relativePath.indexOf("../")))
		// ;
		if (!relativePath.contains("../")) {
			return UtilClassLoader.getResource(relativePath);

		}
		String classPathAbsolutePath = UtilClassLoader
				.getAbsolutePathOfClassLoaderClassPath();
		if (relativePath.substring(0, 1).equals("/")) {
			relativePath = relativePath.substring(1);
		}
		String wildcardString = relativePath.substring(0, relativePath
				.lastIndexOf("../") + 3);
		relativePath = relativePath
				.substring(relativePath.lastIndexOf("../") + 3);
		int containSum = UtilClassLoader.containSum(wildcardString, "../");
		classPathAbsolutePath = UtilClassLoader.cutLastString(
				classPathAbsolutePath, "/", containSum);
		String resourceAbsolutePath = classPathAbsolutePath + relativePath;
		URL resourceAbsoluteURL = new URL(resourceAbsolutePath);
		return resourceAbsoluteURL;
	}

	/**
	 * 
	 * @param source
	 * @param dest
	 * @return int
	 */
	private static int containSum(String source, String dest) {
		int containSum = 0;
		int destLength = dest.length();
		while (source.contains(dest)) {
			containSum = containSum + 1;
			source = source.substring(destLength);

		}

		return containSum;
	}

	/**
	 * 
	 * @param source
	 * @param dest
	 * @param num
	 * @return String
	 */
	private static String cutLastString(String source, String dest, int num) {
		// String cutSource=null;
		for (int i = 0; i < num; i++) {
			source = source.substring(0, source.lastIndexOf(dest, source
					.length() - 2) + 1);

		}

		return source;
	}

	/**
	 * 
	 * @param resource
	 * @return URL
	 */
	public static URL getResource(String resource) {
		return UtilClassLoader.getClassLoader().getResource(resource);
	}

}