/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Who?
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package com.github.neoflyingsaucer.resource;

import java.io.InputStream;

import com.github.neoflyingsaucer.extend.useragent.ImageResourceI;

/**
 * Use this class to return an ImageResource from the user agent.
 * 
 */
public class ImageResource implements ImageResourceI {
    private final String _imageUri;
    private final InputStream strm;

    public ImageResource(String uri, InputStream strm) {
        _imageUri = uri;
        this.strm = strm;
    }

    /* (non-Javadoc)
	 * @see com.github.neoflyingsaucer.extend.ImageResourceI#getImage()
	 */
    @Override
	public InputStream getImage() {
        return strm;
    }

    /* (non-Javadoc)
	 * @see com.github.neoflyingsaucer.extend.ImageResourceI#getImageUri()
	 */
    @Override
	public String getImageUri() {
        return _imageUri;
    }
}