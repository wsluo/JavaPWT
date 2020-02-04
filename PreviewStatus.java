/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javapwt;

/**
 *
 * @author wawnx
 */
public class PreviewStatus {
    public PreviewStatus(String path, Boolean viewed){
        this.filepath=path;
        this.previewed=viewed;
    }
    private String filepath;
    private Boolean previewed;
    public String getPath(){
        return filepath;
    }
    public Boolean getPreviewStatus(){
        return previewed;
    }
    public void setPath(String path){
        this.filepath=path;
    }
    public void setPreviewStatus(Boolean viewed){
        this.previewed=viewed;
    }
}
