package be.garagepoort.staffplusplus.discord.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface DiscordClient {

    @POST("{apiKey}")
    @Headers("Content-Type: application/json")
    Call<Void> sendTemplate(@Path("apiKey") String apiKey, @Body String template);
}
