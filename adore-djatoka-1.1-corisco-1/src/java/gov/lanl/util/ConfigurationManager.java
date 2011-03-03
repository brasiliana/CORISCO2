/*
 * ConfigurationManager.java
 *
 * Brasiliana USP, 2011.
 * 
 * Copied and modified from:
 *
 * Version: $Revision: 4243 $
 *
 * Date: $Date: 2009-09-02 09:12:23 +0000 (Wed, 02 Sep 2009) $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the DSpace Foundation nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package gov.lanl.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;


import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.OptionConverter;

/**
 * Class for reading the Djatoka system configuration. The main configuration is
 * read in as properties from a standard properties file. Email templates and
 * configuration files for other tools are also be accessed via this class.
 * <P>
 * The main configuration is by default read from the <em>resource</em>
 * <code>/djatoka.cfg</code>.
 * To specify a different configuration, the system property
 * <code>djatoka.configuration</code> should be set to the <em>filename</em>
 * of the configuration file.
 * <P>
 * Other configuration files are read from the <code>config</code> directory
 * of the Djatoka installation directory.
 *
 * 
 * @author Fabio N. Kepler - Modifications for use with Djatoka.
 * @author Robert Tansley
 * @author Larry Stone - Interpolated values.
 * @author Mark Diggory - General Improvements to detection, logging and loading.
 * @version $Revision: 4243 $
 */
public class ConfigurationManager
{
    /** log4j category */
    private static Logger log = Logger.getLogger(ConfigurationManager.class);

    /** The configuration properties */
    private static Properties properties = null;

    /** The default license */
    private static String license;

    // limit of recursive depth of property variable interpolation in
    // configuration; anything greater than this is very likely to be a loop.
    private final static int RECURSION_LIMIT = 9;

    /**
     * Identify if Djatoka is properly configured
     * @return boolean true if configured, false otherwise
     */
    public static boolean isConfigured()
    {
        return properties != null;
    }
    
    /**
     * 
     */
    public static Properties getProperties()
    {
        if (properties == null)
        {
            loadConfig(null);
        }
        
        return (Properties)properties.clone();
    }
    
    /**
     * Get a configuration property
     * 
     * @param property
     *            the name of the property
     * 
     * @return the value of the property, or <code>null</code> if the property
     *         does not exist.
     */
    public static String getProperty(String property)
    {
        if (properties == null)
        {
            loadConfig(null);
        }
        String propertyValue = properties.getProperty(property);
        
        if (propertyValue != null)
        {
            propertyValue = propertyValue.trim();
        }
        return propertyValue;
    }


    /**
     * Get a configuration property, with default
     * 
     * @param property
     *            the name of the property
     * 
     * @param defaultValue
     *            value to return if property is not found
     *
     * @return the value of the property, or <code>default</code> if the property
     *         does not exist or is null.
     */
    public static String getProperty(String property, String defaultValue)
    {
        if (properties == null)
        {
            loadConfig(null);
        }
        String propertyValue = properties.getProperty(property);
        
        if (propertyValue != null)
        {
            propertyValue = propertyValue.trim();
        }
        else
        {
            propertyValue = defaultValue;
        }
        return propertyValue;
    }


    /**
     * Get a configuration property as an integer
     * 
     * @param property
     *            the name of the property
     * 
     * @return the value of the property. <code>0</code> is returned if the
     *         property does not exist. To differentiate between this case and
     *         when the property actually is zero, use <code>getProperty</code>.
     */
    public static int getIntProperty(String property)
    {
        return getIntProperty(property, 0);
    }

    /**
     * Get a configuration property as an integer, with default
     * 
     * @param property
     *            the name of the property
     *            
     * @param defaultValue
     *            value to return if property is not found or is not an Integer.
     *
     * @return the value of the property. <code>default</code> is returned if
     *         the property does not exist or is not an Integer. To differentiate between this case
     *         and when the property actually is false, use
     *         <code>getProperty</code>.
     */
    public static int getIntProperty(String property, int defaultValue)
    {
        if (properties == null)
        {
            loadConfig(null);
        }

        String stringValue = properties.getProperty(property);
        int intValue = defaultValue;

        if (stringValue != null)
        {
            try
            {
                intValue = Integer.parseInt(stringValue.trim());
            }
            catch (NumberFormatException e)
            {
                warn("Warning: Number format error in property: " + property);
            }
        }

        return intValue;
    }

