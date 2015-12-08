package org.xzc.bilibili;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.xzc.bilibili.dao.BilibiliDB;
import org.xzc.bilibili.model.Account;

import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;

@Configuration
@ComponentScan
public class AppConfig {

	@Bean(destroyMethod = "close")
	public ConnectionSource connectionSource() {
		try {
			Class.forName( "org.sqlite.JDBC" );
			ConnectionSource cs = new JdbcConnectionSource( "jdbc:sqlite:bilibili.db" );
			return cs;
		} catch (Exception e) {
			throw new IllegalStateException( e );
		}
	}

	@Bean(name = "simpleAccount")
	public Account simpleAccount() {
		Account a = new Account( 19216452, "704fe3e6,1450055207,1cc44621" );
																	//  704fe3e6,1450078325,1d03124f
		return a;
	}
	@Bean(name = "mainAccount")
	public Account mainAccount() {
		Account a = new Account( 1655915, "137419f3,1449971802,b48e796e" );
		return a;
	}

	@Bean(name = "simpleBilibiliService")
	public BilibiliService simpleBilibiliService(@Qualifier("simpleAccount") Account a) {
		//		Account a = new Account( "19161363", "b365258b,1449971602,80d03867" );
		//		Account a = new Account( "19216452", "704fe3e6,1450055207,1cc44621" );
		//Account a = new Account( "1655915", "137419f3,1449971802,b48e796e" );
		BilibiliService bs = new BilibiliService( a );
		return bs;
	}

	@Bean(name = "mainBilibiliService")
	public BilibiliService mainBilibiliService(@Qualifier("mainAccount") Account a) {
		BilibiliService bs = new BilibiliService( a );
		return bs;

	}
}
