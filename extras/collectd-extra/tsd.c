/*
 * collectd - src/tsd.c
 *
 * Copyright 2014 Groupon.com
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
 *
 * Authors:
 *   Ville Koskela (vkoskela at groupon dot com)
 */

#include "collectd.h"
#include "common.h"
#include "plugin.h"
#include "utils_cache.h"

#include <dirent.h>
#include <errno.h>
#include <inttypes.h>
#include <pthread.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>
#include <stdio.h>
#include <sys/param.h>
#include <time.h>

#ifdef __FAST_MATH__
#error "Not compatible with fast math optimization"
#endif

#ifndef _D_EXACT_NAMLEN
#define _D_EXACT_NAMLEN(dirent) dirent->d_namlen
#endif

// Buffer
typedef struct
{
  char* data;      // Pointer to allocated memory
  size_t length;   // The number of bytes of data excluding the terminator
  size_t capacity; // The number of bytes allocated including the terminator
} Buffer;

// Constants
static const char* config_keys[] =
{
  "DataDir",
  "Version",
  "FileBufferSize",
  "FilesToRetain",
  "StoreRates"
};
static const int config_keys_num = STATIC_ARRAY_SIZE(config_keys);

static const char* valid_versions[] =
{
    "2c"
    //"2d", // Work in progress (MAI-48)
    //"2e"  // Work in progress (MAI-49)
};
static const int version_2c = 0;
static const int version_2d = 1;
static const int version_2e = 2;
static const int valid_versions_num = STATIC_ARRAY_SIZE(valid_versions);

// Configuration
static char *data_dir = NULL;
static int use_stdio = 0;
static int version = 0;
static size_t file_buffer_size = BUFSIZ;
static int files_to_retain = 24;
static bool store_rates = false;

// State
static FILE* file = NULL;
static char file_name[512];
static char* file_buffer = NULL;
static unsigned long file_timestamp = 0;
static pthread_mutex_t lock;

static unsigned long tsd_create_timestamp(const time_t* time)
{
  const struct tm* time_info = gmtime(time);
  return (time_info->tm_year + 1900) * 1000000
      + (time_info->tm_mon + 1) * 10000
      + time_info->tm_mday * 100
      + time_info->tm_hour;
}

static time_t tsd_from_timestamp(const unsigned long timestamp)
{
  // NOTE: tm_day and tm_yday are ignored by mktime
  struct tm time_info;
  time_info.tm_year = timestamp / 1000000 - 1900;
  time_info.tm_mon = ((timestamp / 10000) % 100) - 1;
  time_info.tm_mday= (timestamp / 100) % 100;
  time_info.tm_hour = timestamp % 100;
  time_info.tm_min = 0;
  time_info.tm_sec = 0;
  time_info.tm_isdst = 0;
  return mktime(&time_info);
}

static bool is_rate_enabled(const data_set_t* data_set, const value_list_t* value_list)
{
  // TODO: Enable rate configuration per metric prefix. (MAI-198)
  // TODO: Enable logging of both rate and non-rate metric. (MAI-199)
  return store_rates;
}

static void tsd_format_file_name(char* buffer, const size_t buffer_size, const unsigned long* timestamp)
{
  if (timestamp == NULL)
  {
    snprintf(
        buffer,
        buffer_size,
        "%scollectd-query.log",
        data_dir);
  }
  else
  {
    snprintf(
        buffer,
        buffer_size,
        "%scollectd-query.%lu.log",
        data_dir,
        *timestamp);
  }
}

static void tsd_buffer_initialize(Buffer* buffer, const size_t capacity)
{
  buffer->data = malloc(capacity);
  if (buffer->data != NULL)
  {
    buffer->data[0] = '\0';
    buffer->capacity = capacity;
  }
  else
  {
    buffer->capacity = 0;
  }
  buffer->length = 0;
}

static void tsd_buffer_free(Buffer* buffer)
{
  free(buffer->data);
  buffer->data = NULL;
}

