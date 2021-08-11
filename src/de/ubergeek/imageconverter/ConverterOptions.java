/*
 * Copyright (C) 2021 André Gewert <agewert@ubergeek.de>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package de.ubergeek.imageconverter;

import de.ubergeek.imageconverter.Converter.Mode;
import java.awt.Color;

/**
 * Simple pojo for all option the ImageConverter knows.
 * @author André Gewert <agewert@ubergeek.de>
 */
public class ConverterOptions {

    // <editor-fold desc="Properties">

    /**
     * The background color the source image should be drawn on.
     * -c / --backgroundcolor
     */
    public Color backgroundColor = Color.BLACK;

    /**
     * Conversion mode.
     * -m / --mode
     */
    public Mode mode = Mode.RGB565;

    /**
     * The variable name that should be used in generated source code.
     * -v / --varname
     */
    public String variableName = "";
    
    /**
     * The variable type that should be used in generated source code.
     * -t / --vartype
     */
    public String variableType = "const unsigned short PROGMEM";
    
    /**
     * Name of the output file to be written.
     * -o / --outputfile
     */
    public String outputFilename = "";
    
    /**
     * Indicates if the colors of the source image should be inverted after
     * color reduction.
     * -i / --invertcolors
     */
    public boolean invertColors = false;
    
    /**
     * Indicates if the generated image array should include the image's
     * dimension at the beginning.
     * -d / --includedimensions
     */
    public boolean includeDimensions = false;

    // </editor-fold>
    
}
