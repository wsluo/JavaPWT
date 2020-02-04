/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package javapwt;
import java.util.concurrent.*;
import java.util.*;
/**
 *
 * @author wawnx
 */
public class MyThreadPoolExecutor extends ThreadPoolExecutor{

    
    public MyThreadPoolExecutor(int pSize,int mpSize,long kT,TimeUnit unit, ArrayBlockingQueue<Runnable>q){
        super(pSize,mpSize,kT,unit,q);      
    }
    
    public void runTask(Runnable task){
        this.execute(task);
        System.out.println("Task num:"+this.getQueue().size());
    }
  
}