static void tsd_buffer_accommodate(Buffer* buffer, const size_t additional_capacity)
{
  const size_t required_capacity = buffer->length + additional_capacity;
  if (required_capacity > buffer->capacity)
  {
    const size_t new_capacity = MAX(2 * (buffer->capacity), required_capacity);
    buffer->data = realloc(buffer->data, new_capacity);
    if (buffer->data != NULL)
    {
      buffer->capacity = new_capacity;
    }
    else
    {
      ERROR("tsd plugin: memory re-allocation failure");
      buffer->capacity = 0;
      buffer->length = 0;
    }
  }
}

static void tsd_buffer_append(Buffer* buffer, const char* str)
{
  unsigned int i = 0;
  for (; str[i] != '\0'; ++i)
  {
    if (buffer->capacity == buffer->length)
    {
      tsd_buffer_accommodate(buffer, 1);
      if (buffer->data == NULL) return;
    }
    buffer->data[buffer->length] = str[i];
    buffer->length += 1;
  }
  if (buffer->capacity == buffer->length)
  {
    tsd_buffer_accommodate(buffer, 1);
    if (buffer->data == NULL) return;
  }
  buffer->data[buffer->length] = '\0';
}

static void tsd_buffer_sprintf(Buffer* buffer, const char* format, ...)
{
  va_list args;
  va_start(args, format);
  const size_t available_capacity = buffer->capacity - buffer->length;
  const size_t str_length = vsnprintf(
      buffer->data + buffer->length,
      available_capacity,
      format,
      args);
  if (str_length >= available_capacity)
  {
    tsd_buffer_accommodate(buffer, str_length + 1);
    if (buffer->data == NULL) return;
    vsnprintf(
        buffer->data + buffer->length,
        str_length + 1,
        format,
        args);
  }
  buffer->length += str_length;
  va_end(args);
}

static void tsd_buffer_append_name(Buffer* buffer, const value_list_t *value_list, const data_set_t *data_set, const unsigned int index)
{
  // Extract the state and data
  bool include_plugin_instance = (value_list->plugin_instance != NULL) && (value_list->plugin_instance[0] != 0);
  bool include_type_instance = (value_list->type_instance != NULL) && (value_list->type_instance[0] != 0);
  bool include_data_source_name = (data_set->ds != NULL) && (data_set->ds->name != NULL) && (data_set->ds->name[0] != 0);

  const char* plugin = value_list->plugin;
  const char* plugin_instance = value_list->plugin_instance;
  const char* type = value_list->type;
  const char* type_instance = value_list->type_instance;
  const char* data_set_name = data_set->ds->name;

  // Apply special conversion rules
  if (strcmp(plugin, "interface") == 0
      && include_plugin_instance
      && strncmp(type, "if_", 3) == 0
      && include_data_source_name
      && strcmp(data_set_name, "rx") == 0
      && index == 1)
  {
    data_set_name = "tx";
  }
  if (strcmp(plugin, "load") == 0
        && strcmp(type, "load") == 0)
  {
    if (index == 0)
    {
      include_data_source_name = true;
      data_set_name = "1min";
    }
    else if (index == 1)
    {
      include_data_source_name = true;
      data_set_name = "5min";
    }
    else if (index == 2)
    {
      include_data_source_name = true;
      data_set_name = "15min";
    }
  }
  if (strcmp(plugin, "disk") == 0
      && include_plugin_instance
      && strncmp(type, "disk_", 5) == 0
      && include_data_source_name
      && strcmp(data_set_name, "read") == 0
      && index == 1)
  {
    data_set_name = "write";
  }
  if (strcmp(plugin, "processes") == 0
      && include_plugin_instance
      && strcmp(type, "ps_disk_ops") == 0
      && include_data_source_name
      && strcmp(data_set_name, "read") == 0
      && index == 1)
  {
    data_set_name = "write";
  }
  if (strcmp(plugin, "processes") == 0
      && include_plugin_instance
      && strcmp(type, "ps_cputime") == 0
      && include_data_source_name
      && strcmp(data_set_name, "user") == 0
      && index == 1)
  {
    data_set_name = "system";
  }
  if (strcmp(plugin, "processes") == 0
      && include_plugin_instance
      && strcmp(type, "ps_count") == 0
      && include_data_source_name
      && strcmp(data_set_name, "processes") == 0
      && index == 1)
  {
    data_set_name = "lwp";
  }
  if (strcmp(plugin, "processes") == 0
        && include_plugin_instance
        && strcmp(type, "ps_pagefaults") == 0
        && include_data_source_name
        && strcmp(data_set_name, "minflt")
        && index == 1)
  {
    data_set_name = "majflt";
  }
  if (strcmp(plugin, "processes") == 0
        && include_plugin_instance
        && strcmp(type, "ps_disk_octets") == 0
        && include_data_source_name
        && strcmp(data_set_name, "read")
        && index == 1)
    {
      data_set_name = "write";
    }

  // Render the name
  tsd_buffer_append(buffer, plugin);
  tsd_buffer_append(buffer, include_plugin_instance == true ? "/" : "");
  tsd_buffer_append(buffer, include_plugin_instance == true ? plugin_instance : "");
  tsd_buffer_append(buffer, "/");
  tsd_buffer_append(buffer, type);
  tsd_buffer_append(buffer, include_type_instance == true ? "/" : "");
  tsd_buffer_append(buffer, include_type_instance == true ? type_instance : "");
  tsd_buffer_append(buffer, include_data_source_name == true ? "/" : "");
  tsd_buffer_append(buffer, include_data_source_name == true ? data_set_name : "");
}

