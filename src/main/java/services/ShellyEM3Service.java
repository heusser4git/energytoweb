package services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.Power;
import interfaces.EventListener;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ShellyEM3Service {
    private List<EventListener> listeners = new ArrayList<>();
    private final CloseableHttpClient httpClient = HttpClients.createDefault();
    private Power currentPower = new Power();
    private String ipaddress;

    public ShellyEM3Service(String ipaddress) {
        this.ipaddress = ipaddress;
    }

    public void addListener(EventListener eventListener) {
        listeners.add(eventListener);
    }

    private void callListeners() {
        for (EventListener listener : listeners) {
            listener.update(currentPower);
        }
    }

    public void run() {
        getShellyDataByHttpGet();
        callListeners();
    }
    private void getShellyDataByHttpGet() {
        HttpGet request = new HttpGet("http://" + ipaddress + "/status");
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                // return it as a String
                String json = EntityUtils.toString(entity);
                if(json.length()>0) {
                    getCurrentPowerFromShellyJson(json);
                }
            }
        } catch (IOException e) {
            System.out.println("Probleme bei der GET Abfrage des Shelly: Versuche in 5s nochmals.");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            getShellyDataByHttpGet();
            //throw new RuntimeException(e);
        }
    }

    private void getCurrentPowerFromShellyJson(String json) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(json);
        JsonNode totalPowerNode = rootNode.path("total_power");
        JsonNode unixTimstampNode = rootNode.path("unixtime");

        currentPower.setTotalpower(totalPowerNode.asDouble());
        currentPower.setUnixtimestamp(unixTimstampNode.asInt());
    }

    public Power getCurrentPower() {
        return currentPower;
    }
}
