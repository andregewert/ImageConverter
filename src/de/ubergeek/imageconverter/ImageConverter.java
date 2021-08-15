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

            try {
                while (i < args.length) {
                    arg = args[i];

                    switch (arg) {

                        case "-c", "--backgroundcolor" -> {
                            value = getArg(args, ++i);
                            if (value != null) options.backgroundColor = Color.decode(value);
                        }

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

                        case "-v", "--varname" -> {
                            options.variableName = getArg(args, ++i);
                        }

                        case "-t", "--vartype" -> {
                            options.variableType = getArg(args, ++i);
                        }

                        case "-o", "--outputfile" -> {
                            if (outdir != null) {
                                throw new IllegalArgumentException("Options --directory and --outputfile cannot be combined!");
                            }
                            outfile = getArg(args, ++i);
                        }

                        case "-i", "--invertcolors" -> {
                            options.invertColors = true;
                        }

                        case "-d", "--includedimensions" -> {
                            options.includeDimensions = true;
                        }

                        case "-a", "--ascii" -> {
                            options.createAsciiArt = true;
                        }

                        case "-p", "--preset" -> {
                            value = getArg(args, ++i);
                            options.applyPreset(ConverterOptions.Preset.valueOf(value));
                        }
                        
                        case "-e", "--directory" -> {
                            if (outfile != null) {
                                throw new IllegalArgumentException("Options --directory and --outputfile cannot be combined!");
                            }
                            outdir = getArg(args, ++i);
                        }

                        case "-h", "--help" -> {
                            // TODO show help text
                        }

                        default -> {
                            filenames.add(arg);
                        }
                    }
                    i++;
                }

                if (filenames.isEmpty()) {
                    throw new IllegalArgumentException("No input files given!");
                }
            
            } catch (IllegalArgumentException ex) {
                showHelp();
                return;
            }

            // Remove file before appending data to it
            if (outfile != null && Files.exists(Path.of(outfile))) {
                Files.delete(Path.of(outfile));
            }

            boolean appendToFile = outfile != null && filenames.size() > 1;
            for (var filename : filenames) {
                converter.loadImage(filename);
                
                // TODO use outdir if specified!
                options.outputFilename = (outfile == null)? converter.getDefaultOutputFileName() : outfile;
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
        // TODO implement
    }

}
