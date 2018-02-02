package org.wltea.analyzer.dic;

import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;

/**
 * Created by eson on 2017/10/25.
 */
public class DictionaryRequestHandler extends RequestHandlerBase {

	@Override
	public void handleRequestBody(SolrQueryRequest solrQueryRequest, SolrQueryResponse solrQueryResponse)
			throws Exception {
		System.out.println("========更新词库开始=============");
		Dictionary.reloadExtDirectionary();
		System.out.println("========更新词库结束=============");
	}



	@Override
	public String getDescription() {
		return "更新词库handler";
	}
}
