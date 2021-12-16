package be.garagepoort.staffplusplus.discord.api;

import be.garagepoort.staffplusplus.discord.StaffPlusPlusDiscord;
import be.garagepoort.staffplusplus.discord.common.config.WebhookConfig;
import org.bukkit.Bukkit;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DiscordUtil {

    public static DiscordMessageFooter createFooter() {
        return new DiscordMessageFooter("Provided by Staff++", "https://cdn.discordapp.com/embed/avatars/0.png");
    }

    public static void sendEvent(DiscordClient client, WebhookConfig webhookConfig, String template) {
        Bukkit.getScheduler().runTaskAsynchronously(StaffPlusPlusDiscord.get(), () -> client.sendTemplate(webhookConfig.getApiKey(), template).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Call to discord failed: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable throwable) {
                throw new RuntimeException("Call to discord failed", throwable);
            }
        }));
    }
}
