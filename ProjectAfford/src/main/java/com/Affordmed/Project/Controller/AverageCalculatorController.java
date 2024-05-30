package com.Affordmed.Project.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
public class AverageCalculatorController {

    private static final int WINDOW_SIZE = 10;
    private static final String TEST_SERVER_URL = "http://example.com/api/numbers/{type}";
    private final RestTemplate restTemplate;
    private final Set<Integer> numberWindow = new LinkedHashSet<>();

    public AverageCalculatorController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/numbers/{type}")
    public ResponseEntity<AverageResponse> getNumbers(@PathVariable String type,
                                                      @RequestHeader(value = "Authorization", required = true) String authHeader) {
        validateType(type);
        List<Integer> newNumbers = fetchNumbersFromTestServer(type, authHeader);

        synchronized (numberWindow) {
            updateNumberWindow(newNumbers);
            double average = calculateAverage(numberWindow);
            return ResponseEntity.ok(new AverageResponse(newNumbers, average, numberWindow));
        }
    }

    private void validateType(String type) {
        if (!List.of("p", "f", "e", "r").contains(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid type");
        }
    }

    private List<Integer> fetchNumbersFromTestServer(String type, String authHeader) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authHeader);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List> response = restTemplate.exchange(TEST_SERVER_URL, HttpMethod.GET, entity, List.class, type);
            return (List<Integer>) response.getBody();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Error fetching numbers from test server");
        }
    }

    private void updateNumberWindow(List<Integer> newNumbers) {
        for (Integer number : newNumbers) {
            if (numberWindow.size() >= WINDOW_SIZE) {
                numberWindow.remove(numberWindow.iterator().next());
            }
            numberWindow.add(number);
        }
    }

    private double calculateAverage(Set<Integer> numbers) {
        return numbers.stream().mapToDouble(Integer::doubleValue).average().orElse(0);
    }

    static class AverageResponse {
        private final List<Integer> numbers;
        private final List<Integer> windowPrevState;
        private final List<Integer> windowCurrState;
        private final double avg;

        public AverageResponse(List<Integer> newNumbers, double avg, Set<Integer> currentWindow) {
            this.numbers = newNumbers;
            this.windowPrevState = List.copyOf(currentWindow);  // Save previous state correctly
            this.windowCurrState = newNumbers;  // This needs adjustment to reflect actual current state after processing
            this.avg = avg;
        }

        // Getters
        public List<Integer> getNumbers() {
            return numbers;
        }

        public List<Integer> getWindowPrevState() {
            return windowPrevState;
        }

        public List<Integer> getWindowCurrState() {
            return windowCurrState;
        }

        public double getAvg() {
            return avg;
        }
    }
}