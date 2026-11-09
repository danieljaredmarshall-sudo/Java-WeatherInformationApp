import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class WeatherApp extends JFrame {

    private JTextField cityField;
    private JButton searchButton;
    private JComboBox<String> unitBox;

    private JLabel iconLabel;
    private JLabel temperatureLabel;
    private JLabel humidityLabel;
    private JLabel windSpeedLabel;
    private JLabel conditionLabel;

    private JTextArea forecastArea;
    private JTextArea historyArea;

    private JPanel mainPanel;
    private JPanel topPanel;
    private JPanel weatherPanel;
    private JPanel forecastPanel;
    private JPanel historyPanel;

    private final String API_KEY = "12c105c053ba90dd2efd815254eb2c09";

    public WeatherApp() {
        setupWindow();
        createComponents();
        layoutComponents();
        addButtonAction();
        changeBackgroundByTime();

        setVisible(true);
    }

    private void setupWindow() {
        setTitle("Weather Information App");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void createComponents() {
        cityField = new JTextField(20);
        searchButton = new JButton("Tell Me The Weather");

        unitBox = new JComboBox<>();
        unitBox.addItem("Celsius / m/s");
        unitBox.addItem("Fahrenheit / mph");

        iconLabel = new JLabel("Weather Status: Waiting", SwingConstants.CENTER);
        iconLabel.setFont(new Font("Arial", Font.BOLD, 18));

        temperatureLabel = new JLabel("Temperature: ");
        humidityLabel = new JLabel("Humidity: ");
        windSpeedLabel = new JLabel("Wind Speed: ");
        conditionLabel = new JLabel("Condition: ");

        forecastArea = new JTextArea(8, 35);
        forecastArea.setEditable(false);
        forecastArea.setLineWrap(true);
        forecastArea.setWrapStyleWord(true);

        historyArea = new JTextArea(6, 35);
        historyArea.setEditable(false);
        historyArea.setLineWrap(true);
        historyArea.setWrapStyleWord(true);
    }

    private void layoutComponents() {
        mainPanel = new JPanel(new BorderLayout(10, 10));

        topPanel = new JPanel();
        topPanel.add(new JLabel("Enter Your City: "));
        topPanel.add(cityField);
        topPanel.add(unitBox);
        topPanel.add(searchButton);

        weatherPanel = new JPanel(new GridLayout(5, 1, 5, 5));
        weatherPanel.setBorder(BorderFactory.createTitledBorder("Weather Right Now"));

        weatherPanel.add(iconLabel);
        weatherPanel.add(temperatureLabel);
        weatherPanel.add(humidityLabel);
        weatherPanel.add(windSpeedLabel);
        weatherPanel.add(conditionLabel);

        forecastPanel = new JPanel(new BorderLayout());
        forecastPanel.setBorder(BorderFactory.createTitledBorder("Short-Term Forecast"));
        forecastPanel.add(new JScrollPane(forecastArea), BorderLayout.CENTER);

        historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBorder(BorderFactory.createTitledBorder("Search History"));
        historyPanel.add(new JScrollPane(historyArea), BorderLayout.CENTER);

        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        centerPanel.add(forecastPanel);
        centerPanel.add(weatherPanel);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(historyPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void addButtonAction() {
        searchButton.addActionListener(e -> {
            String city = cityField.getText().trim();

            if (city.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter your city name.");
                return;
            }

            fetchWeatherData(city);
        });
    }

    private void fetchWeatherData(String city) {
        try {
            if (API_KEY.equals("PASTE_YOUR_API_KEY_HERE")) {
                JOptionPane.showMessageDialog(this, "Please add your OpenWeatherMap API key first.");
                return;
            }

            searchButton.setEnabled(false);
            searchButton.setText("Loading...");

            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);

            String units;
            String temperatureSymbol;
            String windSymbol;

            if (unitBox.getSelectedItem().equals("Celsius / m/s")) {
                units = "metric";
                temperatureSymbol = "°C";
                windSymbol = "m/s";
            } else {
                units = "imperial";
                temperatureSymbol = "°F";
                windSymbol = "mph";
            }

            String url = "https://api.openweathermap.org/data/2.5/weather?q="
                    + encodedCity
                    + "&appid="
                    + API_KEY
                    + "&units="
                    + units;

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                JOptionPane.showMessageDialog(this,
                        "Weather data could not be loaded.\n"
                                + "Please check the city name, internet connection, or API key.\n"
                                + "Status Code: " + response.statusCode());
                return;
            }

            String data = response.body();

            double temperature = extractDouble(data, "\"temp\":");
            int humidity = (int) extractDouble(data, "\"humidity\":");
            double windSpeed = extractDouble(data, "\"speed\":");
            String condition = extractString(data, "\"description\":\"");

            temperatureLabel.setText("Temperature: " + temperature + " " + temperatureSymbol);
            humidityLabel.setText("Humidity: " + humidity + "%");
            windSpeedLabel.setText("Wind Speed: " + windSpeed + " " + windSymbol);
            conditionLabel.setText("Condition: " + condition);

            updateWeatherIcon(condition);
            fetchForecastData(city, units, temperatureSymbol);
            addToHistory(city);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Something went wrong. Please check your internet connection or API key.");
        } finally {
            searchButton.setEnabled(true);
            searchButton.setText("Tell Me The Weather");
        }
    }

    private void fetchForecastData(String city, String units, String temperatureSymbol) {
        try {
            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);

            String url = "https://api.openweathermap.org/data/2.5/forecast?q="
                    + encodedCity
                    + "&appid="
                    + API_KEY
                    + "&units="
                    + units;

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                forecastArea.setText("Forecast data could not be loaded.");
                return;
            }

            String data = response.body();

            StringBuilder forecastText = new StringBuilder();
            forecastText.append("Short-Term Forecast:\n\n");

            int searchPosition = 0;

            for (int i = 0; i < 5; i++) {
                int dateIndex = data.indexOf("\"dt_txt\":\"", searchPosition);

                if (dateIndex == -1) {
                    break;
                }

                String dateTime = extractStringFromPosition(data, "\"dt_txt\":\"", dateIndex);
                double temp = extractLastDoubleBefore(data, "\"temp\":", dateIndex);
                String description = extractLastStringBefore(data, "\"description\":\"", dateIndex);

                forecastText.append(dateTime)
                        .append(" | ")
                        .append(temp)
                        .append(" ")
                        .append(temperatureSymbol)
                        .append(" | ")
                        .append(description)
                        .append("\n");

                searchPosition = dateIndex + 1;
            }

            forecastArea.setText(forecastText.toString());

        } catch (Exception ex) {
            forecastArea.setText("Something went wrong while loading the forecast.");
        }
    }

    private double extractDouble(String data, String key) {
        int startIndex = data.indexOf(key);

        if (startIndex == -1) {
            return 0;
        }

        startIndex = startIndex + key.length();
        int endIndex = startIndex;

        while (endIndex < data.length()
                && (Character.isDigit(data.charAt(endIndex))
                || data.charAt(endIndex) == '.'
                || data.charAt(endIndex) == '-')) {
            endIndex++;
        }

        return Double.parseDouble(data.substring(startIndex, endIndex));
    }

    private String extractString(String data, String key) {
        int startIndex = data.indexOf(key);

        if (startIndex == -1) {
            return "Unknown";
        }

        startIndex = startIndex + key.length();
        int endIndex = data.indexOf("\"", startIndex);

        if (endIndex == -1) {
            return "Unknown";
        }

        String result = data.substring(startIndex, endIndex);

        if (result.length() == 0) {
            return "Unknown";
        }

        return result.substring(0, 1).toUpperCase() + result.substring(1);
    }

    private String extractStringFromPosition(String data, String key, int startSearch) {
        int startIndex = data.indexOf(key, startSearch);

        if (startIndex == -1) {
            return "Unknown";
        }

        startIndex = startIndex + key.length();
        int endIndex = data.indexOf("\"", startIndex);

        if (endIndex == -1) {
            return "Unknown";
        }

        return data.substring(startIndex, endIndex);
    }

    private double extractLastDoubleBefore(String data, String key, int beforeIndex) {
        int startIndex = data.lastIndexOf(key, beforeIndex);

        if (startIndex == -1) {
            return 0;
        }

        startIndex = startIndex + key.length();
        int endIndex = startIndex;

        while (endIndex < data.length()
                && (Character.isDigit(data.charAt(endIndex))
                || data.charAt(endIndex) == '.'
                || data.charAt(endIndex) == '-')) {
            endIndex++;
        }

        return Double.parseDouble(data.substring(startIndex, endIndex));
    }

    private String extractLastStringBefore(String data, String key, int beforeIndex) {
        int startIndex = data.lastIndexOf(key, beforeIndex);

        if (startIndex == -1) {
            return "Unknown";
        }

        startIndex = startIndex + key.length();
        int endIndex = data.indexOf("\"", startIndex);

        if (endIndex == -1) {
            return "Unknown";
        }

        String result = data.substring(startIndex, endIndex);

        if (result.length() == 0) {
            return "Unknown";
        }

        return result.substring(0, 1).toUpperCase() + result.substring(1);
    }

    private void updateWeatherIcon(String condition) {
        condition = condition.toLowerCase();

        if (condition.contains("clear")) {
            iconLabel.setText("Weather Status: SUN / Clear");
        } else if (condition.contains("cloud")) {
            iconLabel.setText("Weather Status: CLOUD / Cloudy");
        } else if (condition.contains("rain")) {
            iconLabel.setText("Weather Status: RAIN / Rainy");
        } else if (condition.contains("thunder") || condition.contains("storm")) {
            iconLabel.setText("Weather Status: STORM / Stormy");
        } else if (condition.contains("snow")) {
            iconLabel.setText("Weather Status: SNOW / Snowy");
        } else if (condition.contains("mist") || condition.contains("fog")) {
            iconLabel.setText("Weather Status: FOG / Foggy");
        } else {
            iconLabel.setText("Weather Status: NORMAL");
        }
    }

    private void changeBackgroundByTime() {
        int hour = LocalDateTime.now().getHour();

        Color backgroundColor;

        if (hour >= 5 && hour < 12) {
            backgroundColor = new Color(210, 235, 255);
        } else if (hour >= 12 && hour < 17) {
            backgroundColor = new Color(230, 245, 255);
        } else if (hour >= 17 && hour < 20) {
            backgroundColor = new Color(255, 220, 180);
        } else {
            backgroundColor = new Color(210, 210, 230);
        }

        mainPanel.setBackground(backgroundColor);
        topPanel.setBackground(backgroundColor);
        weatherPanel.setBackground(backgroundColor);
        forecastPanel.setBackground(backgroundColor);
        historyPanel.setBackground(backgroundColor);
    }

    private void addToHistory(String city) {
        String time = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        historyArea.append(city + " - " + time + "\n");
    }

    public static void main(String[] args) {
        new WeatherApp();
    }
}