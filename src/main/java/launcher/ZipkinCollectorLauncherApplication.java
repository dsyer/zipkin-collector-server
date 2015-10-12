package launcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.sleuth.stream.SleuthStreamAutoConfiguration;
import org.springframework.cloud.sleuth.stream.SleuthSink;
import org.springframework.cloud.stream.annotation.EnableBinding;

import io.zipkin.server.EnableZipkinServer;

@SpringBootApplication(exclude = SleuthStreamAutoConfiguration.class)
@EnableBinding(SleuthSink.class)
@EnableZipkinServer
public class ZipkinCollectorLauncherApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(ZipkinCollectorLauncherApplication.class, args);
	}

}
