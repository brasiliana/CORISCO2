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

package gov.lanl.adore.djatoka;

import java.io.File;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;

import gov.lanl.adore.djatoka.kdu.KduCompressExe;
import gov.lanl.adore.djatoka.util.IOUtils;
import gov.lanl.adore.djatoka.util.SourceImageFileFilter;

/**
 * Compression Application
 * @author Ryan Chute
 *
 */
public class DjatokaCompress {
	static Logger logger = Logger.getLogger(DjatokaCompress.class);
	/**
	 * Uses apache commons cli to parse input args. Passes parsed
	 * parameters to ICompress implementation.
	 * @param args command line parameters to defined input,output,etc.
	 */
	public static void main(String[] args) {
		// create the command line parser
		CommandLineParser parser = new PosixParser();

		// create the Options
		Options options = new Options();
		options.addOption( "i", "input", true, "Filepath of the input file or dir." );
		options.addOption( "o", "output", true, "Filepath of the output file or dir." );
		options.addOption( "r", "rate", true, "Absolute Compression Ratio" );
		options.addOption( "s", "slope", true, "Used to generate relative compression ratio based on content characteristics." );
		options.addOption( "y", "Clayers", true, "Number of quality levels." );
		options.addOption( "l", "Clevels", true, "Number of DWT levels (reolution levels)." );
		options.addOption( "v", "Creversible", true, "Use Reversible Wavelet" );
		options.addOption( "c", "Cprecincts", true, "Precinct dimensions" );
		options.addOption( "p", "props", true, "Compression Properties File" );
		options.addOption( "d", "Corder", true, "Progression order" );
		options.addOption( "g", "ORGgen_plt", true, "Enables insertion of packet length information in the header" );
		options.addOption( "t", "ORGtparts", true, "Division of each tile's packets into tile-parts" );
		options.addOption( "b", "Cblk", true, "Codeblock Size" );
		options.addOption( "a", "AltImpl", true, "Alternate ICompress Implemenation" );
		
		try {
			if (args.length == 0) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("gov.lanl.adore.djatoka.DjatokaCompress", options);
				System.exit(0);
			}
			
		    // parse the command line arguments
		    CommandLine line = parser.parse(options, args);
		    String input = line.getOptionValue("i");
		    String output = line.getOptionValue("o");
		    
		    String propsFile = line.getOptionValue("p");
		    DjatokaEncodeParam p;
		    if (propsFile != null){
		    	Properties props = IOUtils.loadConfigByPath(propsFile);
		    	p = new DjatokaEncodeParam(props);
		    } else
		    	p = new DjatokaEncodeParam();
		    String rate = line.getOptionValue("r");
		    if (rate != null)
		    	p.setRate(rate);
		    String slope = line.getOptionValue("s");
		    if (slope != null)
		    	p.setSlope(slope);
		    String Clayers = line.getOptionValue("y");
		    if (Clayers != null)
		    	p.setLayers(Integer.parseInt(Clayers));
		    String Clevels = line.getOptionValue("l");
		    if (Clevels != null)
		    	p.setLevels(Integer.parseInt(Clevels));
		    String Creversible = line.getOptionValue("v");
		    if (Creversible != null)
		    	p.setUseReversible(Boolean.parseBoolean(Creversible));
		    String Cprecincts = line.getOptionValue("c");
		    if (Cprecincts != null)
		    	p.setPrecincts(Cprecincts);
		    String Corder = line.getOptionValue("d");
		    if (Corder != null)
		    	p.setProgressionOrder(Corder);
		    String ORGgen_plt = line.getOptionValue("g");
		    if (ORGgen_plt != null)
		    	p.setInsertPLT(Boolean.parseBoolean(ORGgen_plt));
		    String Cblk = line.getOptionValue("b");
		    if (Cblk != null)
		    	p.setCodeBlockSize(Cblk);
		    String alt = line.getOptionValue("a");
		    
			ICompress jp2 = new KduCompressExe();
			if (alt != null)
				jp2 = (ICompress) Class.forName(alt).newInstance();
			if (new File(input).isDirectory() && new File(output).isDirectory()) {
				ArrayList<File> files = IOUtils.getFileList(input, new SourceImageFileFilter(), false);
				for (File f : files) {
				    long x = System.currentTimeMillis();
				    File outFile = new File(output, f.getName().substring(0, f.getName().indexOf(".")) + ".jp2");
				    compress(jp2, f.getAbsolutePath(), outFile.getAbsolutePath(), p);
			        report(f.getAbsolutePath(), x);
				}
			} else {
				long x = System.currentTimeMillis();
		    	File f = new File(input);
			    if (output == null)
			    	output = f.getName().substring(0, f.getName().indexOf(".")) + ".jp2";
			    if (new File(output).isDirectory())
			    	output = output + f.getName().substring(0, f.getName().indexOf(".")) + ".jp2";
			    compress(jp2, input, output, p);
			    report(input, x);
			}
		} catch( ParseException e ) {
		    logger.error( "Parse exception:" + e.getMessage(), e );
		} catch (DjatokaException e) {
			logger.error( "djatoka Compression exception:" + e.getMessage(), e );
		} catch (InstantiationException e) {
			logger.error( "Unable to initialize alternate implemenation:" + e.getMessage(), e );
		} catch (Exception e) {
			logger.error( "An exception occured:" + e.getMessage(), e );
		}
	}
	
	/**
	 * Print time, in seconds, to process resource
	 * @param id Identifier or File Path to indicate processing resource
	 * @param x System time in milliseconds when resource processing started
	 */
	public static void report(String id, long x) {
		logger.info("Compression Time: " + ((double) (System.currentTimeMillis() - x) / 1000) + " seconds for " + id);
	}
	
	/**
	 * Simple compress wrapper to catch exceptions, useful when 
	 * @param jp2
	 * @param input
	 * @param output
	 * @param p
	 */
	public static void compress(ICompress jp2, String input, String output, DjatokaEncodeParam p) {
		try {
			jp2.compressImage(input, output, p);
		} catch (DjatokaException e) {
			logger.error("djatoka Compression exception:" + e.getMessage(), e);
		} 
	}
}