static void tsd_buffer_append_value(Buffer* buffer, const value_list_t *value_list, const data_set_t *data_set, const unsigned int index, gauge_t* rates)
{
  // Extract the state and data
  bool include_plugin_instance = (value_list->plugin_instance != NULL) && (value_list->plugin_instance[0] != 0);
  // Uncomment when needed for value modification:
  //bool include_type_instance = (value_list->type_instance != NULL) && (value_list->type_instance[0] != 0);
  bool include_data_source_name = (data_set->ds != NULL) && (data_set->ds->name != NULL) && (data_set->ds->name[0] != 0);

  const char* plugin = value_list->plugin;
  // Uncomment when needed for value modification:
  //const char* plugin_instance = value_list->plugin_instance;
  const char* type = value_list->type;
  // Uncomment when needed for value modification:
  //const char* type_instance = value_list->type_instance;
  const char* data_set_name = data_set->ds->name;

  value_t value = value_list->values[index];
  int value_type = data_set->ds[index].type;
  bool use_rate = is_rate_enabled(data_set, value_list);

  if (strcmp(plugin, "processes") == 0
        && include_plugin_instance
        && strcmp(type, "ps_cputime") == 0
        && include_data_source_name
        && strcmp(data_set_name, "user") == 0)
  {

    if (value_type == DS_TYPE_DERIVE)
    {
      // Process cpu time is reported in micro-seconds; convert it to
      // a percentage of the reporting time interval from 0 to 100.
      value.gauge = rates[index] / 10000.0;
      value_type = DS_TYPE_GAUGE;
    }
  }

  bool is_nan = false;
  const char* value_type_name = "unknown";
  if (value_type == DS_TYPE_GAUGE)
  {
    // Requires fast math optimization is disabled
    is_nan = !(value.gauge == value.gauge);
    value_type_name = "gauge";
    if (!is_nan)
    {
      tsd_buffer_sprintf(buffer, "%lf", value.gauge);
    }
  }
  else
  {
    // Output rates or bare value
    if (use_rate && rates == NULL)
    {
      WARNING("tsd plugin: uc_get_rate failed");
    }
    else if (use_rate)
    {
      // Requires fast math optimization is disabled
      is_nan = !(rates[index] == rates[index]);
      value_type_name = "rate";
      if (!is_nan)
      {
        tsd_buffer_sprintf(buffer, "%lf", rates[index]);
      }
    }
    // TODO: Record the counters as counters instead of as gauges (MAI-50)
    else if (value_type == DS_TYPE_COUNTER)
    {
      // Requires fast math optimization is disabled
      is_nan = !(value.counter == value.counter);
      value_type_name = "counter";
      if (!is_nan)
      {
        tsd_buffer_sprintf(buffer, "%llu", value.counter);
      }
    }
    else if (value_type == DS_TYPE_DERIVE)
    {
      // Requires fast math optimization is disabled
      is_nan = !(value.derive == value.derive);
      value_type_name = "derived";
      if (!is_nan)
      {
        tsd_buffer_sprintf(buffer, "%"PRIi64, value.derive);
      }
    }
    else if (value_type == DS_TYPE_ABSOLUTE)
    {
      // Requires fast math optimization is disabled
      is_nan = !(value.absolute == value.absolute);
      value_type_name = "absolute";
      if (!is_nan)
      {
        tsd_buffer_sprintf(buffer, "%"PRIi64, value.absolute);
      }
    }
  }

  // TODO: Add suppression of warning for the first time each rate metric is
  // encountered since a rate cannot be computed for a single data point.
  // (MAI-200)
  if (is_nan)
  {
    Buffer name_buffer;
    tsd_buffer_initialize(&name_buffer, 256);
    tsd_buffer_append_name(&name_buffer, value_list, data_set, index);
    WARNING("tsd plugin: suppressed %s sample as it is not a number: %s", value_type_name, name_buffer.data);
    tsd_buffer_free(&name_buffer);
  }
}

