package launcher;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.TimelineAnnotation;
import org.springframework.cloud.sleuth.stream.Host;
import org.springframework.cloud.sleuth.stream.SleuthSink;
import org.springframework.cloud.sleuth.stream.Spans;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.util.StringUtils;

import io.zipkin.Annotation;
import io.zipkin.BinaryAnnotation;
import io.zipkin.BinaryAnnotation.Type;
import io.zipkin.Endpoint;
import io.zipkin.Span.Builder;
import io.zipkin.SpanStore;
import lombok.extern.apachecommons.CommonsLog;

@MessageEndpoint
@CommonsLog
public class ZipkinMessageListener {

	@Autowired
	SpanStore spanStore;

	@ServiceActivator(inputChannel = SleuthSink.INPUT)
	public void sink(Spans input) throws TException {
		List<io.zipkin.Span> spans = new ArrayList<>();
		for (Span span : input.getSpans()) {
			if (!span.getName().equals("message/" + SleuthSink.INPUT)) {
				spans.add(convert(span, input.getHost()));
			} else {
				log.warn("Message tracing cycle detected for: " + span);
			}
		}
		if (!spans.isEmpty()) {
			this.spanStore.accept(spans);
		}
	}

	/**
	 * Converts a given Sleuth span to a Zipkin Span.
	 * <ul>
	 * <li>Set ids, etc
	 * <li>Create timeline annotations based on data from Span object.
	 * <li>Create binary annotations based on data from Span object.
	 * </ul>
	 */
	public io.zipkin.Span convert(Span span, Host host) {
		Builder zipkinSpan = new io.zipkin.Span.Builder();

		Endpoint ep = Endpoint.create(host.getServiceName(), host.getIpv4(),
				host.getPort().shortValue());
		List<Annotation> annotationList = createZipkinAnnotations(span, ep);
		List<BinaryAnnotation> binaryAnnotationList = createZipkinBinaryAnnotations(span,
				ep);
		zipkinSpan.traceId(hash(span.getTraceId()));
		if (span.getParents().size() > 0) {
			if (span.getParents().size() > 1) {
				log.error("zipkin doesn't support spans with multiple parents.  Omitting "
						+ "other parents for " + span);
			}
			zipkinSpan.parentId(hash(span.getParents().get(0)));
		}
		zipkinSpan.id(hash(span.getSpanId()));
		if (StringUtils.hasText(span.getName())) {
			zipkinSpan.name(span.getName());
		}
		for (Annotation annotation : annotationList) {
			zipkinSpan.addAnnotation(annotation);
		}
		for (BinaryAnnotation annotation : binaryAnnotationList) {
			zipkinSpan.addBinaryAnnotation(annotation);
		}
		return zipkinSpan.build();
	}

	/**
	 * Add annotations from the sleuth Span.
	 */
	private List<Annotation> createZipkinAnnotations(Span span, Endpoint endpoint) {
		List<Annotation> annotationList = new ArrayList<>();
		for (TimelineAnnotation ta : span.getTimelineAnnotations()) {
			Annotation zipkinAnnotation = createZipkinAnnotation(ta.getMsg(),
					ta.getTime(), endpoint, true);
			annotationList.add(zipkinAnnotation);
		}
		return annotationList;
	}

	/**
	 * Creates a list of Annotations that are present in sleuth Span object.
	 *
	 * @return list of Annotations that could be added to Zipkin Span.
	 */
	private List<BinaryAnnotation> createZipkinBinaryAnnotations(Span span,
			Endpoint endpoint) {
		List<BinaryAnnotation> l = new ArrayList<>();
		for (Map.Entry<String, String> e : span.getAnnotations().entrySet()) {
			BinaryAnnotation.Builder binaryAnn = new BinaryAnnotation.Builder();
			binaryAnn.type(Type.STRING);
			binaryAnn.key(e.getKey());
			try {
				binaryAnn.value(e.getValue().getBytes("UTF-8"));
			}
			catch (UnsupportedEncodingException ex) {
				log.error("Error encoding string as UTF-8", ex);
			}
			binaryAnn.endpoint(endpoint);
			l.add(binaryAnn.build());
		}
		return l;
	}

	/**
	 * Create an annotation with the correct times and endpoint.
	 *
	 * @param value Annotation value
	 * @param time timestamp will be extracted
	 * @param endpoint the endpoint this annotation will be associated with.
	 * @param sendRequest use the first or last timestamp.
	 */
	private static Annotation createZipkinAnnotation(String value, long time,
			Endpoint endpoint, boolean sendRequest) {
		Annotation.Builder annotation = new Annotation.Builder();
		annotation.endpoint(endpoint);

		// Zipkin is in microseconds
		if (sendRequest) {
			annotation.timestamp(time * 1000);
		}
		else {
			annotation.timestamp(time * 1000);
		}
		annotation.value(value);
		return annotation.build();
	}

	private static long hash(String string) {
		long h = 1125899906842597L;
		if (string == null) {
			return h;
		}
		int len = string.length();

		for (int i = 0; i < len; i++) {
			h = 31 * h + string.charAt(i);
		}
		return h;
	}

}
