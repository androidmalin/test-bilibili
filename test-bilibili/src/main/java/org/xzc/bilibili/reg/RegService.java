package org.xzc.bilibili.reg;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class RegService {
	private HC hc;

	public RegService(final String DedeUserID, final String SESSDATA) {
		RequestConfig rc = RequestConfig.custom().setCookieSpec( CookieSpecs.IGNORE_COOKIES ).build();
		CloseableHttpClient hc0 = HttpClients.custom().setDefaultRequestConfig( rc )
				.addInterceptorFirst( new HttpRequestInterceptor() {
					public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
						//xzchaooDRF1request.addHeader( "Cookie", "DedeUserID=19533545;SESSDATA=f0ee7f17,1481508926,1406d614;" );
						//f0ee7f17,1450578153,e9d8a416
						//xzchaooDRF2
						request.addHeader( "Cookie", "DedeUserID=" + DedeUserID + ";SESSDATA=" + SESSDATA + ";" );
					}
				} ).build();
		hc = new HC( hc0 );
	}

	public Base getBase1() {
		String s = hc.getAsString( "https://account.bilibili.com/answer/base" );
		return JSON.toJavaObject( hc.getAsJSON( "https://account.bilibili.com/answer/getBaseQ" ), Base.class );
	}

	public String submitBase1(String url, List<Question> list) {
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Question q : list) {
			if (!first)
				sb.append( "," );
			first = false;
			sb.append( q.qs_id );
			String name = "ans_hash_" + q.qs_id;
			params.add( new BasicNameValuePair( name, q.myAns ) );
		}
		params.add( new BasicNameValuePair( "qs_ids", sb.toString() ) );
		UrlEncodedFormEntity e = null;
		try {
			e = new UrlEncodedFormEntity( params, "utf-8" );
		} catch (UnsupportedEncodingException ex) {
		}
		return hc.asString(
				RequestBuilder.post( url ).setEntity( e ).build() );
	}

	private Random random = new Random();

	private void randomAns(List<Question> list) {
		for (Question q : list) {
			int id = random.nextInt( 4 ) + 1;
			switch (id) {
			case 1:
				q.myAns = q.ans1_hash;
				break;
			case 2:
				q.myAns = q.ans2_hash;
				break;
			case 3:
				q.myAns = q.ans3_hash;
				break;
			case 4:
				q.myAns = q.ans4_hash;
				break;
			}
		}
	}

	public boolean answer2() {
		Base2 b2 = getBase2();
		//setToNextAns( b2.data );
		randomAns( b2.data );
		String result = submitBase1( "https://account.bilibili.com/answer/checkPAns", b2.data );
		JSONObject jo = JSON.parseObject( result );
		result = jo.getString( "data" );
		int id = Integer.parseInt( result.substring( result.lastIndexOf( '/' ) + 1 ) );
		JSONObject json = hc.getAsJSON( "https://account.bilibili.com/ajax/answer/getCoolInfo?id=" + id );
		int score = json.getJSONObject( "data" ).getIntValue( "score" );
		System.out.println( "得分=" + score );
		return score >= 60;
	}

	public boolean answer1() {
		Base b1 = getBase1();
		while (setToNextAns( b1.data.questionList )) {
			String result = submitBase1( "https://account.bilibili.com/answer/goPromotion", b1.data.questionList );
			if (result.contains( "false" )) {
				JSONObject jo = JSON.parseObject( result );
				JSONArray ja = jo.getJSONArray( "message" );
				Set<String> wrongAnsQID = new HashSet<String>();
				for (int i = 0; i < ja.size(); ++i) {
					wrongAnsQID.add( ja.getString( i ) );
				}
				//如果不在错误名单里 那么就是通过了!
				for (Question q : b1.data.questionList) {
					if (!wrongAnsQID.contains( q.qs_id )) {
						q.status = 5;
					}
				}
			} else {
				break;
			}
		}
		return true;
	}

	private boolean setToNextAns(List<Question> list) {
		boolean changed = false;
		for (Question q : list) {
			if (q.status == 5)//已经正确了
				continue;
			++q.status;
			changed = true;
			switch (q.status) {
			case 1:
				q.myAns = q.ans1_hash;
				break;
			case 2:
				q.myAns = q.ans2_hash;
				break;
			case 3:
				q.myAns = q.ans3_hash;
				break;
			case 4:
				q.myAns = q.ans4_hash;
				break;
			}
		}
		return changed;
	}

	public Base2 getBase2() {
		HttpUriRequest req = RequestBuilder.post( "https://account.bilibili.com/answer/getQstByType" )
				//.addHeader( "User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:42.0) Gecko/20100101 Firefox/42.0" )
				.setEntity( HC.makeFormEntity( "type_ids", "11,12,13" ) )
				.build();
		String content = hc.asString( req );
		return JSON.toJavaObject( hc.asJSON( req ), Base2.class );
	}

}