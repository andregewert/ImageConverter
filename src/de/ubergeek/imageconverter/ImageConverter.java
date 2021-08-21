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

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * ImageConverter - command line wrapper
 * @author André Gewert <agewert@ubergeek.de>
 */
public class ImageConverter {

    /**
     * ImageConverter - command line wrapper
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (UnsupportedLookAndFeelException ex) {
            Logger.getLogger(ImageConverter.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Open ui if program is called without arguments
        if (args.length == 0) {
            java.awt.EventQueue.invokeLater(() -> {
                new MainWindow().setVisible(true);
            });
        } else {
            Converter converter = new Converter();
            ConverterOptions options = new ConverterOptions();
            int i = 0;
            String arg;
            String value;
            String outfile = null;
            String outdir = null;
            ArrayList<String> filenames = new ArrayList<>();
            boolean helpShown = false;

            try {
                while (i < args.length) {
                    arg = args[i];

                    switch (arg) {

                        // Specify the background color
                        case "-c", "--backgroundcolor" -> {
                            value = getArg(args, ++i);
                            if (value != null) options.backgroundColor = Color.decode(value);
                        }

                        // Specify output format / conversion mode
                        case "-m", "--mode" -> {
                            value = getArg(args, ++i);
                            switch (value) {
                                case "rgb565":
                                    options.mode = Converter.Mode.RGB565;
                                    break;
                                case "mono":
                                case "monov":
                                    options.mode = Converter.Mode.MONOV;
                                    break;
                                case "monoh":
                                    options.mode = Converter.Mode.MONOH;
                                    break;
                                default:
                                    throw new IllegalArgumentException("Unknown mode " + value);
                            }
                        }

                        // Specify the variable name
                        case "-v", "--varname" -> {
                            options.variableName = getArg(args, ++i);
                        }

                        // Specify the variable type
                        case "-t", "--vartype" -> {
                            options.variableType = getArg(args, ++i);
                        }

                        // Specify output file name
                        case "-o", "--outputfile" -> {
                            if (outdir != null) {
                                throw new IllegalArgumentException("Options --directory and --outputfile cannot be combined!");
                            }
                            outfile = getArg(args, ++i);
                        }

                        // Specify if colors should be inverted
                        case "-i", "--invertcolors" -> {
                            options.invertColors = true;
                        }

                        // Specify if image dimensions should be included in the array
                        case "-d", "--includedimensions" -> {
                            options.includeDimensions = true;
                        }

                        // Specify if ascii representation should be created
                        case "-a", "--ascii" -> {
                            options.createAsciiArt = true;
                        }

                        // Specify the preset of typical options that should be used
                        case "-p", "--preset" -> {
                            value = getArg(args, ++i);
                            options.applyPreset(ConverterOptions.Preset.valueOf(value));
                        }

                        // Specify the output directory
                        case "-e", "--directory" -> {
                            if (outfile != null) {
                                throw new IllegalArgumentException("Options --directory and --outputfile cannot be combined!");
                            }
                            outdir = getArg(args, ++i);
                        }

                        // Output some help
                        case "-h", "--help" -> {
                            showHelp();
                            helpShown = true;
                        }

                        // Other arguments are interpreted as input file names
                        default -> {
                            filenames.add(arg);
                        }
                    }
                    i++;
                }

                // There should be some input files
                if (filenames.isEmpty()) {
                    throw new IllegalArgumentException("No input files given!");
                }
            
            } catch (IllegalArgumentException ex) {
                if (!helpShown) showHelp();
                return;
            }

            if (helpShown) return;
            
            // Remove file before appending data to it
            if (outfile != null && Files.exists(Path.of(outfile))) {
                Files.delete(Path.of(outfile));
            }

            boolean appendToFile = outfile != null && filenames.size() > 1;
            for (var filename : filenames) {
                converter.loadImage(filename);
                
                // Create output file name
                if (outfile != null) {
                    options.outputFilename = outfile;
                } else if (outdir != null) {
                    options.outputFilename = outdir + File.separator + converter.getDefaultOutputFileName(false);
                } else {
                    options.outputFilename = converter.getDefaultOutputFileName(true);
                }
                                
                if (options.variableName.isBlank()) {
                    options.variableName = converter.getDefaultVariableName();
                }
                converter.saveOutputfile(options, appendToFile);
            }
        }
    }
    
    private static String getArg(String[] args, int index) {
        if (index < args.length) return args[index];
        throw new IllegalArgumentException("Required argument is missing");
    }
    
    private static void showHelp() {
        var nl = System.lineSeparator();
        System.out.println(
            "ImageConverter - creates C source files from images" + nl + nl +
            "Usage:" + nl +
            "-p, --preset <arduboy|cos|cosmono>" + nl +
            "  Use an option preset for the given target." + nl + nl +
            "-c, --backgroundcolor <color code>" + nl +
            "  Sets the background color for the target image." + nl + nl +
            "-m, --mode <mode>" + nl +
            "  Specifies the output format: `rgb565` for 16bit color images or `monoh` or `monov` for monochrome images (horizontally or vertically grouped)." + nl + nl +
            "-v, --varname <variable name>" + nl + 
            "  Specifies the variable name that should be generated. Should not be used if multiple files should be converted in one program call. In this case the variable name will be derived from file name." + nl + nl +
            "-t, --vartype <type>" + nl +
            "  The c type expression that should be used in the generated source code." + nl + nl +
            "-o, --outputfile <filename>" + nl +
            "  Specifies the name of the output file that should be created." + nl + nl +
            "-i, --invertcolors" + nl +
            "  Set this option to invert the color reduced image." + nl + nl +
            "-d, --includedimensions" + nl +
            "  When this option is set, the generated array data will contain the image dimensions at the beginning." + nl + nl +
            "-a, --ascii" + nl + 
            "  Use this option when a ascii representation of the image should be included in the generated source code." + nl + nl +
            "-e, --directory" + nl + 
            "  Specify an output directory for generated files." + nl + nl +
            "-h, --help" + nl + 
            "  Show this help text." + nl + nl +
            "Other argument will be interpreted as input file names. Don't give any arguments to start a graphical interface." + nl + nl
        );
    }

}
