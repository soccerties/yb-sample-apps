// Copyright (c) YugaByte, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distributed under the License
// is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
// or implied.  See the License for the specific language governing permissions and limitations
// under the License.
//

package com.yugabyte.sample.apps;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;

/**
 * This workload writes and reads some random string keys from a CQL server. By default, this app
 * inserts a million keys, and reads/updates them indefinitely.
 */
public class CassandraKeyValue extends CassandraKeyValueBase {
  private static final Logger LOG = Logger.getLogger(CassandraKeyValue.class);

  // The default table name to create and use for CRUD ops.
  private static final String DEFAULT_TABLE_NAME = CassandraKeyValue.class.getSimpleName();
  static {
    // The number of keys to read.
    appConfig.numKeysToRead = NUM_KEYS_TO_READ_FOR_YSQL_AND_YCQL;
    // The number of keys to write. This is the combined total number of inserts and updates.
    appConfig.numKeysToWrite = NUM_KEYS_TO_WRITE_FOR_YSQL_AND_YCQL;
    // The number of unique keys to write. This determines the number of inserts (as opposed to
    // updates).
    appConfig.numUniqueKeysToWrite = NUM_UNIQUE_KEYS_FOR_YSQL_AND_YCQL;
  }
  @Override
  public List<String> getCreateTableStatements() {
    String create_stmt = String.format(
      "CREATE TABLE IF NOT EXISTS %s (k varchar, v blob, primary key (k))", getTableName());

    if (appConfig.tableTTLSeconds > 0) {
      create_stmt += " WITH default_time_to_live = " + appConfig.tableTTLSeconds;
    }
    create_stmt += ";";
    return Arrays.asList(create_stmt);
  }

  @Override
  protected String getDefaultTableName() {
    return DEFAULT_TABLE_NAME;
   }

  protected PreparedStatement getPreparedInsert() {
    return getPreparedInsert(String.format("INSERT INTO %s (k, v) VALUES (?, ?);", getTableName()));
  }

  @Override
  protected BoundStatement bindSelect(String key)  {
    PreparedStatement prepared_stmt = getPreparedSelect(
        String.format("SELECT k, v FROM %s WHERE k = ?;", getTableName()));
    BoundStatement boundStmt = prepared_stmt.bind(key);
    if (appConfig.localReads) {
      LOG.debug("Doing local reads");
      boundStmt.setConsistencyLevel(ConsistencyLevel.ONE);
    }

    return boundStmt;
  }

  @Override
  protected BoundStatement bindInsert(String key, ByteBuffer value)  {
    return getPreparedInsert().bind(key, value);
   }

  @Override
  public List<String> getWorkloadDescription() {
    return Arrays.asList(
      "Sample key-value app built on Cassandra with concurrent reader and writer threads.",
      " Each of these threads operates on a single key-value pair. The number of readers ",
      " and writers, the value size, the number of inserts vs updates are configurable. " ,
       "By default number of reads and writes operations are configured to "+AppBase.appConfig.numKeysToRead+" and "+AppBase.appConfig.numKeysToWrite+" respectively." ,
       " User can run read/write(both) operations indefinitely by passing -1 to --num_reads or --num_writes or both");
  }
}
