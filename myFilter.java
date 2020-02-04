/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javapwt;

import java.io.File;

/**
 *
 * @author wawnx
 */
public class myFilter extends javax.swing.filechooser.FileFilter implements java.io.FileFilter {
        String suffix = null;

    public myFilter(String suf) {
        this.suffix = suf;
    }

  
    @Override
    public boolean accept(File f) {
        return (f.getAbsolutePath().endsWith(this.suffix)||f.isDirectory());
    }

 
    public String getDescription() {
        return this.suffix;
    }
    
}


