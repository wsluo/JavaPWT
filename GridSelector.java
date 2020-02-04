/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gridselection;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.*;

/**
 *
 * @author wawnx
 */
public class GridSelector extends JFrame {

    private int maxX = 0;
    private int maxY = 0;
    
    private JPanel selectionPanel;
    private JPanel okPanel;
    JButton[] grid;
    
    private int startXLoc = 0;
    private int startYLoc = 0;
    private int endXLoc = 0;
    private int endYLoc = 0;
    
    private int startX=-1;
    private int startY=-1;
    private int endX=-1;
    private int endY=-1;
        
    
    private JButton okButton;

    public GridSelector(int x, int y) {
        super("Grid Selector");
        this.setLayout(new GridBagLayout());
        
        maxX = x;
        maxY = y;
        grid = new JButton[x * y];
        selectionPanel = new JPanel(new GridLayout(x, y));
        okPanel = new JPanel();

        for (int i = 0; i < x * y; i++) {
            grid[i] = new JButton();
            grid[i].setPreferredSize(new Dimension(50, 40));
            grid[i].addMouseListener(new MouseListener() {

                @Override
                public void mouseClicked(MouseEvent me) {
                }

                @Override
                public void mousePressed(MouseEvent me) {
                    for (int i = 0; i < maxX * maxY; i++) {
                        grid[i].setBackground(new JButton().getBackground());
                    }

                    JButton a = (JButton) me.getComponent();
                    for (int i = 0; i < maxX * maxY; i++) {
                        if (a.equals(grid[i])) {
                            grid[i].setBackground(Color.yellow);
                            startXLoc = grid[i].getLocation().x;
                            startYLoc = grid[i].getLocation().y;
                            break;
                        }
                    }

                }

                @Override
                public void mouseReleased(MouseEvent me) {
                }

                @Override
                public void mouseEntered(MouseEvent me) {
                }

                @Override
                public void mouseExited(MouseEvent me) {
                }
            });


            grid[i].addMouseMotionListener(new MouseMotionListener() {

                @Override
                public void mouseDragged(MouseEvent me) {
                    endXLoc = me.getX();
                    endYLoc = me.getY();
                    for (int i = 0; i < maxX * maxY; i++) {
                        if (grid[i].getLocation().x >= startXLoc && grid[i].getLocation().x <= endXLoc && grid[i].getLocation().y >= startYLoc && grid[i].getLocation().y <= endYLoc) {
                            grid[i].setBackground(Color.yellow);
                        } else {
                            grid[i].setBackground(new JButton().getBackground());
                        }
                    }

                }

                @Override
                public void mouseMoved(MouseEvent me) {
                }
            });



            selectionPanel.add(grid[i]);
        }

        okButton = new JButton("OK");
        okPanel.add(okButton);
        okPanel.setPreferredSize(new Dimension(selectionPanel.getSize().width, 50));
        
        okButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                boolean first=true;
                boolean last=true;
                int start=0; //keep the smallest index
                int end=0;
                for(int i=0;i<maxX*maxY;i++){                
                    if(grid[i].getBackground().equals(Color.yellow)){            
                       if(first){
                       first=false;
                       start=i;                       
                       }
                    }
                    if(grid[maxX*maxY-i-1].getBackground().equals(Color.yellow)){
                        if(last){
                            last=false;
                            end=maxX*maxY-i-1;
                        }
                    }
                } //end of for loop
                
                startX=start/maxY;
                startY=start%maxY;
                endX=end/maxY;
                endY=end%maxY;
                
                System.out.println("from "+startX+","+startY+" to "+endX+","+endY);
            }
        });

        GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
        gridBagConstraints1.gridx = 0;
        gridBagConstraints1.gridy = 0;
        gridBagConstraints1.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints1.anchor = GridBagConstraints.CENTER;
        gridBagConstraints1.insets = new Insets(4, 4, 4, 4);


        GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
        gridBagConstraints2.gridx = 0;
        gridBagConstraints2.gridy = 1;
        gridBagConstraints2.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints2.anchor = GridBagConstraints.CENTER;
        gridBagConstraints2.insets = new Insets(4, 4, 4, 4);
        this.add(selectionPanel, gridBagConstraints1);
        this.add(okPanel, gridBagConstraints2);

        this.pack();
        this.setVisible(true);
    }
    
    
    public int getStartX(){
        return startX;
    }
    
    public int getStartY(){
        return startY;
    }
    
    public int getEndX(){
        return endX;        
    }
    
    public int getEndY(){
        return endY;
    }
    
    public void reset(){
        startX=-1;
        endX=-1;
        startY=-1;
        endY=-1;
    }
    
    
}
