/**
 * Copyright 2015 Groupon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

 create table clusteragg.partition_set (
  id SERIAL PRIMARY KEY,
  version INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now(),
  name VARCHAR(255) NOT NULL,
  count INTEGER NOT NULL DEFAULT 0,
  maximum_partitions INTEGER NOT NULL DEFAULT 0,
  maximum_entries_per_partition INTEGER NOT NULL DEFAULT 0,
  is_full BOOLEAN NOT NULL DEFAULT FALSE,
  UNIQUE (name)
);

create table clusteragg.partition (
  id SERIAL PRIMARY KEY,
  version INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now(),
  partition_number INTEGER NOT NULL,
  count INTEGER NOT NULL DEFAULT 0,
  partition_set_id BIGINT NOT NULL REFERENCES clusteragg.partition_set (id),
  UNIQUE (partition_set_id, partition_number)
);

create table clusteragg.partition_entry (
  id SERIAL PRIMARY KEY,
  key VARCHAR(255) NOT NULL,
  version INTEGER NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now(),
  partition_id INTEGER NOT NULL REFERENCES clusteragg.partition (id),
  UNIQUE (partition_id, key)
);
