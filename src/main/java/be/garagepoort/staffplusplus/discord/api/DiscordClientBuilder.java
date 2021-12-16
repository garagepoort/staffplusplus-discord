package be.garagepoort.staffplusplus.discord.api;

import be.garagepoort.mcioc.IocBean;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.net.MalformedURLException;
import java.net.URL;

@IocBean
public class DiscordClientBuilder {

    public DiscordClient buildClient(String webhookUrl) {
        try {
            return new Retrofit.Builder()
                .baseUrl(new URL(webhookUrl))
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
                .create(DiscordClient.class);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed url", e);
        }
    }
}
