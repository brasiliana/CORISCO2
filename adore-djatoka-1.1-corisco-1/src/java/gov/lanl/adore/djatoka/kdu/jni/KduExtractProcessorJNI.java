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

package gov.lanl.adore.djatoka.kdu.jni;

import gov.lanl.adore.djatoka.DjatokaDecodeParam;
import gov.lanl.adore.djatoka.DjatokaException;
import gov.lanl.adore.djatoka.util.IOUtils;
import gov.lanl.adore.djatoka.util.ImageProcessingUtils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.StringTokenizer;

import kdu_jni.Jp2_family_src;
import kdu_jni.Jp2_locator;
import kdu_jni.Jp2_source;
import kdu_jni.Jpx_source;
import kdu_jni.KduException;
import kdu_jni.Kdu_channel_mapping;
import kdu_jni.Kdu_codestream;
import kdu_jni.Kdu_compositor_buf;
import kdu_jni.Kdu_compressed_source;
import kdu_jni.Kdu_coords;
import kdu_jni.Kdu_dims;
import kdu_jni.Kdu_global;
import kdu_jni.Kdu_region_compositor;
import kdu_jni.Kdu_region_decompressor;
import kdu_jni.Kdu_simple_file_source;

/**
 * Uses Kakadu Java Native Interface to extract regions. This implementation
 * is provided for reference purposes only.  There are serious issues related 
 * to multi-threading and byte buffers when pushing this implementation. This
 * implementation should be used for experimental purposes only.  The kdu_expand
 * bridge method is far more scalable and memory efficient.
 * @author Ryan Chute
 *
 */
public class KduExtractProcessorJNI {

	static {
		System.loadLibrary("kdu_jni");
	}
	
	private String sourceFile;
	private InputStream is;
	
	private DjatokaDecodeParam params = null;

	public KduExtractProcessorJNI(String sourceFile) {
		this.sourceFile = sourceFile;
	}

	public KduExtractProcessorJNI(String sourceFile, DjatokaDecodeParam params) {
		this(sourceFile);
		this.params = params;
	}

	public KduExtractProcessorJNI(InputStream is, DjatokaDecodeParam params) {
		this.is = is;
		this.params = params;
	}
	
	public DjatokaDecodeParam getDjatokaDecodeParam() {
		return params;
	}

	public void setDjatokaDecodeParam(DjatokaDecodeParam params) {
		this.params = params;
	}

