//========================================================================
// Original Authors : Van den Broeke Iris, Deville Daniel, Dubois Roger, Greg Wilkins
// Revision Author : Ryan Chute
// Copyright (c) 2001 Deville Daniel. All rights reserved.
// Permission to use, copy, modify and distribute this software
// for non-commercial or commercial purposes and without fee is
// hereby granted provided that this copyright notice appears in
// all copies.
//========================================================================

package gov.lanl.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

/**
 * Handler to filter remote content by Apache .htaccess format
 * 
 * @author Ryan Chute
 * 
 * Parts of code from HTAccessHandler in Jetty 6:
 * http://www.mortbay.org/jetty/jetty-6/xref/org/mortbay/jetty/security/
 * HTAccessHandler.html
 */
public class AccessManager {
	static Logger logger = Logger.getLogger(AccessManager.class);
	private ArrayList<String> _allowList = new ArrayList<String>();
	private ArrayList<String> _denyList = new ArrayList<String>();
	int _order;
	
	public AccessManager(String resource) {
		this(new File(resource));
	}
	
	public AccessManager(File resource) {
		BufferedReader htin = null;
		try {
			htin = new BufferedReader(new InputStreamReader(
					new FileInputStream(resource)));
			parse(htin);
		} catch (IOException e) {
			logger.warn(e, e);
		}
	}
	
	public boolean checkAccess(String host) {
		// Figure out if it's a host or ip
		boolean isIP = false;
		char a = host.charAt(0);
		if (a >= '0' && a <= '9')
			isIP = true;
		
		String elm;
		boolean alp = false;
		boolean dep = false;

		// if no allows and no deny defined, then return true
		if (_allowList.size() == 0 && _denyList.size() == 0)
			return (true);

		// looping for allows
		for (int i = 0; i < _allowList.size(); i++) {
			elm = _allowList.get(i);
			if (elm.equals("all")) {
				alp = true;
				break;
			} else {
				char c = elm.charAt(0);
				if (c >= '0' && c <= '9') {
					// ip
					if (isIP && host.startsWith(elm)) {
						alp = true;
						break;
					}
				} else {
					// hostname
					if (!isIP && host.endsWith(elm)) {
						alp = true;
						break;
					}
				}
			}
		}

		// looping for denies
		for (int i = 0; i < _denyList.size(); i++) {
			elm = _denyList.get(i);
			if (elm.equals("all")) {
				dep = true;
				break;
			} else {
				char c = elm.charAt(0);
				if (c >= '0' && c <= '9') { // ip
					if (isIP && host.startsWith(elm)) {
						dep = true;
						break;
					}
				} else { // hostname
					if (!isIP && host.endsWith(elm)) {
						dep = true;
						break;
					}
				}
			}
		}

		if (_order < 0) // deny,allow
			return !dep || alp;

		return alp && !dep;
	}

	public boolean isAccessLimited() {
		if (_allowList.size() > 0 || _denyList.size() > 0)
			return true;
		else
			return false;
	}

	private void parse(BufferedReader htin) throws IOException {
		String line;
		int limit = 0;
		while ((line = htin.readLine()) != null) {
			line = line.trim();
			if (line.startsWith("#"))
				continue;
			if (line.startsWith("order")) {
				if (logger.isDebugEnabled())
					logger.debug("orderline=" + line + "order=" + _order);
				if (line.indexOf("allow,deny") > 0) {
					logger.debug("==>allow+deny");
					_order = 1;
				} else if (line.indexOf("deny,allow") > 0) {
					logger.debug("==>deny,allow");
					_order = -1;
				}
			} else if (line.startsWith("allow from")) {
				int pos1 = 10;
				limit = line.length();
				while ((pos1 < limit) && (line.charAt(pos1) <= ' '))
					pos1++;
				if (logger.isDebugEnabled())
					logger.debug("allow from:" + line.substring(pos1));
				StringTokenizer tkns = new StringTokenizer(line.substring(pos1));
				while (tkns.hasMoreTokens()) {
					_allowList.add(tkns.nextToken());
				}
			} else if (line.startsWith("deny from")) {
				int pos1 = 9;
				limit = line.length();
				while ((pos1 < limit) && (line.charAt(pos1) <= ' '))
					pos1++;
				if (logger.isDebugEnabled())
					logger.debug("deny from:" + line.substring(pos1));

				StringTokenizer tkns = new StringTokenizer(line.substring(pos1));
				while (tkns.hasMoreTokens()) {
					_denyList.add(tkns.nextToken());
				}
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		AccessManager am = new AccessManager(new File(args[0]));
		System.out.println("157.193.199.44:" + am.checkAccess("157.193.199.44"));
		System.out.println("157.193.199.43:" + am.checkAccess("157.193.199.43"));
		System.out.println("128.84.103.:" + am.checkAccess("128.84.103."));
		System.out.println("68.224.187.40:" + am.checkAccess("68.224.187.40"));
		System.out.println("68.224.187.:" + am.checkAccess("68.224.187."));
		java.net.URL url = new java.net.URL("http://java.sun.com/j2se/1.4.2/docs/api/java/net/InetAddress.html");
		System.out.println("sun.com:" + am.checkAccess(url.getHost()));
	}
}