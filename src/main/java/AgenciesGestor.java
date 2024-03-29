import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import static spark.Spark.*;

public class AgenciesGestor {
    public static void main(String[] args) {
        get("/agencias", (req, res) -> {
            try {
                res.type("application/json");

                String siteId = req.queryParams("site_id");
                String paymentMethod = req.queryParams("payment_method_id");
                String zipCode = req.queryParams("zip_code");
                String nearTo = req.queryParams("near_to");

                String order = req.queryParams("order");

                String mapsUrl = "https://maps.googleapis.com/maps/api/geocode/json?address="+zipCode+"&key=AIzaSyBfnGx5oJwDXT_XsdqMsF1moWRH0NQYKr0";

                JsonArray mapResults = (JsonArray) readUrl(mapsUrl);
                JsonObject location =  (JsonObject) mapResults.get(0);
                String latitude = location.getAsJsonObject("geometry").getAsJsonObject("location").get("lat").getAsString();
                String longitude = location.getAsJsonObject("geometry").getAsJsonObject("location").get("lng").getAsString();
                String agenciesUrl = null;
                if(nearTo != null) {
                    agenciesUrl = "https://api.mercadolibre.com/sites/" + siteId + "/payment_methods/" + paymentMethod + "/agencies?near_to=" + nearTo;
                } else {
                    agenciesUrl = "https://api.mercadolibre.com/sites/" + siteId + "/payment_methods/" + paymentMethod + "/agencies?near_to=" + latitude + "," + longitude;
                }
                System.out.println(agenciesUrl);
                JsonArray data = (JsonArray) readUrl(agenciesUrl);
                TypeToken<ArrayList<Agency>> token = new TypeToken<ArrayList<Agency>>() {};
                ArrayList<Agency> agencias = new Gson().fromJson(data, token.getType());
                logAcceso(agenciesUrl);

                if (order != null) {
                    setCriterio(order);
                    Operador.ordenarArreglo(agencias);
                }
                for (int i = 0; i < agencias.size(); i++) {
                    System.out.println(agencias.get(i));
                }
                System.out.println(data);
                return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS, data));
            } catch(IOException e) {
                e.printStackTrace();
                return new Gson().toJson(new StandardResponse(StatusResponse.ERROR, ""));
            }
        });

        System.out.println();
    }

    public static JsonElement readUrl(String urlString) throws IOException {
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            char[] chars = new char[1024];
            StringBuffer buffer = new StringBuffer();
            int read = 0;
            while ((read = reader.read(chars)) != -1) {
                buffer.append(chars, 0, read);
            }
            JsonParser parser = new JsonParser();
            JsonObject resultObj = parser.parse(buffer.toString()).getAsJsonObject();
            JsonArray resultString = (JsonArray) resultObj.get("results");
            return resultString;
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    public static void setCriterio(String order) {
        System.out.println(order);
        if (order.toLowerCase().equals("distancia")) {
            Agency.criterio = Agency.Criterio.DISTANCE;
            return;
        }
        if (order.toLowerCase().equals("address_line")) {
            System.out.println("Entras");
            Agency.criterio = Agency.Criterio.ADRESS_LINE;
            return;
        }
        Agency.criterio = Agency.Criterio.AGENCY_CODE;

    }

    public static void logAcceso(String url) {
        BufferedWriter writer = null;
        try {
            File file = new File("./api.log");
            if(!file.exists()) {
                file.createNewFile();
            }
            writer = new BufferedWriter(new FileWriter(file, true));
            Date fecha = new Date();
            String log = fecha.toString() +"\t"+url;
            System.out.println(log);
            writer.write(log);
            writer.newLine();
            writer.close();
        } catch(IOException e) {
            System.out.println("No se pudo crear el archivo de log");
        }
    }
}