	public BufferedImage extractUsingCompositor() throws IOException, DjatokaException {
		boolean useRegion = false;
		int left = 0;
		int top = 0;
		int width = 50;
		int height = 50;
		boolean useleftDouble = false;
		Double leftDouble = 0.0;
		boolean usetopDouble = false;
		Double topDouble = 0.0;
		boolean usewidthDouble = false;
		Double widthDouble = 0.0;
		boolean useheightDouble = false;
		Double heightDouble = 0.0;
		
		if (params.getRegion() != null) {
			StringTokenizer st = new StringTokenizer(params.getRegion(), "{},");
			String token;
			// top
			if ((token=st.nextToken()).contains(".")) {
				topDouble = Double.parseDouble(token);
				usetopDouble = true;
			} else
				top = Integer.parseInt(token);
			// left
			if ((token=st.nextToken()).contains(".")) {
				leftDouble = Double.parseDouble(token);
				useleftDouble = true;
			} else
				left = Integer.parseInt(token);
			// height
			if ((token=st.nextToken()).contains(".")) {
				heightDouble = Double.parseDouble(token);
				useheightDouble = true;
			} else
				height = Integer.parseInt(token);
			// width
			if ((token=st.nextToken()).contains(".")) {
				widthDouble = Double.parseDouble(token);
				usewidthDouble = true;
			} else
				width = Integer.parseInt(token);
			
			useRegion = true;
		}

		if (is != null) {
			File f = File.createTempFile("tmp", ".jp2");
			f.deleteOnExit();
			FileOutputStream fos = new FileOutputStream(f);
			sourceFile = f.getAbsolutePath();
			IOUtils.copyStream(is, fos);
		}

		Kdu_simple_file_source raw_src = null; // Must be disposed last
		Jp2_family_src family_src = new Jp2_family_src(); // Dispose last
		Jpx_source wrapped_src = new Jpx_source(); // Dispose in the middle
		Kdu_region_compositor compositor = null; // Must be disposed first
		BufferedImage image = null;

		try {
			family_src.Open(sourceFile);
			int success = wrapped_src.Open(family_src, true);
			if (success < 0) {
				family_src.Close();
				wrapped_src.Close();
				raw_src = new Kdu_simple_file_source(sourceFile);
			}

			compositor = new Kdu_region_compositor();
			if (raw_src != null)
				compositor.Create(raw_src);
			else
				compositor.Create(wrapped_src);

			Kdu_dims imageDimensions = new Kdu_dims();
			compositor.Get_total_composition_dims(imageDimensions);
			Kdu_coords imageSize = imageDimensions.Access_size();
			Kdu_coords imagePosition = imageDimensions.Access_pos();

			if (useleftDouble)
				left = imagePosition.Get_x() + (int) Math.round(leftDouble * imageSize.Get_x());
			if (usetopDouble)
				top = imagePosition.Get_y() + (int) Math.round(topDouble * imageSize.Get_y());
			if (useheightDouble)
				height = (int) Math.round(heightDouble * imageSize.Get_y());
			if (usewidthDouble)
				width = (int) Math.round(widthDouble * imageSize.Get_x());

			if (useRegion) {
				imageSize.Set_x(width);
				imageSize.Set_y(height);
				imagePosition.Set_x(left);
				imagePosition.Set_y(top);
			}

			int reduce = 1 << params.getLevelReductionFactor();
			imageSize.Set_x(imageSize.Get_x());
			imageSize.Set_y(imageSize.Get_y());
			imagePosition.Set_x(imagePosition.Get_x() / reduce - (1 / reduce - 1) / 2);
			imagePosition.Set_y(imagePosition.Get_y() / reduce - (1 / reduce - 1) / 2);

			Kdu_dims viewDims = new Kdu_dims();
			viewDims.Assign(imageDimensions);
			viewDims.Access_size().Set_x(imageSize.Get_x());
			viewDims.Access_size().Set_y(imageSize.Get_y());
			compositor.Add_compositing_layer(0, viewDims, viewDims);

			if (params.getRotationDegree() == 90)
				compositor.Set_scale(true, false, true, 1.0F);
			else if (params.getRotationDegree() == 180)
				compositor.Set_scale(false, true, true, 1.0F);
			else if (params.getRotationDegree() == 270)
				compositor.Set_scale(true, true, false, 1.0F);
			else 
			    compositor.Set_scale(false, false, false, 1.0F);

			compositor.Get_total_composition_dims(viewDims);
			Kdu_coords viewSize = viewDims.Access_size();
			compositor.Set_buffer_surface(viewDims);

			int[] imgBuffer = new int[viewSize.Get_x() * viewSize.Get_y()];
			Kdu_compositor_buf compositorBuffer = compositor.Get_composition_buffer(viewDims);
			int regionBufferSize = 0;
			int[] kduBuffer = null;
			Kdu_dims newRegion = new Kdu_dims();
			while (compositor.Process(100000, newRegion)) {
				Kdu_coords newOffset = newRegion.Access_pos();
				Kdu_coords newSize = newRegion.Access_size();
				newOffset.Subtract(viewDims.Access_pos());

				int newPixels = newSize.Get_x() * newSize.Get_y();
				if (newPixels == 0)
					continue;
				if (newPixels > regionBufferSize) {
					regionBufferSize = newPixels;
					kduBuffer = new int[regionBufferSize];
				}

				compositorBuffer.Get_region(newRegion, kduBuffer);
				int imgBuffereIdx = newOffset.Get_x() + newOffset.Get_y() * viewSize.Get_x();
				int kduBufferIdx = 0;
				int xDiff = viewSize.Get_x() - newSize.Get_x();
				for (int j = 0; j < newSize.Get_y(); j++, imgBuffereIdx += xDiff) {
					for (int i = 0; i < newSize.Get_x(); i++) {
						imgBuffer[imgBuffereIdx++] = kduBuffer[kduBufferIdx++];
					}
				}
			}
			if (params.getRotationDegree() == 90 || params.getRotationDegree() == 270)
			    image = new BufferedImage(imageSize.Get_y(), imageSize.Get_x(), BufferedImage.TYPE_INT_RGB);
			else
				image = new BufferedImage(imageSize.Get_x(), imageSize.Get_y(), BufferedImage.TYPE_INT_RGB);
			image.setRGB(0, 0, viewSize.Get_x(), viewSize.Get_y(), imgBuffer, 0, viewSize.Get_x());

			if (compositor != null)
				compositor.Native_destroy();
			wrapped_src.Native_destroy();
			family_src.Native_destroy();
			if (raw_src != null)
				raw_src.Native_destroy();
			
			return image;
		} catch (KduException e) {
			e.printStackTrace();
			throw new DjatokaException(e);
		} catch (Exception e) {
			e.printStackTrace();
			throw new DjatokaException(e);
		}
	}
	
