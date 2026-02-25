package ai.planmate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.tracing.wavefront.WavefrontTracingAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {WavefrontTracingAutoConfiguration.class})
@EnableScheduling
@SuppressWarnings({"checkstyle:FinalClass", "checkstyle:HideUtilityClassConstructor"})
public class PlanmateApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlanmateApiApplication.class, args);
    }
}
