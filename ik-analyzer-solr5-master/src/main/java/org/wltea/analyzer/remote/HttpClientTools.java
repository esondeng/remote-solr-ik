package org.wltea.analyzer.remote;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

/**
 * Created by eson on 2017/10/24.
 */
public class HttpClientTools {

	private HttpClientTools() {

	}

	private final static PoolingHttpClientConnectionManager	CONNECTION_MANAGER;
	private final static CloseableHttpClient				DEFAULT_CLIENT;

	static {
		CONNECTION_MANAGER = new PoolingHttpClientConnectionManager();
		CONNECTION_MANAGER.setMaxTotal(20000);
		CONNECTION_MANAGER.setDefaultMaxPerRoute(500);

		// 请求重试处理
		HttpRequestRetryHandler httpRequestRetryHandler = new HttpRequestRetryHandler() {
			public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
				if (executionCount >= 2) {// 如果已经重试了2次，就放弃
					return false;
				}
				if (exception instanceof NoHttpResponseException) {// 如果服务器丢掉了连接，那么就重试
					return true;
				}
				if (exception instanceof SSLHandshakeException) {// 不要重试SSL握手异常
					return false;
				}
				if (exception instanceof InterruptedIOException) {// 超时
					return false;
				}
				if (exception instanceof UnknownHostException) {// 目标服务器不可达
					return false;
				}
				if (exception instanceof ConnectTimeoutException) {// 连接被拒绝
					return false;
				}
				if (exception instanceof SSLException) {// SSL握手异常
					return false;
				}

				HttpClientContext clientContext = HttpClientContext.adapt(context);
				HttpRequest request = clientContext.getRequest();
				// 如果请求是幂等的，就再次尝试
				if (!(request instanceof HttpEntityEnclosingRequest)) {
					return true;
				}
				return false;
			}
		};

		DEFAULT_CLIENT = HttpClients.custom().setConnectionManager(CONNECTION_MANAGER).setConnectionManagerShared(true)
				.setRetryHandler(httpRequestRetryHandler).build();

	}



	public static CloseableHttpClient retrieveHttpClient() {
		return DEFAULT_CLIENT;
	}



	/**
	 *
	 * 功能说明：HTTP Get 获取内容
	 *
	 * @param url
	 *            请求的url地址 ?之前的地址
	 * @param charSet
	 *            编码格式
	 *
	 * @return String 返回内容
	 */
	public static String doGet(String url, String charSet) {
		return doGet(url, charSet, null);
	}



	/**
	 *
	 * 功能说明：HTTP Get 获取内容
	 *
	 * @param url
	 *            请求的url地址 ?之前的地址
	 * @return String 返回内容
	 */
	public static String doGet(String url) {
		return doGet(url, "UTF-8", null);
	}



	/**
	 *
	 * 功能说明：HTTP Get 获取内容
	 *
	 * @param url
	 *            请求的url地址 ?之前的地址
	 * @param parameters
	 *            请求参数
	 * @return String 返回内容
	 */
	public static String doGet(String url, Map<String, String> parameters) {
		if (StringUtils.isEmpty(url)) {
			return null;
		}
		// 创建参数队列
		StringBuilder stringBuilder = new StringBuilder(url);
		if (parameters != null && parameters.size() > 0) {
			stringBuilder.append("?");
			for (Map.Entry<String, String> paremeter : parameters.entrySet()) {
				stringBuilder.append(paremeter.getKey() + "=" + paremeter.getValue() + "&");
			}
		}

		return doGet(stringBuilder.substring(0, stringBuilder.length() - 1));
	}



	/**
	 *
	 * 功能说明：HTTP Get 获取内容
	 *
	 * @param url
	 *            请求的url地址 ?之前的地址
	 * @param parameters
	 *            请求参数
	 * @return String 返回内容
	 */
	public static String doGet(String url, Map<String, String> parameters, Map<String, String> headers) {
		if (StringUtils.isEmpty(url)) {
			return null;
		}
		// 创建参数队列
		StringBuilder stringBuilder = new StringBuilder(url);
		if (parameters != null && parameters.size() > 0) {
			stringBuilder.append("?");
			for (Map.Entry<String, String> paremeter : parameters.entrySet()) {
				stringBuilder.append(paremeter.getKey() + "=" + paremeter.getValue() + "&");
			}
		}

		return doGet(stringBuilder.substring(0, stringBuilder.length() - 1), headers);
	}



	/**
	 *
	 * 功能说明：HTTP Get 获取内容
	 *
	 * @param url
	 *            请求的url地址 ?之前的地址
	 * @param charSet
	 *            编码格式
	 * @param headers
	 *            http header
	 * @return String 返回内容
	 */
	public static String doGet(String url, String charSet, Map<String, String> headers) {
		if (StringUtils.isEmpty(url)) {
			return null;
		}

		CloseableHttpResponse response = null;
		CloseableHttpClient httpClient = null;

		try {
			httpClient = retrieveHttpClient();
			HttpGet httpGet = new HttpGet(url);
			if (headers != null && !headers.isEmpty()) {
				for (Iterator<Map.Entry<String, String>> it = headers.entrySet().iterator(); it.hasNext();) {
					Map.Entry<String, String> header = it.next();
					httpGet.addHeader(header.getKey(), header.getValue());
				}
			}

			response = httpClient.execute(httpGet);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != 200) {
				throw new RuntimeException("HttpClient,error status code :" + statusCode);
			}
			HttpEntity entity = response.getEntity();
			String result = null;
			if (entity != null) {
				result = EntityUtils.toString(entity, charSet);
			}
			return result;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			try {
				response.close();
			} catch (IOException e1) {
				// do thing
			}

		}
	}

}