    /**
     * Get a configuration property as a long
     *
     * @param property
     *            the name of the property
     *
     * @return the value of the property. <code>0</code> is returned if the
     *         property does not exist. To differentiate between this case and
     *         when the property actually is zero, use <code>getProperty</code>.
     */
    public static long getLongProperty(String property)
    {
        return getLongProperty(property, 0);
    }

    /**
     * Get a configuration property as an long, with default
     *
     * @param property
     *            the name of the property
     *
     * @param defaultValue
     *            value to return if property is not found or is not a Long.
     *
     * @return the value of the property. <code>default</code> is returned if
     *         the property does not exist or is not an Integer. To differentiate between this case
     *         and when the property actually is false, use
     *         <code>getProperty</code>.
     */
    public static long getLongProperty(String property, int defaultValue)
    {
        if (properties == null)
        {
            loadConfig(null);
        }

        String stringValue = properties.getProperty(property);
        long longValue = defaultValue;

        if (stringValue != null)
        {
            try
            {
                longValue = Long.parseLong(stringValue.trim());
            }
            catch (NumberFormatException e)
            {
                warn("Warning: Number format error in property: " + property);
            }
        }

        return longValue;
    }

    /**
     * Get the License
     * 
     * @param
     *         license file name
     *  
     *  @return
     *         license text
     * 
     */
    public static String getLicenseText(String licenseFile)
    {
    // Load in default license

        FileReader fr = null;
        BufferedReader br = null;
        try
        {
            fr = new FileReader(licenseFile);
            br = new BufferedReader(fr);
            String lineIn;
            license = "";
            while ((lineIn = br.readLine()) != null)
            {
                license = license + lineIn + '\n';
            }
        }
        catch (IOException e)
        {
            fatal("Can't load configuration", e);

            // FIXME: Maybe something more graceful here, but with the
           // configuration we can't do anything
            System.exit(1);
        }
        finally
        {
            if (br != null)
                try { br.close(); } catch (IOException ioe) { }

            if (fr != null)
                try { fr.close(); } catch (IOException ioe) { }
        }

        return license;
    }
     

    /**
     * Get a configuration property as a boolean. True is indicated if the value
     * of the property is <code>TRUE</code> or <code>YES</code> (case
     * insensitive.)
     * 
     * @param property
     *            the name of the property
     * 
     * @return the value of the property. <code>false</code> is returned if
     *         the property does not exist. To differentiate between this case
     *         and when the property actually is false, use
     *         <code>getProperty</code>.
     */
    public static boolean getBooleanProperty(String property)
    {
        return getBooleanProperty(property, false);
    }

    /**
     * Get a configuration property as a boolean, with default.
     * True is indicated if the value
     * of the property is <code>TRUE</code> or <code>YES</code> (case
     * insensitive.)
     *
     * @param property
     *            the name of the property
     *
     * @param defaultValue
     *            value to return if property is not found.
     *
     * @return the value of the property. <code>default</code> is returned if
     *         the property does not exist. To differentiate between this case
     *         and when the property actually is false, use
     *         <code>getProperty</code>.
     */
    public static boolean getBooleanProperty(String property, boolean defaultValue)
    {
        if (properties == null)
        {
            loadConfig(null);
        }

        String stringValue = properties.getProperty(property);

        if (stringValue != null)
        {
            stringValue = stringValue.trim();
            return  stringValue.equalsIgnoreCase("true") ||
                    stringValue.equalsIgnoreCase("yes");
        }
        else
        {
            return defaultValue;
        }
    }

    /**
     * Returns an enumeration of all the keys in the Djatoka configuration
     * 
     * @return an enumeration of all the keys in the Djatoka configuration
     */
    public static Enumeration propertyNames()
    {
        if (properties == null)
            loadConfig(null);

        return properties.propertyNames();
    }


