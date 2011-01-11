package gov.lanl.adore.djatoka.openurl;

import gov.lanl.adore.djatoka.DjatokaException;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface IReferentMigrator {

	/**
	 * Returns a local File object for a provide URI
	 * @param uri the URI of an image to be downloaded and compressed as JP2
	 * @return File object of JP2 compressed image
	 * @throws DjatokaException
	 */
	public abstract File convert(URI uri) throws DjatokaException;

	/**
	 * Returns a local File object for a provide URI
	 * @param img File object on local image to be compressed
	 * @param uri the URI of an image to be compressed as JP2
	 * @return File object of JP2 compressed image
	 * @throws DjatokaException
	 */
	public abstract File processImage(File img, URI uri)
			throws DjatokaException;

	/**
	 * Return list of images currently being processed. Images are removed once complete.
	 * @return list of images being processed
	 */
	public abstract List<?> getProcessingList();

	/**
	 * Returns map of format extension (e.g. jpg) to mimetype mappings (e.g. image/jpeg)
	 * @return format extension to mimetype mappings
	 */
	public abstract Map<?, ?> getFormatMap();

	/**
	 * Sets map of format extension (e.g. jpg) to mimetype mappings (e.g. image/jpeg)
	 * @param formatMap extension to mimetype mappings
	 */
	public abstract void setFormatMap(HashMap<String, String> formatMap);

}