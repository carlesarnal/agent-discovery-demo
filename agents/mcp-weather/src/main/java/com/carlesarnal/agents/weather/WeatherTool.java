package com.carlesarnal.agents.weather;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.TextContent;

import java.util.Map;

public class WeatherTool {

    private static final Map<String, String> WEATHER_DATA = Map.of(
            "amsterdam", "Amsterdam: 18C, partly cloudy, wind 15km/h W",
            "london", "London: 14C, overcast, light rain expected",
            "paris", "Paris: 22C, sunny, humidity 45%",
            "berlin", "Berlin: 20C, clear skies, wind 10km/h NE",
            "madrid", "Madrid: 32C, sunny, UV index high",
            "oslo", "Oslo: 12C, cloudy, chance of rain 60%",
            "munich", "Munich: 19C, partly cloudy, wind 8km/h S"
    );

    @Tool(description = "Get current weather for a city in Europe")
    public TextContent getWeather(@ToolArg(description = "City name") String city) {
        String weather = WEATHER_DATA.getOrDefault(
                city.toLowerCase().trim(),
                city + ": Weather data not available. Try Amsterdam, London, Paris, Berlin, Madrid, Oslo, or Munich.");
        return new TextContent(weather);
    }
}