    /**
     * Writes license to a text file.
     * 
     * @param news
     *            the text to be written to the file.
     */
    public static void writeLicenseFile(String licenseFile, String newLicense)
    {
        try
        {
            // write the news out to the appropriate file
            FileOutputStream fos = new FileOutputStream(licenseFile);
            OutputStreamWriter osr = new OutputStreamWriter(fos, "UTF-8");
            PrintWriter out = new PrintWriter(osr);
            out.print(newLicense);
            out.close();
        }
        catch (IOException e)
        {
            warn("license_write: " + e.getLocalizedMessage());
        }

        license = newLicense;
     }

    private static File loadedFile = null;

    /**
     * Load the Djatoka configuration properties. Only does anything if
     * properties are not already loaded. Properties are loaded in from the
     * specified file, or default locations.
     * 
     * @param configFile
     *            The <code>djatoka.cfg</code> configuration file to use, or
     *            <code>null</code> to try default locations
     */
    public static synchronized void loadConfig(String configFile)
    {
        
        if (properties != null)
        {
            return;
        }


        URL url = null;
        
        InputStream is = null;
        try
        {
            String configProperty = null;
            try
            {
                configProperty = System.getProperty("djatoka.configuration");
            }
            catch (SecurityException se)
            {
                // A security manager may stop us from accessing the system properties.
                // This isn't really a fatal error though, so catch and ignore
                log.warn("Unable to access system properties, ignoring.", se);
            }

            if (configFile != null)
            {
                info("Loading provided config file: " + configFile);
                
                loadedFile = new File(configFile);
                url = loadedFile.toURL();
                
            }
            // Has the default configuration location been overridden?
            else if (configProperty != null)
            {
                info("Loading system provided config property (-Ddjatoka.configuration): " + configProperty);
                
                // Load the overriding configuration
                loadedFile = new File(configProperty);
                url = loadedFile.toURL();
            }
            // Load configuration from default location
            else
            {
                url = ConfigurationManager.class.getResource("/djatoka.cfg");
                if (url != null)
                {
                    info("Loading from classloader: " + url);
                    
                    loadedFile = new File(url.getPath());
                }
            }
            
            if (url == null)
            {
                fatal("Cannot find djatoka.cfg");
                throw new RuntimeException("Cannot find djatoka.cfg");
            }
            else
            {
                properties = new Properties();
                is = url.openStream();
                properties.load(is);

                // walk values, interpolating any embedded references.
                for (Enumeration pe = properties.propertyNames(); pe.hasMoreElements(); )
                {
                    String key = (String)pe.nextElement();
                    String value = interpolate(key, 1);
                    if (value != null)
                        properties.setProperty(key, value);
                }
            }

        }
        catch (IOException e)
        {
            fatal("Can't load configuration: " + url, e);

            // FIXME: Maybe something more graceful here, but with the
            // configuration we can't do anything
            throw new RuntimeException("Cannot load configuration: " + url, e);
        }
        finally
        {
            if (is != null)
                try { is.close(); } catch (IOException ioe) { }
        }

        try
        {
            /*
             * Initialize Logging once ConfigurationManager is initialized.
             * 
             * This is selection from a property in djatoka.cfg, if the property
             * is absent then nothing will be configured and the application
             * will use the defaults provided by log4j. 
             * 
             * Property format is:
             * 
             * log.init.config = ${djatoka.dir}/config/log4j.properties
             * or
             * log.init.config = ${djatoka.dir}/config/log4j.xml
             * 
             * See default log4j initialization documentation here:
             * http://logging.apache.org/log4j/docs/manual.html
             * 
             * If there is a problem with the file referred to in
             * "log.configuration" it needs to be sent to System.err
             * so do not instantiate another Logging configuration.
             *
             */
            String dsLogConfiguration = ConfigurationManager.getProperty("log.init.config");

            if (dsLogConfiguration == null ||  System.getProperty("djatoka.log.init.disable") != null)
            {
                /* 
                 * Do nothing if log config not set in djatoka.cfg or "djatoka.log.init.disable" 
                 * system property set.  Leave it upto log4j to properly init its logging 
                 * via classpath or system properties.
                 */
                info("Using default log4j provided log configuration," +
                        "if uninitended, check your djatoka.cfg for (log.init.config)");
            }
            else
            {
                info("Using djatoka provided log configuration (log.init.config)");
                
                
                File logConfigFile = new File(dsLogConfiguration);
                
                if(logConfigFile.exists())
                {
                    info("Loading: " + dsLogConfiguration);
                    
                    OptionConverter.selectAndConfigure(logConfigFile.toURL(), null,
                            org.apache.log4j.LogManager.getLoggerRepository());
                }
                else
                {
                    info("File does not exist: " + dsLogConfiguration);
                }
            }

        }
        catch (MalformedURLException e)
        {
            fatal("Can't load djatoka provided log4j configuration", e);
            throw new RuntimeException("Cannot load djatoka provided log4j configuration",e);
        }
        
    }

