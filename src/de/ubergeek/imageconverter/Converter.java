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

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardOpenOption.*;
import javax.imageio.ImageIO;

/**
 * ImageConverter - main logic implemantion
 * @author André Gewert <agewert@ubergeek.de>
 */
public class Converter {
    
    /**
     * Conversion mode.
     */
    public enum Mode {
        RGB565, MONO
    };
    
    // <editor-fold desc="Properties">

    /**
     * Name of the original image file
     */
    private String filename;

    /**
     * BufferedImage created form source image file.
     * If no image has been loaded, this should be null.
     */
    private BufferedImage sourceImage;

    // </editor-fold>
    
    
    // <editor-fold desc="Accessors">

    /**
     * Returns the name of the last loaded image file
     * @return Filename of the last loaded image
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Return an BufferedImage containing the last loaded image file.
     * If no file has been loaded this should return null.
     * @return BufferedImage created from image file
     */
    public BufferedImage getSourceImage() {
        return sourceImage;
    }

    /**
     * Returns true if any image has been loaded
     * @return true if any image has been loaded
     */
    public boolean isImageLoaded() {
        return filename != null && sourceImage != null;
    }
    
    // </editor-fold>
    
    
    // <editor-fold desc="Constructors">
    
    /**
     * Empty default constructor.
     */
    public Converter() {
    }
    
    // </editor-fold>
    
    
    // <editor-fold desc="Public methods">
    
    /**
     * Creates a default file name for the source file to be created
     * @return Default file name for the source file
     */
    public String getDefaultOutputFileName() {
        var path = Path.of(getFilename());
        var dir = path.getParent();
        var fname = path.getFileName().toString();
        
        fname = fname.substring(0, fname.lastIndexOf(".")) + ".c";
        if (dir != null) fname = dir.toString() + File.separator + fname;
        return fname;
    }
    
    /**
     * Creates a default variable name derived from the last loaded file name.
     * @return Default variable name
     */
    public String getDefaultVariableName() {
        var path = Path.of(getFilename());
        var fname = path.getFileName().toString();
        return fname.substring(0, fname.lastIndexOf(".")).replaceAll("[^a-zA-Z0-9\\-_]", "");
    }
    
    /**
     * Loads an image from the given file name.
     * @param filename Name of the image file to be loaded
     * @throws IOException 
     */
    public void loadImage(String filename) throws IOException {
        this.filename = filename;
        sourceImage = ImageIO.read(new File(filename));
    }
    
    /**
     * Creates a copy of the source image with reduced color space according to
     * given options.
     * @param converterOptions Options for image conversion
     * @return BufferedImage with reduced colors
     */
    public Image createReducedImage(ConverterOptions converterOptions) {        
        var targetImage = new BufferedImage(
                sourceImage.getWidth(),
                sourceImage.getHeight(),
                (converterOptions.mode == Mode.MONO) ? BufferedImage.TYPE_BYTE_BINARY : BufferedImage.TYPE_USHORT_565_RGB
        );
        
        var graphics = targetImage.createGraphics();
        graphics.setColor(converterOptions.backgroundColor);
        graphics.fillRect(0, 0, targetImage.getWidth(), targetImage.getHeight());
        graphics.drawImage(sourceImage, 0, 0, null);
        
        if (converterOptions.invertColors) {
            invertImageColors(targetImage);
        }
        
        return targetImage;
    }
    
    /**
     * Creates the source code and saves it to the filename that's specified in
     * the given option.
     * @param options Converter options
     * @param append Set to true if generated source code should be appended to existing files
     * @throws IOException If the file could not be written
     */
    public void saveOutputfile(ConverterOptions options, boolean append) throws IOException {
        var fname = options.outputFilename;
        if (fname == null || fname.isBlank()) {
            fname = getDefaultOutputFileName();
        }

        var sb = new StringBuilder();
        sb.append(options.variableType).append(" ").append(options.variableName).append("[] = {").append(System.lineSeparator());
        
        // Dimensions of the created image data
        if (options.includeDimensions) {
            int targetWidth = sourceImage.getWidth();
            int targetHeight = sourceImage.getHeight();
            if (options.mode == Mode.MONO) {
                targetHeight = (int)(Math.ceil(targetHeight /8f) *8);
            }            
            sb.append(targetWidth).append(", ").append(targetHeight).append(", ").append(System.lineSeparator());
        }
        createSourceCodeFromImage(options, sb);
        sb.append("};").append(System.lineSeparator()).append(System.lineSeparator());

        if (append) {
            Files.writeString(Path.of(fname), sb.toString(), CREATE, APPEND);
        } else {
            Files.writeString(Path.of(fname), sb.toString(), CREATE, TRUNCATE_EXISTING);
        }
    }
    
