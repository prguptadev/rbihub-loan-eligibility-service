package in.rbihub.lending;

import in.rbihub.lending.config.LendingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(LendingProperties.class)
public class LoanEligibilityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanEligibilityServiceApplication.class, args);
    }
}