static void tsd_format_2e(Buffer* buffer, const data_set_t *data_set, const value_list_t *value_list)
{
  // TODO: Implement me. (MAI-48)
}

static void tsd_format_2d(Buffer* buffer, const data_set_t *data_set, const value_list_t *value_list)
{
  // TODO: Implement me. (MAI-49)
}

static void tsd_format_2c(Buffer* buffer, const data_set_t *data_set, const value_list_t *value_list)
{
  // Open JSON and record version
  tsd_buffer_sprintf(buffer, "{\"version\":\"%s\",", valid_versions[version_2c]);

  // Record initial and final timestamp annotations
  tsd_buffer_sprintf(
      buffer,
      "\"annotations\":{\"finalTimestamp\":\"%5.3f\",\"initTimestamp\":\"%5.3f\"},",
      CDTIME_T_TO_MS(value_list->time) / 1000.0,
      CDTIME_T_TO_MS(value_list->time - value_list->interval) / 1000.0);

  // Record everything as a gauge
  tsd_buffer_append(buffer, "\"gauges\":{");
  unsigned int i = 0;   // Declaration inside for loop not supported pre-C99
  gauge_t* rates = uc_get_rate(data_set, value_list);
  for (; i < data_set->ds_num; ++i)
  {
    if (i > 0)
    {
      tsd_buffer_append(buffer, ",");
    }
    tsd_buffer_append(buffer, "\"");
    tsd_buffer_append_name(buffer, value_list, data_set, i);
    tsd_buffer_append(buffer, "\":[");
    tsd_buffer_append_value(buffer, value_list, data_set, i, rates);
    tsd_buffer_append(buffer, "]");
  }
  sfree(rates);

  // Record empty timers and counters then close the JSON
  tsd_buffer_append(buffer, "},\"timers\":{},\"counters\":{}}\n");
}

