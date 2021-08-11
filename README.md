# ImageConverter
Creates c source files from images.

Supports output as RGB-565 (16 Bits per pixel) and monochrome (1 Bit per pixel, height will be padded to a multiple of 8).

This tool targets ArduBoy and Circuitmess Nibble development but should be useful for other environments, too.

It can be used as a command line tool and for batch processing (optionally all input images can be written into a single output file) but also includes a nice user interface. Just start the Java application without arguments to open the graphical user interface.

If a Java runtime is installed, after unpacking the ZIP file, the file `ImageConverter.jar` should be runnable by double clicking it. Alternatively it can be run via command line typing `java -jar ImageConverter.jar`.

![imageconverter](ImageConverter.png)


# Command line arguments
- `-c`, `--backgroundcolor` <color code>  
Sets the background color for the target.
- `-m`, `--mode` <mode>  
Specifies the output format: `rgb565` for 16bit color images or `mono` for monochrome images.
- `-v`, `--varname` <variable name>    
Specifies the variable name that should be generated. Should not be used if multiple files should be converted in one program call. In this case the variable name will be derived from file name.
- `-t`, `--vartype` <type>  
The c type expression that should be used in the generated source code. Defaults to `const unsigned short PROGMEM` which is suitable when targeting the Circuitmess platforms. When targeting ArduBoy, the type should be `const uint8_t PROGMEM`. Other environments may require other data types.
- `-o`, `--outputfile` <filename>  
Specifies the name of the output file that should be created. If no output file is specified, the name will be derived from the input file. Note that any existing file with that name will be overwritten, if existing! If more than one input file is given use this option to write into a single output file.
- `-i`, `--invertcolors`  
Set this option to invert the color reduced image. Can be useful for some monochrome images.
- `-d`, `--includedimensions`  
When this option is set, the generated array data will contain the image dimensions at first. This format is required by some graphics libraries.

# Third party components
This software uses some icons from the [FatCow icon collection](https://www.fatcow.com/free-icons)
and the Java Look and Feel [FlatLaf](https://www.formdev.com/flatlaf/).
