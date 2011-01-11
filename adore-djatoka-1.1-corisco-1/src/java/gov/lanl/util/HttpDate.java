/*
 * Copyright (c) 2008  Los Alamos National Security, LLC.
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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 
 * @author Ryan Chute
 *
 */
public class HttpDate {
    public final static Locale LOCALE_US = Locale.US;
	public final static TimeZone GMT_ZONE = TimeZone.getTimeZone("GMT");
    public final static String RFC1123_PATTERN = "EEE, dd MMM yyyyy HH:mm:ss z";
    public final static SimpleDateFormat rfc1123Format = new SimpleDateFormat(RFC1123_PATTERN, LOCALE_US);
    
    public static String getHttpDate() {
    	Calendar calendar = new GregorianCalendar(GMT_ZONE, LOCALE_US);
        return getHttpDate(calendar, new Date(System.currentTimeMillis()));
    }
    
    public static String getHttpDate(Calendar calendar, Date time) {
        calendar.setTime(time);
        return rfc1123Format.format(calendar.getTime());
    }
}