static FILE* tsd_get_file() {
  // Check if the file needs to be rotated
  time_t current_time;
  time(&current_time);
  const unsigned long current_timestamp = tsd_create_timestamp(&current_time);
  if (use_stdio == 0 && file != NULL && file_timestamp != current_timestamp)
  {
    // Close the current file
    fclose(file);
    file = NULL;

    // Archive the current file
    char archive_name[512];
    tsd_format_file_name(archive_name, sizeof(archive_name), &file_timestamp);
    if (rename(file_name, archive_name) != 0)
    {
      char error_buffer[1024];
      ERROR(
          "tsd plugin: rename (%s, %s) failed: %s",
          file_name,
          archive_name,
          sstrerror(
              errno,
              error_buffer,
              sizeof(error_buffer)));
      return NULL;
    }

    // Check for the n+1-th oldest file and remove if it exists
    // NOTE: On start-up the daemon removes any files in excess of N
    const time_t remove_time = current_time - 3600 * (files_to_retain + 1);
    const unsigned long remove_timestamp = tsd_create_timestamp(&remove_time);
    char remove_name[512];
    tsd_format_file_name(remove_name, sizeof(remove_name), &remove_timestamp);
    INFO(
        "tsd plugin: removing file: %s",
        remove_name);
    if (remove(remove_name) != 0)
    {
      char error_buffer[1024];
      WARNING(
          "tsd plugin: remove (%s) failed: %s",
          remove_name,
          sstrerror(
              errno,
              error_buffer,
              sizeof(error_buffer)));
    }
  }

  if (file == NULL) {
    if (use_stdio == 1)
    {
      file = stdout;
    }
    else if (use_stdio == 2)
    {
      file = stderr;
    }
    else
    {
      // Create the full file path and name
      tsd_format_file_name(file_name, sizeof(file_name), NULL);

      // Open the file
      file = fopen(file_name, "a");
      if (file == NULL)
      {
        char error_buffer[1024];
        ERROR(
            "tsd plugin: fopen (%s) failed: %s",
            file_name,
            sstrerror(
                errno,
                error_buffer,
                sizeof(error_buffer)));
        return NULL;
      }

      // Set the file line buffer
      setvbuf(file, file_buffer, _IOLBF, file_buffer_size);

      // Set the timestamp
      file_timestamp = current_timestamp;
    }
  }
  return file;
}

static int tsd_write(const data_set_t *data_set, const value_list_t *value_list, user_data_t __attribute__((unused)) *user_data)
{
  // Create a null terminated buffer
  volatile static int max_buffer_size = 1024;
  Buffer buffer;
  tsd_buffer_initialize(&buffer, max_buffer_size);
  if (buffer.data == NULL)
  {
    ERROR("tsd plugin: memory allocation failure");
    return -1;
  }

  // Format the records
  if (version == version_2e)
  {
    tsd_format_2e(&buffer, data_set, value_list);
  }
  else if (version == version_2d)
  {
    tsd_format_2d(&buffer, data_set, value_list);
  }
  else if (version == version_2c)
  {
    tsd_format_2c(&buffer, data_set, value_list);
  }
  else
  {
    ERROR("tsd plugin: invalid version: %i", version);
    tsd_buffer_free(&buffer);
    return -1;
  }

  // CollectD documentation specifies that plugins must be thread safe
  pthread_mutex_lock(&lock);
  if (buffer.capacity > max_buffer_size)
  {
    max_buffer_size = buffer.capacity;
  }
  FILE* tsd = tsd_get_file();
  if (tsd == NULL)
  {
    ERROR("tsd plugin: invalid file handle");
    pthread_mutex_unlock(&lock);
    tsd_buffer_free(&buffer);
    return -1;
  }
  if (fputs(buffer.data, tsd) < 0)
  {
    ERROR("tsd plugin: error writing to file");
    pthread_mutex_unlock(&lock);
    tsd_buffer_free(&buffer);
    return -1;
  }
  pthread_mutex_unlock(&lock);
  tsd_buffer_free(&buffer);

  return 0;
}

static int tsd_shutdown()
{
  if (file != NULL)
  {
    if (fclose(file) != 0)
    {
      char error_buffer[1024];
      ERROR(
          "tsd plugin: fclose failed: %s",
          sstrerror(
              errno,
              error_buffer,
              sizeof(error_buffer)));
      return -1;
    }
    file = NULL;
  }
  if (file_buffer != NULL)
  {
    free(file_buffer);
    file_buffer = NULL;
  }
  return 0;
}

