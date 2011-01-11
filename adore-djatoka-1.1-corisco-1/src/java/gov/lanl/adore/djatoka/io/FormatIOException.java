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

package gov.lanl.adore.djatoka.io;

import gov.lanl.adore.djatoka.DjatokaException;

/**
 * Exception handler used during Format I/O operations.
 * @author Ryan Chute
 *
 */
public class FormatIOException extends DjatokaException {

	private static final long serialVersionUID = 1307882022387500079L;

	public FormatIOException(String message) {
        super(message);
    }

    public FormatIOException(String message, Throwable cause) {
        super(message, cause);
    }

    public FormatIOException(Throwable cause) {
        super(cause);
    }

}