    /**
     * Recursively interpolate variable references in value of
     * property named "key".
     * @return new value if it contains interpolations, or null
     *   if it had no variable references.
     */
    private static String interpolate(String key, int level)
    {
        if (level > RECURSION_LIMIT)
            throw new IllegalArgumentException("ConfigurationManager: Too many levels of recursion in configuration property variable interpolation, property="+key);
        String value = (String)properties.get(key);
        int from = 0;
        StringBuffer result = null;
        while (from < value.length())
        {
            int start = value.indexOf("${", from);
            if (start >= 0)
            {
                int end = value.indexOf("}", start);
                if (end < 0)
                    break;
                String var = value.substring(start+2, end);
                if (result == null)
                    result = new StringBuffer(value.substring(from, start));
                else
                    result.append(value.substring(from, start));
                if (properties.containsKey(var))
                {
                    String ivalue = interpolate(var, level+1);
                    if (ivalue != null)
                    {
                        result.append(ivalue);
                        properties.setProperty(var, ivalue);
                    }
                    else
                        result.append((String)properties.getProperty(var));
                }
                else
                {
                    log.warn("Interpolation failed in value of property \""+key+
                             "\", there is no property named \""+var+"\"");
                }
                from = end+1;
            }
            else
                break;
        }
        if (result != null && from < value.length())
            result.append(value.substring(from));
        return (result == null) ? null : result.toString();
    }

    /**
     * Command-line interface for running configuration tasks. Possible
     * arguments:
     * <ul>
     * <li><code>-property name</code> prints the value of the property
     * <code>name</code> from <code>djatoka.cfg</code> to the standard
     * output. If the property does not exist, nothing is written.</li>
     * </ul>
     * 
     * @param argv
     *            command-line arguments
     */
    public static void main(String[] argv)
    {
        if ((argv.length == 2) && argv[0].equals("-property"))
        {
            String val = getProperty(argv[1]);

            if (val != null)
            {
                System.out.println(val);
            }
            else
            {
                System.out.println("");
            }

            System.exit(0);
        }
        else
        {
            System.err
                    .println("Usage: ConfigurationManager OPTION\n  -property prop.name  get value of prop.name from djatoka.cfg");
        }

        System.exit(1);
    }
    
    private static void info(String string)
    {
        if (!isLog4jConfigured())
        {
            System.out.println("INFO: " + string);
        }
        else
        {
            log.info(string);
        }
    }

    private static void warn(String string)
    {
        if (!isLog4jConfigured())
        {
            System.out.println("WARN: " + string);
        }
        else
        {
            log.warn(string);
        }
    }

    private static void fatal(String string, Exception e)
    {
        if (!isLog4jConfigured())
        {
            System.out.println("FATAL: " + string);
            e.printStackTrace();
        }
        else
        {
            log.fatal(string, e);
        }
    }

    private static void fatal(String string)
    {
        if (!isLog4jConfigured())
        {
            System.out.println("FATAL: " + string);
        }
        else
        {
            log.fatal(string);
        }
    }

    /*
     * Only current solution available to detect 
     * if log4j is truly configured. 
     */
    private static boolean isLog4jConfigured()
    {
        Enumeration en = org.apache.log4j.LogManager.getRootLogger()
                .getAllAppenders();

        if (!(en instanceof org.apache.log4j.helpers.NullEnumeration))
        {
            return true;
        }
        else
        {
            Enumeration cats = Category.getCurrentCategories();
            while (cats.hasMoreElements())
            {
                Category c = (Category) cats.nextElement();
                if (!(c.getAllAppenders() instanceof org.apache.log4j.helpers.NullEnumeration))
                    return true;
            }
        }
        return false;
    }

}