static int tsd_config(const char *key, const char *value)
{
  if (strcasecmp("DataDir", key) == 0)
  {
    if (data_dir != NULL)
    {
      free(data_dir);
      data_dir = NULL;
    }
    if (strcasecmp("stdout", value) == 0)
    {
      use_stdio = 1;
      return 0;
    }
    else if (strcasecmp("stderr", value) == 0)
    {
      use_stdio = 2;
      return 0;
    }
    const int value_len = strlen(value);
    const int data_dir_size = value_len + 1 + (value[value_len - 1] == '/' ? 0 : 1);
    data_dir = malloc(data_dir_size);
    if (data_dir == NULL)
    {
      ERROR("tsd plugin: memory allocation failure");
      return -1;
    }
    if (value[value_len - 1] == '/')
    {
      snprintf(data_dir, data_dir_size, "%s", value);
    }
    else
    {
      snprintf(data_dir, data_dir_size, "%s/", value);
    }
  }
  else if (strcasecmp("Version", key) == 0)
  {
    unsigned int version_index = 0; // Declaration inside for loop not supported pre-C99
    for (; version_index < valid_versions_num; ++version_index)
    {
      if (strcasecmp(valid_versions[version_index], value) == 0)
      {
        break;
      }
    }
    if (version_index >= valid_versions_num)
    {
      ERROR(
          "tsd plugin: unsupported version: %s",
          value);
      return -1;
    }
    version = version_index;
  }
  else if (strcasecmp("FileBufferSize", key) == 0)
  {
    file_buffer_size = strtol(value, NULL, 10);
    if (errno == ERANGE)
    {
      ERROR(
          "tsd plugin: invalid file buffer size: %s",
          value);
      return -1;
    }
    if (file_buffer != NULL)
    {
      free(file_buffer);
      file_buffer = NULL;
    }
    if (file_buffer_size > 0)
    {
      file_buffer = malloc(file_buffer_size);
      if (file_buffer == NULL)
      {
        ERROR("tsd plugin: memory allocation failure");
        return -1;
      }
    }
  }
  else if (strcasecmp("FilesToRetain", key) == 0)
  {
    files_to_retain = strtol(value, NULL, 10);
    if (errno == ERANGE)
    {
      ERROR(
          "tsd plugin: invalid files to retain: %s",
          value);
      return -1;
    }
  }
  else if (strcasecmp("StoreRates", key) == 0)
  {
    if (IS_TRUE(value))
    {
      store_rates = true;
    }
    else
    {
      store_rates = false;
    }
  }
  return 0;
}

static int tsd_init()
{
  if (pthread_mutex_init(&lock, NULL) != 0)
  {
    ERROR("tsd plugin: pthread_mutex_init failed");
    return -1;
  }

  // Check for old archived files and remove them
  if (use_stdio == 0)
  {
    DIR *dir = opendir(data_dir);
    if (dir != NULL)
    {
      struct dirent *entry;
      while ((entry = readdir(dir)) != NULL)
      {
        DEBUG(
            "tsd plugin: considering file: %s",
            entry->d_name);

        unsigned long timestamp;
        if (sscanf(entry->d_name, "collectd-query.%lu.log", &timestamp) == 1)
        {
          // Compute the file age
          time_t current_time;
          time(&current_time);
          const time_t file_time = tsd_from_timestamp(timestamp);
          const int age = difftime(current_time, file_time) / 3600;

          DEBUG(
              "tsd plugin: candidate file timestamp: %lu is %i hours old",
              timestamp,
              age);

          // Remove files beyond the retention period
          if (age > files_to_retain)
          {
            char remove_name[512];
            tsd_format_file_name(remove_name, sizeof(remove_name), &timestamp);

            NOTICE(
                "tsd plugin: removing file: %s (%i hours old)",
                remove_name,
                age);

            if (remove(remove_name) != 0)
            {
              char error_buffer[1024];
              WARNING(
                  "tsd plugin: remove (%s) failed: %s",
                  remove_name,
                  sstrerror(
                      errno,
                      error_buffer,
                      sizeof(error_buffer)));
            }
          }
        }
      }
      closedir(dir);
    }
    else // dir == NULL
    {
      char error_buffer[1024];
      WARNING(
          "tsd plugin: readdir (%s) failed: %s",
          data_dir,
          sstrerror(
              errno,
              error_buffer,
              sizeof(error_buffer)));
    }
  }
  return 0;
}

void module_register()
{
  plugin_register_init("tsd", tsd_init);
  plugin_register_config("tsd", tsd_config, config_keys, config_keys_num);
  plugin_register_write("tsd", tsd_write, /* user_data = */ NULL);
  plugin_register_shutdown("tsd", tsd_shutdown);
}

