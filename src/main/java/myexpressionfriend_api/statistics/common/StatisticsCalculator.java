package myexpressionfriend_api.statistics.common;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class StatisticsCalculator {

    public double trendSlope(List<Double> values) {
        if (values == null || values.size() < 2) {
            return 0.0;
        }

        int n = values.size();
        double meanX = (n + 1) / 2.0;
        double meanY = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double numerator = 0.0;
        double denominator = 0.0;

        for (int i = 0; i < n; i++) {
            double x = i + 1;
            double dx = x - meanX;
            numerator += dx * (values.get(i) - meanY);
            denominator += dx * dx;
        }

        return denominator == 0.0 ? 0.0 : numerator / denominator;
    }

    public String trendDirection(double slope) {
        if (slope >= 0.03) return "IMPROVING";
        if (slope <= -0.03) return "DECLINING";
        return "STABLE";
    }

    public double confidenceScore(int sessionCount, Double consistencyStd, Instant lastPlayedAt) {
        double countScore = Math.min(sessionCount / 8.0, 1.0);
        double consistencyScore = consistencyStd == null
                ? (sessionCount >= 3 ? 0.6 : 0.3)
                : Math.max(0.0, 1.0 - Math.min(consistencyStd / 0.35, 1.0));
        double recencyScore = recencyScore(lastPlayedAt);
        return clamp((countScore * 0.45) + (consistencyScore * 0.35) + (recencyScore * 0.20));
    }

    public String confidenceLevel(double score) {
        if (score >= 0.75) return "HIGH";
        if (score >= 0.45) return "MEDIUM";
        return "LOW";
    }

    private double recencyScore(Instant lastPlayedAt) {
        if (lastPlayedAt == null) {
            return 0.0;
        }
        long days = Math.max(0, Duration.between(lastPlayedAt, Instant.now()).toDays());
        if (days <= 7) return 1.0;
        if (days >= 42) return 0.0;
        return 1.0 - ((days - 7) / 35.0);
    }

    public double stddev(List<Double> values) {
        if (values == null || values.size() < 2) return 0.0;
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0.0);
        return Math.sqrt(variance);
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
