package org.xzc.bilibili.api2;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;
import org.xzc.bilibili.api2.reply.Reply;
import org.xzc.bilibili.model.Account;
import org.xzc.bilibili.model.Video;
import org.xzc.bilibili.scan.Page;
import org.xzc.http.HC;
import org.xzc.http.Params;
import org.xzc.http.Req;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;

public class BilibiliService2 {
	private static final Logger log = Logger.getLogger( BilibiliService2.class );
	public static final String DEFAULT_IP = "61.164.47.167";//14.152.58.20
	public static final String API_HOST = "api.bilibili.com";
	public static final String API_IP = DEFAULT_IP;
	public static final String ACCOUNT_HOST = "account.bilibili.com";
	public static final String ACCOUNT_IP = DEFAULT_IP;
	
	public static final String MEMBER_HOST = "member.bilibili.com";
	public static final String MEMBER_IP = DEFAULT_IP;

	private String apiServerIP = API_IP;
	private String accountServerIP = ACCOUNT_IP;
	private String memberServerIP = MEMBER_IP;

	//如果为true的话 会自动尝试加入cookie
	private boolean autoCookie = true;

	private HC hc;

	private String DedeUserID;

	private String DedeID;

	private String SESSDATA;

	private String proxyHost;

	private int proxyPort;

	private int batch = 2;

	public BilibiliService2() {
	}

	public String action(int aid, int rpid, int action) {
		Req req = Req.post( "http://api.bilibili.com/x/reply/action" )
				.datas( "jsonp", "jsonp", "oid", aid, "type", 1, "rpid", rpid, "action", action );
		return hc.asString( req );
	}

	public void clear() {
		DedeID = null;
		DedeUserID = null;
		SESSDATA = null;
	}

	public int getBatch() {
		return batch;
	}

	public String getDedeID() {
		return DedeID;
	}

	public String getDedeUserID() {
		return DedeUserID;
	}

	public HC getHC() {
		return hc;
	}

	public Result<Page<Reply>> getReplyList(int aid, int page) {
		String content = hc.asString(
				Req.get( "http://" + apiServerIP + "/x/reply?type=1&sort=0&nohot=1&pn=" + page + "&oid=" + aid )
						.host( API_HOST ) );
		JSONObject json = JSON.parseObject( content );
		int code = json.getIntValue( "code" );
		if (code != 0) {
			return new Result<Page<Reply>>( false, code, code, "视频不存在", content, null );
		}
		Page<Reply> ret = new Page<Reply>();
		ret.list = JSON.toJavaObject( json.getJSONObject( "data" ).getJSONArray( "" ),
				(Class<List<Reply>>) new TypeReference<List<Reply>>() {
				}.getType() );
		ret.pagesize = 20;
		ret.page = page;
		ret.total = json.getJSONObject( "data" ).getJSONObject( "page" ).getIntValue( "count" );
		return new Result<Page<Reply>>( true, ret );
	}

	public String getSESSDATA() {
		return SESSDATA;
	}

	public Account getUserInfo() {
		return getUserInfo( (Account) null );
	}

	public Account getUserInfo(Account a) {
		return getUserInfo( Req.get( "http://" + API_IP + "/myinfo" ).host( API_HOST ).account( a ) );
	}

	public Result<Video> getVideo(int aid) {
		String content = hc.asString( Req.get( "http://" + apiServerIP + "/x/video?aid=" + aid ).host( API_HOST ) );
		JSONObject json = JSON.parseObject( content );
		int code = json.getIntValue( "code" );
		if (code != 0) {
			return new Result<Video>( false, code, code, "视频不存在", content, null );
		}
		return new Result<Video>( true, JSON.toJavaObject( json, Video.class ) );
	}

	public boolean isAutoCookie() {
		return autoCookie;
	}

	public boolean isLogined() {
		if (DedeUserID == null || SESSDATA == null)
			return false;
		String content = hc.asString( Req.get( "http://" + memberServerIP + "/main.html" )
				.host( MEMBER_HOST ) );
		return content.contains( DedeUserID );
	}

	public boolean login(Account a) {
		if (a.SESSDATA != null) {
			DedeUserID = Integer.toString( a.mid );
			SESSDATA = a.SESSDATA;
			return isLogined();
		} else {
			return login( a.userid, a.password );
		}
	}

