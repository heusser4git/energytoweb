import services.Database;
import services.HiveMQService;
import services.PvOutputService;
import services.ShellyEM3Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        PvOutputService pvOutputService = new PvOutputService(false);
        HiveMQService hiveMQService = new HiveMQService();
        ShellyEM3Service shellyEM3Service = new ShellyEM3Service("192.168.1.3");
        shellyEM3Service.addListener(pvOutputService);
        //shellyEM3Service.addListener(hiveMQService);

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        Runnable task = () -> {
            shellyEM3Service.run();
        };
        executor.scheduleAtFixedRate(task,0,1, TimeUnit.MINUTES);

//        hiveMQService.subscribeToTopic("energy/1");
//        hiveMQService.getMessagesFromSubscribedTopics();
    }
}
