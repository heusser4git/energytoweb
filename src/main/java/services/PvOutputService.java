package services;


import domain.Power;
import interfaces.EventListener;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import secret.PVOutputSecretsProd;
import secret.PVOutputSecretsTest;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PvOutputService implements EventListener {
    int sysid;
    String apikey;
    private final CloseableHttpClient httpClient = HttpClients.createDefault();
    private Power currentPower;
    private int laststamp = 0;

    public PvOutputService(boolean testmode) {
        if(testmode) {
            sysid = PVOutputSecretsProd.systemId;
            apikey = PVOutputSecretsProd.apikey;
        } else {
            sysid = PVOutputSecretsTest.systemId;
            apikey = PVOutputSecretsTest.apikey;
        }
    }

    @Override
    public void update(Power power) {
        setCurrentPower(power);
                        System.out.println("timeSinceLast writeToWeb: "+ (power.getUnixtimestamp() - laststamp));
        boolean lastWebEntryMoreThanFiveMinutesAgo = laststamp == 0 || (power.getUnixtimestamp() - laststamp) >= 300;
        if(lastWebEntryMoreThanFiveMinutesAgo) {
            writeToWeb();
            laststamp = power.getUnixtimestamp();
        }
    }

    public void writeToWeb() {
        if(currentPower != null) {
            System.out.println("writeToWeb -> Power: " + currentPower.getTotalpower());
            Date date = new Date();
            try {
                writeToPVoutput(date, currentPower);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void writeToPVoutput(Date date, Power power) throws IOException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
        String datum = simpleDateFormat.format(date);

        SimpleDateFormat simpleDateFormatZeit = new SimpleDateFormat("HH:mm");
        String zeit = simpleDateFormatZeit.format(date);

        if(currentPower.getTotalpower()<0) {
            // minuswerte werden von pvoutput.org nicht akzeptiert
            currentPower.setTotalpower(0);
        }

        HttpGet request = new HttpGet("https://pvoutput.org/service/r2/addstatus.jsp?key="+apikey+"&sid="+sysid+"&d="+datum+"&t="+zeit+"&v4="+Integer.valueOf((int) power.getTotalpower()));
        System.out.println(request.getURI());
        // add request headers
        request.addHeader(HttpHeaders.USER_AGENT, "Java8 PVOutput");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
            Header headers = entity.getContentType();
            System.out.println(headers);

            if (entity != null) {
                // return it as a String
                String result = EntityUtils.toString(entity);
                System.out.println(result);
            }
        }
        // TODO catch the connection-Error and try to reconnect... otherwise the app shuts down
    }

    public void setCurrentPower(Power currentPower) {
        System.out.println("setCurrentPower: "+currentPower.getTotalpower());
        this.currentPower = currentPower;
    }
}