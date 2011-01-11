/*
 * Copyright (c) 2009  Los Alamos National Security, LLC.
 *
 * Los Alamos National Laboratory
 * Research Library
 * Digital Library Research & Prototyping Team
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 * 
 */

package gov.lanl.util;

import gov.lanl.adore.djatoka.openurl.ResolverException;

import java.util.Date;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Logger;

/**
 * DBCP / JDBC Utilities Wrapper
 * @author Ryan Chute
 *
 */
public class DBCPUtils {
	static Logger log = Logger.getLogger(DBCPUtils.class.getName());
	
	/**
	 * Set-up a DBCP DataSource from a properties object. Uses a properties 
	 * key prefix to identify the properties associated with profile.  If 
	 * a database profile has a prefix of djatoka, the props object would 
	 * contain the following pairs:
	 * djatoka.url=jdbc:mysql://localhost/djatoka
     * djatoka.driver=com.mysql.jdbc.Driver
     * djatoka.login=root
     * djatoka.pwd=
     * djatoka.maxActive=50
     * djatoka.maxIdle=10
     * @param dbid database profile properties file prefix
     * @param props properties object containing relevant pairs
	 */
	public static DataSource setupDataSource(String dbid, Properties props) throws Exception {
		String url = props.getProperty(dbid + ".url");
		String driver = props.getProperty(dbid + ".driver");
		String login = props.getProperty(dbid + ".login");
		String pwd = props.getProperty(dbid + ".pwd");
		int maxActive = 50;
		if (props.containsKey(dbid + ".maxActive"))
		    maxActive = Integer.parseInt(props.getProperty(dbid + ".maxActive"));
		int maxIdle = 10;
		if (props.containsKey(dbid + ".maxIdle"))
		    maxIdle = Integer.parseInt(props.getProperty(dbid + ".maxIdle"));
		log.debug(url + ";" + driver + ";" + login + ";" + pwd + ";" + maxActive + ";" + maxIdle);
		return setupDataSource(url, driver, login, pwd, maxActive, maxIdle);
	}

	/**
	 * Set-up a DBCP DataSource from core connection properties.
	 * @param connectURI jdbc connection uri
	 * @param jdbcDriverName qualified classpath to jdbc driver for database
	 * @param username database user account
	 * @param password database password
	 * @param maxActive max simultaneous db connections (default: 50)
	 * @param maxIdle max idle db connections (default: 10)
	 */
	public static DataSource setupDataSource(String connectURI,
			String jdbcDriverName, String username, String password, int maxActive, int maxIdle) throws Exception{
		try {
			java.lang.Class.forName(jdbcDriverName).newInstance();
		} catch (Exception e) {
			log.error("Error when attempting to obtain DB Driver: "
					+ jdbcDriverName + " on " + new Date().toString(), e);
			throw new ResolverException(e);
		}

		if (maxActive <= 0)
			maxActive = 50;
		if (maxIdle <= 0)
			maxIdle = 10;
		
		GenericObjectPool connectionPool = new GenericObjectPool(
                null, // PoolableObjectFactory, can be null
                maxActive, // max active
                GenericObjectPool.WHEN_EXHAUSTED_BLOCK, // action when exhausted
                3000, // max wait (milli seconds)
                maxIdle, // max idle
                false, // test on borrow
                false, // test on return
                60000, // time between eviction runs (millis)
                5, // number to test on eviction run
                30000, // min evictable idle time (millis)
                true // test while idle
                );
		
		ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(
				connectURI, username, password);

		PoolableConnectionFactory poolableConnectionFactory = new PoolableConnectionFactory(
				connectionFactory, connectionPool, null, null, false, true);

		PoolingDataSource dataSource = new PoolingDataSource(connectionPool);

		return dataSource;
	}
}