	public BufferedImage extract() throws DjatokaException {
		boolean useRegion = false;
		int left = 0;
		int top = 0;
		int width = 50;
		int height = 50;
		boolean useleftDouble = false;
		Double leftDouble = 0.0;
		boolean usetopDouble = false;
		Double topDouble = 0.0;
		boolean usewidthDouble = false;
		Double widthDouble = 0.0;
		boolean useheightDouble = false;
		Double heightDouble = 0.0;
		
		if (params.getRegion() != null) {
			StringTokenizer st = new StringTokenizer(params.getRegion(), "{},");
			String token;
			// top
			if ((token=st.nextToken()).contains(".")) {
				topDouble = Double.parseDouble(token);
				usetopDouble = true;
			} else
				top = Integer.parseInt(token);
			// left
			if ((token=st.nextToken()).contains(".")) {
				leftDouble = Double.parseDouble(token);
				useleftDouble = true;
			} else
				left = Integer.parseInt(token);
			// height
			if ((token=st.nextToken()).contains(".")) {
				heightDouble = Double.parseDouble(token);
				useheightDouble = true;
			} else
				height = Integer.parseInt(token);
			// width
			if ((token=st.nextToken()).contains(".")) {
				widthDouble = Double.parseDouble(token);
				usewidthDouble = true;
			} else
				width = Integer.parseInt(token);
			
			useRegion = true;
		}
		
		try {
			if (is != null) {
				File f = File.createTempFile("tmp", ".jp2");
				f.deleteOnExit();
				FileOutputStream fos = new FileOutputStream(f);
				sourceFile = f.getAbsolutePath();
				IOUtils.copyStream(is, fos);
				is.close();
				fos.close();
			}
		} catch (IOException e) {
			throw new DjatokaException(e);
		}
		
		try {
			Jp2_source inputSource = new Jp2_source();
			Kdu_compressed_source input = null;
			Jp2_family_src jp2_family_in = new Jp2_family_src();
			Jp2_locator loc = new Jp2_locator();
			jp2_family_in.Open(sourceFile, true);
			inputSource.Open(jp2_family_in, loc);
			inputSource.Read_header();
			input = inputSource;

			Kdu_codestream codestream = new Kdu_codestream();
			codestream.Create(input);
			Kdu_channel_mapping channels = new Kdu_channel_mapping();

			if (inputSource.Exists())
				channels.Configure(inputSource, false);
			else
				channels.Configure(codestream);
			int ref_component = channels.Get_source_component(0);
			Kdu_coords ref_expansion = getReferenceExpansion(ref_component, channels, codestream);
			Kdu_dims image_dims = new Kdu_dims();
			codestream.Get_dims(ref_component, image_dims);
			Kdu_coords imageSize = image_dims.Access_size();
			Kdu_coords imagePosition = image_dims.Access_pos();

			if (useleftDouble)
				left = imagePosition.Get_x() + (int) Math.round(leftDouble * imageSize.Get_x());
			if (usetopDouble)
				top = imagePosition.Get_y() + (int) Math.round(topDouble * imageSize.Get_y());
			if (useheightDouble)
				height = (int) Math.round(heightDouble * imageSize.Get_y());
			if (usewidthDouble)
				width = (int) Math.round(widthDouble * imageSize.Get_x());

			if (useRegion) {
				imageSize.Set_x(width);
				imageSize.Set_y(height);
				imagePosition.Set_x(left);
				imagePosition.Set_y(top);
			}

			int reduce = 1 << params.getLevelReductionFactor();
			imageSize.Set_x(imageSize.Get_x() * ref_expansion.Get_x());
			imageSize.Set_y(imageSize.Get_y() * ref_expansion.Get_y());
			imagePosition.Set_x(imagePosition.Get_x() * ref_expansion.Get_x() / reduce - ((ref_expansion.Get_x() / reduce - 1) / 2));
			imagePosition.Set_y(imagePosition.Get_y() * ref_expansion.Get_y() / reduce - ((ref_expansion.Get_y() / reduce - 1) / 2));

			Kdu_dims view_dims = new Kdu_dims();
			view_dims.Assign(image_dims);
			view_dims.Access_size().Set_x(imageSize.Get_x());
			view_dims.Access_size().Set_y(imageSize.Get_y());

			int region_buf_size = imageSize.Get_x() * imageSize.Get_y();
			int[] region_buf = new int[region_buf_size];
			Kdu_region_decompressor decompressor = new Kdu_region_decompressor();
			decompressor.Start(codestream, channels, -1, params.getLevelReductionFactor(), 16384, image_dims,
					ref_expansion, new Kdu_coords(1, 1), false, Kdu_global.KDU_WANT_OUTPUT_COMPONENTS);
		
			Kdu_dims new_region = new Kdu_dims();
			Kdu_dims incomplete_region = new Kdu_dims();
			Kdu_coords viewSize = view_dims.Access_size();
			incomplete_region.Assign(image_dims);

			int[] imgBuffer = new int[viewSize.Get_x() * viewSize.Get_y()];
			int[] kduBuffer = null;
			while (decompressor.Process(region_buf, image_dims.Access_pos(), 0, 0, region_buf_size, incomplete_region, new_region)) {
				Kdu_coords newOffset = new_region.Access_pos();
				Kdu_coords newSize = new_region.Access_size();
				newOffset.Subtract(view_dims.Access_pos());

				kduBuffer = region_buf;
				int imgBuffereIdx = newOffset.Get_x() + newOffset.Get_y() * viewSize.Get_x();
				int kduBufferIdx = 0;
				int xDiff = viewSize.Get_x() - newSize.Get_x();
				for (int j = 0; j < newSize.Get_y(); j++, imgBuffereIdx += xDiff) {
					for (int i = 0; i < newSize.Get_x(); i++) {
						imgBuffer[imgBuffereIdx++] = kduBuffer[kduBufferIdx++];
					}
				}
			}

			BufferedImage image = new BufferedImage(imageSize.Get_x(), imageSize.Get_y(), BufferedImage.TYPE_INT_RGB);
			image.setRGB(0, 0, viewSize.Get_x(), viewSize.Get_y(), imgBuffer, 0, viewSize.Get_x());
			
			if (params.getRotationDegree() > 0) {				
				image = ImageProcessingUtils.rotate(image, params.getRotationDegree());
			}
			
			decompressor.Native_destroy();
			channels.Native_destroy();
			if (codestream.Exists())
				codestream.Destroy();
			inputSource.Native_destroy();
			input.Native_destroy();
			jp2_family_in.Native_destroy();
			
			return image;
		} catch (KduException e) {
			e.printStackTrace();
			throw new DjatokaException(e);
		} catch (Exception e) {
			e.printStackTrace();
			throw new DjatokaException(e);
		}
	}

