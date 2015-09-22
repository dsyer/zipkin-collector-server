package launcher;

import java.sql.PreparedStatement;
import java.sql.Types;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.twitter.zipkin.gen.Annotation;
import com.twitter.zipkin.gen.BinaryAnnotation;
import com.twitter.zipkin.gen.Span;

@SpringBootApplication
@EnableBinding(Sink.class)
public class ZipkinCollectorLauncherApplication {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@ServiceActivator(inputChannel = Sink.INPUT)
	@Transactional
	public void sink(Span span) {
		long ts = System.currentTimeMillis();
		for (Annotation annotation : span.getAnnotations()) {
			this.jdbcTemplate.update(
					"INSERT INTO zipkin_annotations (trace_id,span_id,span_name,service_name,value,ipv4,port,a_timestamp) values (?,?,?,?,?,?,?,?)",
					(PreparedStatement ps) -> {
						ps.setLong(1, span.getTrace_id());
						ps.setLong(2, span.getId());
						String name = span.getName() == null ? null : span.getName();
						ps.setString(3, name);
						String service = annotation.getHost().getService_name() == null
								? null : annotation.getHost().getService_name();
						ps.setString(4, service);
						ps.setString(5, annotation.getValue());
						ps.setInt(6, annotation.getHost().getIpv4());
						ps.setInt(7, annotation.getHost().getPort());
						ps.setLong(8, annotation.getTimestamp());
					});
			if (annotation.getTimestamp()<ts) {
				ts = annotation.getTimestamp();
			}
		}
		final long createdTs = ts;
		if (this.jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM zipkin_spans where span_id={id}", Integer.class,
				span.getId()) == 0) {
			this.jdbcTemplate.update(
					"INSERT INTO zipkin_spans (trace_id,span_id,span_name,parent_id,created_ts) values (?,?,?,?,?)",
					(PreparedStatement ps) -> {
						ps.setLong(1, span.getTrace_id());
						ps.setLong(2, span.getId());
						if (span.getParent_id() != 0) {
							ps.setLong(4, span.getParent_id());
						}
						else {
							ps.setNull(4, Types.BIGINT);
						}
						String name = span.getName() == null ? null : span.getName();
						ps.setString(3, name);
						ps.setLong(5, createdTs);
					});
		}
		for (BinaryAnnotation annotation : span.getBinary_annotations()) {
			this.jdbcTemplate.update(
					"INSERT INTO zipkin_binary_annotations (trace_id,span_id,span_name,service_name,annotation_key,annotation_value,annotation_type_value,ipv4,port,annotation_ts) values (?,?,?,?,?,?,?,?,?,?)",
					(PreparedStatement ps) -> {
						ps.setLong(1, span.getTrace_id());
						ps.setLong(2, span.getId());
						String name = span.getName() == null ? null : span.getName();
						ps.setString(3, name);
						String service = annotation.getHost().getService_name() == null
								? null : annotation.getHost().getService_name();
						ps.setString(4, service);
						ps.setString(5, annotation.getKey());
						ps.setBytes(6, annotation.getValue());
						ps.setInt(7, annotation.getAnnotation_type().getValue());
						ps.setInt(8, annotation.getHost().getIpv4());
						ps.setInt(9, annotation.getHost().getPort());
						ps.setLong(10, createdTs);
					});
		}
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(ZipkinCollectorLauncherApplication.class, args);
	}
}
