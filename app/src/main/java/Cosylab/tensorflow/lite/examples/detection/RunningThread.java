package Cosylab.tensorflow.lite.examples.detection;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class RunningThread {


    public RunningThread(){

    }
    static Timer timer = null;
    static String thread_running_state = "false";

    static {
        try{
            timer = new Timer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //To check the thread running status
    public static String checkThreadState() throws Exception{
        if(thread_running_state.equals("False")){
             timer.schedule(new RunningThread.runThread(), 0, 5000);
        }
        return null;
    }


    private static class runThread extends TimerTask {
        @Override
        public void run() {
            thread_running_state = "true";
            String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
            Thread t = Thread.currentThread();
            String name  = t.getName();
            try{
                myMethodLogic();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private static String myMethodLogic() {
            return null;
        }
    }
}