    /**
     * Creates the source code and saves it to the filename that's specified in
     * the given option.
     * If the specified output file already exists it will be overwritten.
     * @param options Converter options
     * @throws IOException If the file could not be written
     */
    public void saveOutputfile(ConverterOptions options) throws IOException {
        saveOutputfile(options, false);
    }
    
    /**
     * Creates the source code according to the given ConvertOptions and appends
     * it to the given StringBuilder
     * @param options Converter options
     * @param sb String Builder
     */
    private int createSourceCodeFromImage(ConverterOptions options, StringBuilder sb) {
        if (options.mode == Mode.MONO) {
            return createMonoSourceCode(options, sb);
        }
        return createRgb565SourceCode(options, sb);
    }
    
    /**
     * Creates the source code in RGB-565 format
     * @param options Converter options
     * @param sb StringBuilder
     * @return Number of bytes defined
     */
    private int createRgb565SourceCode(ConverterOptions options, StringBuilder sb) {
        var img = (BufferedImage)createReducedImage(options);
        int result;
        int numberOfBytes = 0;
        int height = img.getHeight();
        int width = img.getWidth();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int[] array = new int[img.getColorModel().getNumComponents()];
                img.getRaster().getPixel(x, y, array);
                result = (array[2]) + (array[1] << 5) + (array[0] << 11);
                
                sb.append("0x").append(
                    padLeft(Integer.toHexString(result), 4, '0')
                );
                if (x != width -1 || y != height -1) {
                    sb.append(", ");
                }
                numberOfBytes += 2;
            }
            sb.append(System.lineSeparator());
        }
        return numberOfBytes;
    }
    
    /**
     * Creates the source code for monochrome images.
     * Bytes will be calculated vertically; image height will be padded to a
     * multiple of 8.
     * @param options Converter options
     * @param sb StringBuilder
     * @return Number of bytes defined
     */
    private int createMonoSourceCode(ConverterOptions options, StringBuilder sb) {
        var img = (BufferedImage)createReducedImage(options);
        int resultByte;
        int numberOfElements = 0;
        int width = img.getWidth();
        int height = img.getHeight();
        
        for (int y = 0; y < height; y += 8) {
            for (int x = 0; x < width; x++) {
                resultByte = 0;
                for (int counter = 0; counter < 8; counter++) {
                    if (y +counter < height) {
                        int[] array = new int[img.getColorModel().getNumComponents()];
                        img.getRaster().getPixel(x, y +counter, array);                
                        resultByte |= (array[0] << counter);
                    }
                }
                
                sb.append("0x").append(
                    padLeft(Integer.toHexString(resultByte), 2, '0')
                );
                if (x != width -1 || y != height -1) {
                    sb.append(", ");
                }
                numberOfElements++;
            }
            sb.append(System.lineSeparator());
        }
        return numberOfElements;
    }
    
    // </editor-fold>
    
    
    // <editor-fold desc="Internal methods">
    
    /**
     * Inverts the colors of the given BufferedImage.
     * @param image The image to be inverted
     */
    private static void invertImageColors(BufferedImage image) {
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int rgba = image.getRGB(x, y);
                Color col = new Color(rgba, true);
                col = new Color(255 - col.getRed(),
                                255 - col.getGreen(),
                                255 - col.getBlue());
                image.setRGB(x, y, col.getRGB());
            }
        }
    }
    
    private static String padLeft(String inputValue, Integer length, char car) {
        return (inputValue + String.format("%" + length + "s", "").replace(" ", String.valueOf(car))).substring(0, length);
    }
    
    // </editor-fold>
    
}
