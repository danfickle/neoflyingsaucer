/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Patrick Wright
 * Copyright (c) 2007 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package com.github.neoflyingsaucer.css.parser.property;

import java.util.List;

import com.github.neoflyingsaucer.css.constants.CSSName;
import com.github.neoflyingsaucer.css.parser.PropertyValue;
import com.github.neoflyingsaucer.css.sheet.PropertyDeclaration;
import com.github.neoflyingsaucer.css.sheet.StylesheetInfo.CSSOrigin;

public interface PropertyBuilder {
    /**
     * Builds a list of <code>PropertyDeclaration</code> objects for the CSS
     * property <code>cssName</code>. <code>values</code> must contain
     * <code>CSSPrimitiveValue</code> objects.
     */
    public List<PropertyDeclaration> buildDeclarations(
            CSSName cssName, List<PropertyValue> values,
            CSSOrigin origin, boolean important, boolean inheritAllowed);
}
