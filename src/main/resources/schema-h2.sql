CREATE TABLE IF NOT EXISTS zipkin_spans (
  `trace_id` BIGINT NOT NULL,
  `id` BIGINT NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `parent_id` BIGINT,
  `debug` BIT(1),
  `start_ts` BIGINT
);

CREATE TABLE IF NOT EXISTS zipkin_annotations (
  `trace_id` BIGINT NOT NULL,
  `span_id` BIGINT NOT NULL,
  `a_key` VARCHAR(255) NOT NULL,
  `a_value` BLOB,
  `a_type` INT NOT NULL,
  `a_timestamp` BIGINT,
  `endpoint_ipv4` INT,
  `endpoint_port` SMALLINT,
  `endpoint_service_name` VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS zipkin_dependencies (
  dlid BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  start_ts BIGINT NOT NULL,
  end_ts BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS zipkin_dependency_links (
  dlid BIGINT NOT NULL,
  parent VARCHAR(255) NOT NULL,
  child VARCHAR(255) NOT NULL,
  call_count BIGINT NOT NULL
);
