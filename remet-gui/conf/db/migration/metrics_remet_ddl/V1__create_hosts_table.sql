CREATE TABLE remet.hosts (
    id SERIAL PRIMARY KEY,
    host_name VARCHAR(255) NOT NULL,
    cluster_name VARCHAR(255) NOT NULL,
    metrics_software_state VARCHAR(255) NOT NULL,
    host_name_idx tsvector
);

CREATE UNIQUE INDEX hosts_host_name ON remet.hosts (host_name);
CREATE INDEX hosts_cluster_name ON remet.hosts (cluster_name);

CREATE TRIGGER hosts_update_idx_host_name BEFORE INSERT OR UPDATE
ON remet.hosts FOR EACH ROW EXECUTE PROCEDURE
tsvector_update_trigger(host_name_idx, 'pg_catalog.english', host_name);
