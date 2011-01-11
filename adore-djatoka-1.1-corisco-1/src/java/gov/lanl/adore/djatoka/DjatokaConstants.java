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

/**
 * Default values from djatoka.properties. As detailed below, many of these
 * default values are derived from the Kakadu Library usage examples. The slope
 * method is used by default as it ensures a compression ratio appropriate for
 * the content (e.g illuminated manuscript) and balances quality with
 * compression.
 * <p/>
 * Excerpts from Kakadu Usage_Examples.txt from which many of the default values
 * were obtained:
 * <p/>
 * kdu_compress -i massive.ppm -o massive.jp2 -rate -,0.001 Clayers=28
 * Creversible=yes Clevels=8 Corder=PCRL ORGgen_plt=yes
 * Cprecincts={256,256},{256,256},{128,128},{64,128},{32,128},
 * {16,128},{8,128},{4,128},{2,128} -flush_period 1024 
 * <p/>
 * You might use this type of command to compress a really massive image, 
 * e.g. 64Kx64K or larger, without requiring the use of tiles. The code-stream is 
 * incrementally flushed out using the `-flush_period' argument to indicate that 
 * an attempt should be made to apply incremental rate control procedures and flush 
 * as much of the generated data to the output file as possible, roughly every 1024 
 * lines. The result is that you will only need about 1000*L bytes of memory to perform 
 * all relevant processing and code-stream management, where L is the image width.
 * It follows that a computer with 256MBytes of RAM could losslessly an image
 * measuring as much as 256Kx256K without resorting to vertical tiling. The
 * resulting code-stream can be efficiently served up to a remote client using
 * `kdu_server'.
 * 
 * kdu_transcode -i in.j2c -o out.j2c Cprecincts={128,128} Corder=RPCL
 * ORGgen_plt=yes You can use something like this to create a new code-stream
 * with all the information of the original, but having an organization (and
 * pointer marker segments) which will enable random access into the code-stream
 * during interactive rendering. The introduction of precincts, PLT marker
 * segments, and a "layer-last" progression sequence such as RPCL, PCRL or CPRL,
 * can also improve the memory efficiency of the "kdu_server" application when
 * used to serve up a very large image to a remote client.
 * 
 * If the image is small to moderate in size (say up to 1K by 1K), it is
 * recommended that you compress the original image using 32x32 code-blocks
 * (Cblk={32,32}) instead of the default 64x64 code-blocks. This can be helpful
 * even for very large images, but if the original uncompressed image size is on
 * the order of 1 Gigabyte or more, larger code-blocks will help reduce the
 * internal state memory resources which the server must dedicate to the client
 * connection.
 * @author Ryan Chute
 */
	
public interface DjatokaConstants {

	/**
	 * Absolute Compression Rate.
	 * Default Value: "1.0"
	 */
	public static final String DEFAULT_RATE = "1.0";

	/**
	 * Relative Compression Slope used to generate a compression level per layer.
	 * Default Value: "51651,51337,51186,50804,50548,50232"
	 */
	public static final String DEFAULT_SLOPE = "51651,51337,51186,50804,50548,50232";

	/**
	 * Number of DWT Levels (i.e. resolutions)
	 * Default Value: "0". 0 indicates application should determine value.
	 */
	public static final int DEFAULT_CLEVELS = 0;

	/**
	 * Number of quality layers, must correspond with number of slope values
	 * Default Value: "6"
	 */
	public static final int DEFAULT_CLAYERS = 6;

	/**
	 * Boolean indicator of whether or not a reversible compression wavelet 
	 * should be used. Default Value: true
	 */
	public static final boolean DEFAULT_USE_REVERSIBLE = true;

	/**
	 * Defines the size of the layer packets.  Default values improve random
	 * access performance. Default Value: "{256,256},{256,256},{128,128}"
	 */
	public static final String DEFAULT_CPRECINCTS = "{256,256},{256,256},{128,128}";

	/**
	 * Preferred progression order. Default values improve resolution access
	 * performance. Default Value: "RPCL"
	 */
	public static final String DEFAULT_CORDER = "RPCL";

	/**
	 * Boolean indicator of whether or not packet organization header info 
	 * should be included. Default Value: true
	 */
	public static final boolean DEFAULT_ORGGEN_PLT = true;

	/**
	 * Preferred order of header information. Default values improve resolution access
	 * performance. Default Value: "R"
	 */
	public static final String DEFAULT_ORGTPARTS = "R";

	/**
	 * Defines default codeblock size.
	 * Default Value: "{32,32}"
	 */
	public static final String DEFAULT_CBLK = "{32,32}";

	/**
	 * Defined the file name of the default compression props file.
	 * Default Value: "djatoka.properties"
	 */
	public static final String DEFAULT_JPEG2K_PROP_FILENAME = "djatoka.properties";
}