	private static Kdu_coords getReferenceExpansion(
			int reference_component, Kdu_channel_mapping channels,
			Kdu_codestream codestream) throws KduException {

		int c;
		Kdu_coords ref_subs = new Kdu_coords();
		Kdu_coords subs = new Kdu_coords();
		codestream.Get_subsampling(reference_component, ref_subs);
		Kdu_coords min_subs = new Kdu_coords();
		min_subs.Assign(ref_subs);

		for (c = 0; c < channels.Get_num_channels(); c++) {
			codestream.Get_subsampling(channels.Get_source_component(c), subs);
			if (subs.Get_x() < min_subs.Get_x())
				min_subs.Set_x(subs.Get_x());
			if (subs.Get_y() < min_subs.Get_y())
				min_subs.Set_y(subs.Get_y());
		}
		
		Kdu_coords expansion = new Kdu_coords();
		expansion.Set_x(ref_subs.Get_x() / min_subs.Get_x());
		expansion.Set_y(ref_subs.Get_y() / min_subs.Get_y());

		for (c = 0; c < channels.Get_num_channels(); c++) {
			codestream.Get_subsampling(channels.Get_source_component(c), subs);
			if ((((subs.Get_x() * expansion.Get_x()) % ref_subs.Get_x()) != 0)
					|| (((subs.Get_y() * expansion.Get_y()) % ref_subs.Get_y()) != 0)) {
				Kdu_global.Kdu_print_error("The supplied JP2 file contains color channels "
								+ "whose sub-sampling factors are not integer "
								+ "multiples of one another.");
				codestream.Apply_input_restrictions(0, 1, 0, 0, null, Kdu_global.KDU_WANT_OUTPUT_COMPONENTS);
				channels.Configure(codestream);
				expansion = new Kdu_coords(1, 1);
			}
		}
		return expansion;
	}
	
	
}
