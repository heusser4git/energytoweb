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
    private final Database database;

    public PvOutputService(boolean testmode) {
        this.database = new Database("PvOutput.db");

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
            writeToPVoutput(currentPower);
            writeDbEntriesToWeb();
        }
    }

    private void writeToPVoutput(Power power) {
        Date date = new Date((long)power.getUnixtimestamp() * 1000);
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

            // return it as a String
            String result = EntityUtils.toString(entity);
            System.out.println(result);
        } catch (IOException e) {
            System.out.println("writeToPVoutput - no Connection: " + e.getMessage());
            // no internet write to db
            writeToDb(date, power);
            //throw new RuntimeException(e);
        }
    }

    private void writeDbEntriesToWeb() {
        for (Power power : database.read()) {
            writeToPVoutput(power);
            database.delPower(power);
            try {
                // sleep because there are only 60 entries per hour allowed on pvOutput.org
                Thread.sleep(61000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void writeToDb(Date date, Power power) {
        power.setUnixtimestamp((int) (date.getTime()/1000));
        database.addPower(power);
    }

    public void setCurrentPower(Power currentPower) {
        System.out.println("setCurrentPower: "+currentPower.getTotalpower());
        this.currentPower = currentPower;
    }
}
