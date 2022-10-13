package services;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import domain.Power;
import interfaces.EventListener;
import secret.HiveMQSecretsProd;

import static com.hivemq.client.mqtt.MqttGlobalPublishFilter.ALL;
import static java.nio.charset.StandardCharsets.UTF_8;

public class HiveMQService implements EventListener {
    final String host = HiveMQSecretsProd.host;
    final String username = HiveMQSecretsProd.username;
    final String password = HiveMQSecretsProd.password;

    private Mqtt5BlockingClient client;

    public HiveMQService() {
        initClient();
        connectClient();
    }

    private void initClient() {
        client = MqttClient.builder()
            .useMqttVersion5()
            .serverHost(host)
            .serverPort(8883)
            .sslWithDefaultConfig()
            .buildBlocking();
    }

    private void connectClient() {
        client.connectWith()
            .simpleAuth()
            .username(username)
            .password(UTF_8.encode(password))
            .applySimpleAuth()
            .send();
    }

    public void sendDataToTopic(String topic, String data) {
        client.publishWith()
            .topic(topic)
            .payload(UTF_8.encode(data))
            .send();
    }

    public void subscribeToTopic(String topic) {
        client.subscribeWith()
            .topicFilter(topic)
            .send();
    }

    public void getMessagesFromSubscribedTopics() {
        client.toAsync().publishes(ALL, publish -> {
            System.out.println("Received message: " +
                    publish.getTopic() + " -> " +
                    UTF_8.decode(publish.getPayload().get()));

            // disconnect the client after a message was received
            //client.disconnect();
        });
    }

    @Override
    public void update(Power power) {
        this.sendDataToTopic("energy/1", String.valueOf(power.getUnixtimestamp() + ":" + power.getTotalpower()));
    }
}
