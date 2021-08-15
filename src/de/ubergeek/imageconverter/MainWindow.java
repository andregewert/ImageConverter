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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.SwingWorker;

/**
 * ImageConverter - main application window
 * @author André Gewert <agewert@ubergeek.de>
 */
public class MainWindow extends javax.swing.JFrame {

    /**
     * Instance of the image converter utility.
     */
    private final Converter converter;

    /**
     * Instance of the currently selected converter options.
     */
    private final ConverterOptions options;
    
    /**
     * Instance of JFileChooser for "Open image" command
     */
    private final JFileChooser fileChooser = new JFileChooser();

    /**
     * Indicates if the application is busy with a loading / saving action.
     * Some action should be deactivated while the application is busy.
     */
    private boolean busy = false;
    
    /**
     * Creates new form MainWindow
     */
    public MainWindow() {
        initComponents();
        
        setIconImage(
            new ImageIcon(getClass().getResource("/de/ubergeek/imageconverter/res/images.png")).getImage()
        );

        statusBarProgressBar.setVisible(false);        
        converter = new Converter();
        options = new ConverterOptions();
        updateUserInterfaceState();
        
        // Drag and drop support
        setDropTarget(null);
        new DropTarget(sourceImageScrollPane, DnDConstants.ACTION_COPY_OR_MOVE, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
		    Transferable tr = dtde.getTransferable();
		    DataFlavor[] flavors = tr.getTransferDataFlavors();

                    for (DataFlavor flavor : flavors) {
                        if (flavor.isFlavorJavaFileListType()) {
                            dtde.acceptDrop(dtde.getDropAction());
                            
                            @SuppressWarnings(value = "unchecked")
                            java.util.List<File> files = (java.util.List<File>)tr.getTransferData(flavor);
                            if (files.size() == 1) {
                                loadImage(files.get(0).toString());
                            }
                            dtde.dropComplete(true);
                        }
                    }
		    return;
		} catch (UnsupportedFlavorException | IOException t) {
                    // Ignore errors
		}
		dtde.rejectDrop();
            }
        });
    }
    
    
    // <editor-fold desc="Internal methods">

    /**
     * Update some user interface elements dependend on current application
     * state.
     */
    private void updateUserInterfaceState() {
        copyOptionsToUserInterface();
        saveButton.setEnabled(converter.isImageLoaded() && !busy);
        saveMenuItem.setEnabled(converter.isImageLoaded() && !busy);
        openImageButton.setEnabled(!busy);
        openImageMenuItem.setEnabled(!busy);
        statusBarProgressBar.setVisible(busy);
        updatePreviewButton.setEnabled(converter.isImageLoaded());
    }
    
    /**
     * Updates the preview image according the current converter options.
     */
    private void updatePreviewImage() {
        if (converter.isImageLoaded()) {
            sourceImageLabel.setIcon(new ImageIcon(converter.getSourceImage()));
            targetImageLabel.setIcon(
                new ImageIcon(converter.createReducedImage(options))
            );
        } else {
            sourceImageLabel.setIcon(null);
            targetImageLabel.setIcon(null);
        }
    }
    
    /**
     * Updates all user interface elements regarding converting options with
     * selected options from options object.
     */
    private void copyOptionsToUserInterface() {
        backgroundColorPreviewPanel.setBackground(options.backgroundColor);
        if (options.mode == Converter.Mode.MONOV) {
            targetFormatComboBox.setSelectedIndex(0);
        } else if (options.mode == Converter.Mode.MONOH) {
            targetFormatComboBox.setSelectedIndex(1);
        } else {
            targetFormatComboBox.setSelectedIndex(2);
        }
        variableNameTextField.setText(options.variableName);
        variableTypeTextField.setText(options.variableType);
        outputFilenameTextField.setText(options.outputFilename);
        invertColorsCheckBox.setSelected(options.invertColors);
        includeDimensionsCheckBox.setSelected(options.includeDimensions);
    }
    
    /**
     * Copies all options from user interface elements into the options object.
     */
    private void copyOptionsFromUserInterface() {
        options.backgroundColor = backgroundColorPreviewPanel.getBackground();
        if (targetFormatComboBox.getSelectedIndex() == 0) {
            options.mode = Converter.Mode.MONOV;
        } else if (targetFormatComboBox.getSelectedIndex() == 1) {
            options.mode = Converter.Mode.MONOH;
        } else if (targetFormatComboBox.getSelectedIndex() == 2) {
            options.mode = Converter.Mode.RGB565;
        }
        options.variableName = variableNameTextField.getText();
        options.variableType = variableTypeTextField.getText();
        options.outputFilename = outputFilenameTextField.getText();
        options.invertColors = invertColorsCheckBox.isSelected();
        options.includeDimensions = includeDimensionsCheckBox.isSelected();
    }
    
    /**
     * Uitilizes a SwingWorker to save the currently loaded image as a source
     * file.
     */
    private void saveImage() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            boolean success = false;

            @Override
            protected Void doInBackground() {
                try {
                    converter.saveOutputfile(options, false);
                    success = true;
                } catch (IOException ex) {
                    Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
                    System.out.println(ex.getMessage());
                    System.out.println(Arrays.toString(ex.getStackTrace()));
                    success = false;
                }
                return null;
            }
            
            @Override
            protected void done() {
                busy = false;
                updateUserInterfaceState();
                if (success) {
                    statusBarLabel.setText("Source file saved");
                } else {
                    statusBarLabel.setText("Could not write file");
                }
            }
        };
          
        busy = true;
        statusBarLabel.setText("Saving source file ...");
        updateUserInterfaceState();
        worker.execute();
    }
    
    /**
     * Uitilizes a SwingWorker to load an image file.
     * @param filename The file to be loaded
     */
    private void loadImage(String filename) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            boolean success = false;
            
            @Override
            protected Void doInBackground() {
                try {
                    converter.loadImage(filename);
                    options.outputFilename = converter.getDefaultOutputFileName();
                    options.variableName = converter.getDefaultVariableName();
                    success = true;
                } catch (IOException ex) {
                    Logger.getLogger(MainWindow.class.getName()).log(Level.SEVERE, null, ex);
                    success = false;
                }
                return null;
            }

            @Override
            protected void done() {
                busy = false;
                updateUserInterfaceState();
                updatePreviewImage();
                if (success) {
                    statusBarLabel.setText("Image loaded");
                } else {
                    statusBarLabel.setText("Error while opening file");
                }
            }
            
        };
        
        busy = true;
        statusBarLabel.setText("Opening image ...");
        updateUserInterfaceState();
        worker.execute();
    }
    
    // </editor-fold>
    
    
    // <editor-fold desc="Internal user interface actions">
    
    /**
     * Opens a color selection dialog and sets the selected color as background
     * color in the converter options.
     */
    private void cmdOpenBackgroundColorChooser() {
        var dialog = new ColorSelectionDialog(this, true);
        dialog.setVisible(true);
        if (dialog.getReturnCode() == ColorSelectionDialog.ReturnCode.OK) {
            backgroundColorPreviewPanel.setBackground(dialog.getSelectedColor());
            copyOptionsFromUserInterface();
            updateUserInterfaceState();
        }
    }

    /**
     * Shows a modal info dialog.
     */
    private void cmdOpenAboutDialog() {
        var dialog = new AboutDialog(this, true);
        dialog.setVisible(true);
    }

    /**
     * Quits the application.
     */
    private void cmdQuit() {
        setVisible(false);
        dispose();
    }

    /**
     * Opens a file chooser and loads the selected image file.
     */
    private void cmdOpenImage() {
        copyOptionsFromUserInterface();
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file != null) {
                loadImage(file.getAbsolutePath());
            }
        }
    }
    
    /**
     * Saves the currently loaded image as source file.
     */
    private void cmdSave() {
        copyOptionsFromUserInterface();
        updatePreviewImage();
        saveImage();
    }

    /**
     * Updates the preview image according to current converter options.
     */
    private void cmdUpdatePreview() {
        copyOptionsFromUserInterface();
        updateUserInterfaceState();
        updatePreviewImage();
    }
    
    // </editor-fold>

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainSplitPane = new javax.swing.JSplitPane();
        imageSplitPane = new javax.swing.JSplitPane();
        sourceImageScrollPane = new javax.swing.JScrollPane();
        sourceImageLabel = new javax.swing.JLabel();
        previewImageScrollPane = new javax.swing.JScrollPane();
        targetImageLabel = new javax.swing.JLabel();
        settingsScrollPane = new javax.swing.JScrollPane();
        settingsFramePanel = new javax.swing.JPanel();
        settingsHeadlineLabel = new javax.swing.JLabel();
        selectBackgroundColorLabel = new javax.swing.JLabel();
        invertColorsLabel = new javax.swing.JLabel();
        targetFormatLabel = new javax.swing.JLabel();
        selectBackgroundColorButton = new javax.swing.JButton();
        targetFormatComboBox = new javax.swing.JComboBox<>();
        invertColorsCheckBox = new javax.swing.JCheckBox();
        variableNameLabel = new javax.swing.JLabel();
        variableNameTextField = new javax.swing.JTextField();
        outputFilename = new javax.swing.JLabel();
        outputFilenameTextField = new javax.swing.JTextField();
        saveButton = new javax.swing.JButton();
        openImageButton = new javax.swing.JButton();
        includeDimensionsLabel = new javax.swing.JLabel();
        includeDimensionsCheckBox = new javax.swing.JCheckBox();
        backgroundColorPreviewPanel = new javax.swing.JPanel();
        variableTypeLabel = new javax.swing.JLabel();
        variableTypeTextField = new javax.swing.JTextField();
        updatePreviewButton = new javax.swing.JButton();
        statusBarPanel = new javax.swing.JPanel();
        statusBarLabel = new javax.swing.JLabel();
        statusBarProgressBar = new javax.swing.JProgressBar();
        mainMenuBar = new javax.swing.JMenuBar();
        mainMenu = new javax.swing.JMenu();
        openImageMenuItem = new javax.swing.JMenuItem();
        saveMenuItem = new javax.swing.JMenuItem();
        menuSeparator = new javax.swing.JPopupMenu.Separator();
        aboutMenuItem = new javax.swing.JMenuItem();
        quitMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("ImageConverter");
        setLocationByPlatform(true);

        mainSplitPane.setDividerLocation(300);
        mainSplitPane.setResizeWeight(1.0);

        imageSplitPane.setDividerLocation(180);
        imageSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        imageSplitPane.setResizeWeight(0.5);

        sourceImageLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        sourceImageScrollPane.setViewportView(sourceImageLabel);

        imageSplitPane.setTopComponent(sourceImageScrollPane);

        targetImageLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        previewImageScrollPane.setViewportView(targetImageLabel);

        imageSplitPane.setRightComponent(previewImageScrollPane);

        mainSplitPane.setLeftComponent(imageSplitPane);

        settingsFramePanel.setMaximumSize(null);
        settingsFramePanel.setPreferredSize(new java.awt.Dimension(200, 120));

        settingsHeadlineLabel.setFont(settingsHeadlineLabel.getFont().deriveFont(settingsHeadlineLabel.getFont().getStyle() | java.awt.Font.BOLD));
        settingsHeadlineLabel.setText("Settings");

        selectBackgroundColorLabel.setLabelFor(selectBackgroundColorButton);
        selectBackgroundColorLabel.setText("Background color");

        invertColorsLabel.setLabelFor(invertColorsCheckBox);
        invertColorsLabel.setText("Invert colors");

        targetFormatLabel.setLabelFor(targetFormatComboBox);
        targetFormatLabel.setText("Target format");

        selectBackgroundColorButton.setText("Select");
        selectBackgroundColorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectBackgroundColorButtonActionPerformed(evt);
            }
        });

        targetFormatComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Monochrome V", "Monochrome H", "RGB-565" }));

        variableNameLabel.setLabelFor(variableNameTextField);
        variableNameLabel.setText("Variable name");

        outputFilename.setLabelFor(outputFilenameTextField);
        outputFilename.setText("Output file");

        saveButton.setText("Save");
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });

        openImageButton.setText("Open Image");
        openImageButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openImageButtonActionPerformed(evt);
            }
        });

        includeDimensionsLabel.setLabelFor(invertColorsCheckBox);
        includeDimensionsLabel.setText("Include dimensions");

        backgroundColorPreviewPanel.setBackground(new java.awt.Color(0, 0, 0));
        backgroundColorPreviewPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));

        javax.swing.GroupLayout backgroundColorPreviewPanelLayout = new javax.swing.GroupLayout(backgroundColorPreviewPanel);
        backgroundColorPreviewPanel.setLayout(backgroundColorPreviewPanelLayout);
        backgroundColorPreviewPanelLayout.setHorizontalGroup(
            backgroundColorPreviewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 20, Short.MAX_VALUE)
        );
        backgroundColorPreviewPanelLayout.setVerticalGroup(
            backgroundColorPreviewPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 20, Short.MAX_VALUE)
        );

        variableTypeLabel.setLabelFor(variableNameTextField);
        variableTypeLabel.setText("Variable type");

        updatePreviewButton.setText("Update Preview");
        updatePreviewButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updatePreviewButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout settingsFramePanelLayout = new javax.swing.GroupLayout(settingsFramePanel);
        settingsFramePanel.setLayout(settingsFramePanelLayout);
        settingsFramePanelLayout.setHorizontalGroup(
            settingsFramePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(settingsFramePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(settingsFramePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(settingsFramePanelLayout.createSequentialGroup()
                        .addGroup(settingsFramePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(selectBackgroundColorLabel)
                            .addComponent(targetFormatLabel)
                            .addComponent(invertColorsLabel)
                            .addComponent(variableNameLabel)
                            .addComponent(settingsHeadlineLabel)
                            .addComponent(outputFilename)
                            .addComponent(includeDimensionsLabel)
                            .addComponent(variableTypeLabel))
                        .addGap(18, 18, 18)
                        .addGroup(settingsFramePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(variableNameTextField)
                            .addComponent(targetFormatComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(outputFilenameTextField)
                            .addGroup(settingsFramePanelLayout.createSequentialGroup()
                                .addGroup(settingsFramePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(invertColorsCheckBox)
                                    .addComponent(includeDimensionsCheckBox))
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(settingsFramePanelLayout.createSequentialGroup()
                                .addComponent(backgroundColorPreviewPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(selectBackgroundColorButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addComponent(variableTypeTextField)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, settingsFramePanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(updatePreviewButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(openImageButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(saveButton)))
                .addContainerGap())
        );
        settingsFramePanelLayout.setVerticalGroup(
            settingsFramePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(settingsFramePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(settingsHeadlineLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(settingsFramePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(settingsFramePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(selectBackgroundColorLabel)
                        .addComponent(selectBackgroundColorButton))
                    .addComponent(backgroundColorPreviewPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(settingsFramePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(targetFormatLabel)
                    .addComponent(targetFormatComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(settingsFramePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(variableNameLabel)
                    .addComponent(variableNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(settingsFramePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(variableTypeLabel)
                    .addComponent(variableTypeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(settingsFramePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(outputFilename)
                    .addComponent(outputFilenameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(settingsFramePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(invertColorsLabel)
                    .addComponent(invertColorsCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(settingsFramePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(includeDimensionsLabel)
                    .addComponent(includeDimensionsCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 95, Short.MAX_VALUE)
                .addGroup(settingsFramePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(saveButton)
                    .addComponent(openImageButton)
                    .addComponent(updatePreviewButton))
                .addContainerGap())
        );

        settingsScrollPane.setViewportView(settingsFramePanel);

        mainSplitPane.setRightComponent(settingsScrollPane);

        statusBarPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));

        statusBarLabel.setText(" ");

        statusBarProgressBar.setIndeterminate(true);

        javax.swing.GroupLayout statusBarPanelLayout = new javax.swing.GroupLayout(statusBarPanel);
        statusBarPanel.setLayout(statusBarPanelLayout);
        statusBarPanelLayout.setHorizontalGroup(
            statusBarPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusBarPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusBarLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusBarProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        statusBarPanelLayout.setVerticalGroup(
            statusBarPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, statusBarPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(statusBarPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(statusBarLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(statusBarProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        mainMenu.setText("File");

        openImageMenuItem.setText("Open Image ...");
        openImageMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openImageMenuItemActionPerformed(evt);
            }
        });
        mainMenu.add(openImageMenuItem);

        saveMenuItem.setText("Save");
        saveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMenuItemActionPerformed(evt);
            }
        });
        mainMenu.add(saveMenuItem);
        mainMenu.add(menuSeparator);

        aboutMenuItem.setText("About ...");
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        mainMenu.add(aboutMenuItem);

        quitMenuItem.setText("Quit");
        quitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                quitMenuItemActionPerformed(evt);
            }
        });
        mainMenu.add(quitMenuItem);

        mainMenuBar.add(mainMenu);

        setJMenuBar(mainMenuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusBarPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mainSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 640, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mainSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 344, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusBarPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
        cmdOpenAboutDialog();
    }//GEN-LAST:event_aboutMenuItemActionPerformed

    private void quitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_quitMenuItemActionPerformed
        cmdQuit();
    }//GEN-LAST:event_quitMenuItemActionPerformed

    private void openImageButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openImageButtonActionPerformed
        cmdOpenImage();
    }//GEN-LAST:event_openImageButtonActionPerformed

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
        cmdSave();
    }//GEN-LAST:event_saveButtonActionPerformed

    private void openImageMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openImageMenuItemActionPerformed
        cmdOpenImage();
    }//GEN-LAST:event_openImageMenuItemActionPerformed

    private void saveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMenuItemActionPerformed
        cmdSave();
    }//GEN-LAST:event_saveMenuItemActionPerformed

    private void updatePreviewButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updatePreviewButtonActionPerformed
        cmdUpdatePreview();
    }//GEN-LAST:event_updatePreviewButtonActionPerformed

    private void selectBackgroundColorButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectBackgroundColorButtonActionPerformed
        cmdOpenBackgroundColorChooser();
    }//GEN-LAST:event_selectBackgroundColorButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JPanel backgroundColorPreviewPanel;
    private javax.swing.JSplitPane imageSplitPane;
    private javax.swing.JCheckBox includeDimensionsCheckBox;
    private javax.swing.JLabel includeDimensionsLabel;
    private javax.swing.JCheckBox invertColorsCheckBox;
    private javax.swing.JLabel invertColorsLabel;
    private javax.swing.JMenu mainMenu;
    private javax.swing.JMenuBar mainMenuBar;
    private javax.swing.JSplitPane mainSplitPane;
    private javax.swing.JPopupMenu.Separator menuSeparator;
    private javax.swing.JButton openImageButton;
    private javax.swing.JMenuItem openImageMenuItem;
    private javax.swing.JLabel outputFilename;
    private javax.swing.JTextField outputFilenameTextField;
    private javax.swing.JScrollPane previewImageScrollPane;
    private javax.swing.JMenuItem quitMenuItem;
    private javax.swing.JButton saveButton;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JButton selectBackgroundColorButton;
    private javax.swing.JLabel selectBackgroundColorLabel;
    private javax.swing.JPanel settingsFramePanel;
    private javax.swing.JLabel settingsHeadlineLabel;
    private javax.swing.JScrollPane settingsScrollPane;
    private javax.swing.JLabel sourceImageLabel;
    private javax.swing.JScrollPane sourceImageScrollPane;
    private javax.swing.JLabel statusBarLabel;
    private javax.swing.JPanel statusBarPanel;
    private javax.swing.JProgressBar statusBarProgressBar;
    private javax.swing.JComboBox<String> targetFormatComboBox;
    private javax.swing.JLabel targetFormatLabel;
    private javax.swing.JLabel targetImageLabel;
    private javax.swing.JButton updatePreviewButton;
    private javax.swing.JLabel variableNameLabel;
    private javax.swing.JTextField variableNameTextField;
    private javax.swing.JLabel variableTypeLabel;
    private javax.swing.JTextField variableTypeTextField;
    // End of variables declaration//GEN-END:variables
}