	public boolean login(String userid, String pwd) {
		try {
			Req req = Req.post( "http://" + accountServerIP + "/ajax/miniLogin/login" )
					.host( ACCOUNT_HOST )
					.datas( "userid", userid, "pwd", pwd );
			String content = hc.asString( req );
			JSONObject json = JSON.parseObject( content );
			if (json.getBooleanValue( "status" )) {
				URIBuilder b = null;
				b = new URIBuilder( json.getJSONObject( "data" ).getString( "crossDomain" ) );
				for (NameValuePair nvp : b.getQueryParams()) {
					if (nvp.getName().equals( "DedeUserID" ))
						DedeUserID = nvp.getValue();
					if (nvp.getName().equals( "SESSDATA" ))
						SESSDATA = nvp.getValue();
				}
				return true;
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void login0(Account a) {
		if (a.SESSDATA == null) {
			login( a );
		} else {
			SESSDATA = a.SESSDATA;
			DedeUserID = Integer.toString( a.mid );
		}
	}

	public void other() {
		hc.getAsString( "http://www.bilibili.com/" );
		hc.getAsString( "http://www.bilibili.com/video/av3431726/" );
	}

	private int timeout = 15000;

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	@PostConstruct
	public void postConstruct() {
		RequestConfig rc = RequestConfig.custom()
				.setCookieSpec( CookieSpecs.IGNORE_COOKIES )
				.setConnectionRequestTimeout( timeout )
				.setConnectTimeout( timeout )
				.setSocketTimeout( timeout )
				.build();
		HttpHost proxy = null;
		if (proxyHost != null) {
			proxy = new HttpHost( proxyHost, proxyPort );
		}
		PoolingHttpClientConnectionManager m = new PoolingHttpClientConnectionManager();
		m.setMaxTotal( batch * 2 );
		m.setDefaultMaxPerRoute( batch );
		CloseableHttpClient chc = HttpClients.custom()
				.setProxy( proxy )
				.setConnectionManager( m )
				.addInterceptorFirst( new HttpRequestInterceptor() {
					public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
						if (request.getFirstHeader( "account" ) == null && autoCookie)
							request.addHeader( "Cookie",
									"DedeUserID=" + DedeUserID + "; SESSDATA=" + SESSDATA + "; DedeID=" + DedeID
											+ ";" );
					}
				} ).setDefaultRequestConfig( rc ).build();
		hc = new HC( chc );
	}

	@PreDestroy
	public void preDestroy() {
	}

	public String report(Account a, int aid, int rpid, int reason, String content) {
		Req req = Req.post( "http://" + API_IP + "/x/reply/report?jsonp=jsonp" )
				.host( API_HOST )
				.datas( "oid", aid, "type", 1, "rpid", rpid, "reason", reason, "content", content )
				.account( a );
		return hc.asString( req );
	}

	public boolean reportWatch() {
		Req req = Req.get(
				"http://www.bilibili.com/api_proxy?action=/report_watch&oid=" + DedeID + "&aid=" + DedeID );
		String content = hc.asString( req );
		return JSON.parseObject( content ).getIntValue( "code" ) == 0;
	}

	public void setAutoCookie(boolean autoCookie) {
		this.autoCookie = autoCookie;
	}

	public void setBatch(int batch) {
		this.batch = batch;
	}

	public void setDedeID(String dedeID) {
		DedeID = dedeID;
	}

	public void setProxy(String proxyHost, int proxyPort) {
		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;
	}

	public boolean shareFirst() {
		return shareFirst( (Account) null );
	}

	public boolean shareFirst(Account a) {
		return shareFirst( Req.post( "http://" + apiServerIP + "/x/share/first" )
				.host( API_HOST )
				.datas( "id", DedeID, "type", 0, "jsonp", "json" )
				.account( a ) );
	}

	public String updatePwd(Account a, String newPassword) {
		return updatePwd( a, a.password, newPassword );
	}

	public String updatePwd(Account a, String oldPassword, String newPassword) {
		Req req = Req.post( "https://account.bilibili.com/site/updatePwd" )
				.datas( "oldpwd", oldPassword, "userpwd", newPassword, "userpwdok", newPassword, "safequestion", 0,
						"safeanswer", "" )
				.header( "Referer", "https://account.bilibili.com/" )
				.header( "Origin", "https://account.bilibili.com" )
				.header( "User-Agent",
						"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.87 Safari/537.36 QQBrowser/9.2.5584.400" )
				.header( "X-Requested-With", "XMLHttpRequest" ).account( a );
		return updatePwd( req );
	}

	public String updatePwd(String oldPassword, String newPassword) {
		return updatePwd( (Account) null, oldPassword, newPassword );
	}

	private Account getUserInfo(Req req) {
		JSONObject json = hc.asJSON( req );
		Account a = JSON.toJavaObject( json, Account.class );
		//等级信息
		JSONObject level_info = json.getJSONObject( "level_info" );
		a.currentLevel = level_info.getIntValue( "current_level" );
		a.currentMin = level_info.getIntValue( "current_min" );
		a.currentExp = level_info.getIntValue( "current_exp" );
		a.nextExp = level_info.getIntValue( "next_exp" );
		//cookie和账号密码信息
		a.SESSDATA = SESSDATA;
		return a;
	}

	private boolean shareFirst(Req req) {
		String content = hc.asString( req );
		JSONObject json = JSON.parseObject( content );
		return json.getIntValue( "code" ) == 0;
	}

	private String updatePwd(Req req) {
		return hc.asString( req );
	}

}