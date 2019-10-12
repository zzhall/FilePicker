
package com.giszone.filepicker;

import java.io.File;
import java.io.FileFilter;
import java.util.Locale;


/*  Class to filter the list of files.
 */
public class ExtensionFilter implements FileFilter {

    private int selectType;
    private String[] validExtensions;

    public ExtensionFilter(int selectType, String[] extensions) {
        this.selectType = selectType;
        if (extensions != null) {
            this.validExtensions = extensions;
        } else {
            this.validExtensions = new String[]{""};
        }
    }

    /**
     * Function to filter files based on defined rules.
     */
    @Override
    public boolean accept(File file) {
        //All directories are added in the least that can be read by the Application
        if (file.isDirectory() && file.canRead()) {
            return true;
        } else if (selectType == FilePickerDialog.TYPE_DIR) {   /*  True for files, If the selection type is Directory type, ie.
         *  Only directory has to be selected from the list, then all files are
         *  ignored.
         */
            return false;
        } else {   /*  Check whether name of the file ends with the extension. Added if it
         *  does.
         */
            String name = file.getName().toLowerCase(Locale.getDefault());
            for (String ext : validExtensions) {
                if (name.endsWith(ext)) {
                    return true;
                }
            }
        }
        return false;
    }
}
