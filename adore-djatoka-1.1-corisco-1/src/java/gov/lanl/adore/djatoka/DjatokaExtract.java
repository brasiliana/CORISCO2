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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;

import gov.lanl.adore.djatoka.kdu.KduExtractExe;

/**
 * Extraction Application
 * @author Ryan Chute
 *
 */
public class DjatokaExtract {
	static Logger logger = Logger.getLogger(DjatokaExtract.class);
	/**
	 * Uses apache commons cli to parse input args. Passes parsed
	 * parameters to IExtract implementation.
	 * @param args command line parameters to defined input,output,etc.
	 */
	public static void main(String[] args) {
		// create the command line parser
		CommandLineParser parser = new PosixParser();

		// create the Options
		Options options = new Options();
		options.addOption( "i", "input", true, "Filepath of the input file." );
		options.addOption( "o", "output", true, "Filepath of the output file." );
		options.addOption( "l", "level", true, "Resolution level to extract." );
		options.addOption( "d", "reduce", true, "Resolution levels to subtract from max resolution." );
		options.addOption( "r", "region", true, "Format: Y,X,H,W. " );
		options.addOption( "c", "cLayer", true, "Compositing Layer Index." );
		options.addOption( "s", "scale", true, "Format: Option 1. Define a long-side dimension (e.g. 96); Option 2. Define absolute w,h values (e.g. 1024,768); Option 3. Define a single dimension (e.g. 1024,0) with or without Level Parameter; Option 4. Use a single decimal scaling factor (e.g. 0.854)");
		options.addOption( "t", "rotate", true, "Number of degrees to rotate image (i.e. 90, 180, 270)." );
		options.addOption( "f", "format", true, "Mimetype of the image format to be provided as response. Default: image/jpeg" );
		options.addOption( "a", "AltImpl", true, "Alternate IExtract Implemenation" );
		
		try {
			if (args.length == 0) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("gov.lanl.adore.djatoka.DjatokaExtract", options);
				System.exit(0);
			}
			
		    // parse the command line arguments
		    CommandLine line = parser.parse(options, args);
		    String input = line.getOptionValue("i");
		    String output = line.getOptionValue("o");
		    
		    DjatokaDecodeParam p = new DjatokaDecodeParam();
		    String level = line.getOptionValue("l");
		    if (level != null)
		    	p.setLevel(Integer.parseInt(level));
		    String reduce = line.getOptionValue("d");
		    if (level == null && reduce != null)
		    	p.setLevelReductionFactor(Integer.parseInt(reduce));
		    String region = line.getOptionValue("r");
		    if (region != null)
		    	p.setRegion(region);
		    String cl = line.getOptionValue("c");
		    if (cl != null) {
				int clayer = Integer.parseInt(cl);
				if (clayer > 0)
				    p.setCompositingLayer(clayer);
		    }
		    String scale = line.getOptionValue("s");
		    if (scale != null) {
				String[] v = scale.split(",");
				if (v.length == 1) {
					if (v[0].contains("."))
						p.setScalingFactor(Double.parseDouble(v[0]));
					else {
						int[] dims = new int[]{-1,Integer.parseInt(v[0])};
						p.setScalingDimensions(dims);
					}
				} else if (v.length == 2) {
					int[] dims = new int[]{Integer.parseInt(v[0]),Integer.parseInt(v[1])};
					p.setScalingDimensions(dims);
				}
		    }
		    String rotate = line.getOptionValue("t");
		    if (rotate != null)
		    	p.setRotationDegree(Integer.parseInt(rotate));
		    String format = line.getOptionValue("f");
		    if (format == null)
		    	format = "image/jpeg";
		    String alt = line.getOptionValue("a");
		    if(output == null)
		    	output = input + ".jpg";
		    
			long x = System.currentTimeMillis();
			IExtract ex = null;
			if (alt != null) {
                System.out.println("alt: " + alt);
				ex = (IExtract) Class.forName(alt).newInstance();
            } else {
                 ex = new KduExtractExe();
            }
			DjatokaExtractProcessor e = new DjatokaExtractProcessor(ex);
			e.extractImage(input, output, p, format);
			logger.info("Extraction Time: " + ((double) (System.currentTimeMillis() - x) / 1000) + " seconds");
		    
		} catch( ParseException e ) {
			logger.error( "Parse exception:" + e.getMessage(), e);
		} catch (DjatokaException e) {
			logger.error( "djatoka Extraction exception:" + e.getMessage(), e);
		} catch (InstantiationException e) {
			logger.error( "Unable to initialize alternate implemenation:" + e.getMessage(), e);
		} catch (Exception e) {
			logger.error( "Unexpected exception:" + e.getMessage(), e);
		}
	}
}
