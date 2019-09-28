
package com.giszone.filepicker.controller;


/**
 * Interface definition for a callback to be invoked
 * when dialog selects files.
 */
public interface DialogSelectionListener {

    /**
     * The method is called when files or directories are selected.
     *
     * @param filePaths The array of String containing selected file paths.
     */
    void onSelectedFilePaths(String[] filePaths);
}
