/*
 * Copyright 2014 CyberVision, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef KAA_DISABLE_FEATURE_LOGGING
#include "kaa_logging.h"

#include <stddef.h>
#include <string.h>
#include "collections/kaa_list.h"
#include "utilities/kaa_mem.h"
#include "utilities/kaa_uuid.h"
#include "utilities/kaa_log.h"
#include "kaa_common.h"
#include "kaa_status.h"
#include "kaa_channel_manager.h"
#include "kaa_platform_utils.h"
#include "kaa_platform_common.h"

#include "avro_src/avro/io.h"

#define KAA_LOGGING_RECEIVE_UPDATES_FLAG   0x01
#define KAA_MAX_PADDING_LENGTH             3

typedef enum {
    LOGGING_RESULT_SUCCESS = 0x00,
    LOGGING_RESULT_FAILURE = 0x01
} logging_sync_result_t;

extern kaa_sync_handler_fn kaa_channel_manager_get_sync_handler(kaa_channel_manager_t *self, kaa_service_t service_type);

static const kaa_service_t logging_sync_services[1] = {KAA_SERVICE_LOGGING};
struct kaa_log_collector {
    uint16_t                        log_bucket_id;
    kaa_log_storage_t           *   log_storage;
    kaa_log_upload_properties_t *   log_properties;
    kaa_storage_status_t        *   log_storage_status;
    log_upload_decision_fn          is_upload_needed_fn;
    kaa_status_t                *   status;
    kaa_channel_manager_t       *   channel_manager;
    kaa_logger_t                *   logger;
};

kaa_error_t kaa_log_collector_create(kaa_log_collector_t ** log_collector_p, kaa_status_t *status, kaa_channel_manager_t *channel_manager, kaa_logger_t *logger)
{
    KAA_RETURN_IF_NIL(log_collector_p, KAA_ERR_BADPARAM);
    kaa_log_collector_t * collector = (kaa_log_collector_t *) KAA_MALLOC(sizeof(kaa_log_collector_t));
    KAA_RETURN_IF_NIL(collector, KAA_ERR_NOMEM);

    collector->log_bucket_id        = 0;
    collector->log_storage          = NULL;
    collector->log_storage_status   = NULL;
    collector->log_properties       = NULL;
    collector->status               = status;
    collector->channel_manager      = channel_manager;
    collector->logger               = logger;

    *log_collector_p = collector;
    return KAA_ERR_NONE;
}

void kaa_log_collector_destroy(kaa_log_collector_t *self)
{
    if (self) {
        if (self->log_storage)
            (*self->log_storage->destroy)();
        KAA_FREE(self);
    }
}

kaa_error_t kaa_logging_init(
                              kaa_log_collector_t *collector
                            , kaa_log_storage_t * storage
                            , kaa_log_upload_properties_t *properties
                            , kaa_storage_status_t * status
                            , log_upload_decision_fn need_upl
                           )
{
    KAA_RETURN_IF_NIL(collector, KAA_ERR_BADPARAM);
    KAA_RETURN_IF_NIL4(storage, status, need_upl, properties, KAA_ERR_BADPARAM);

    if (collector->log_storage)
        (*collector->log_storage->destroy)();

    collector->log_storage = storage;
    collector->log_properties = properties;
    collector->log_storage_status = status;
    collector->is_upload_needed_fn = need_upl;

    KAA_LOG_INFO(collector->logger, KAA_ERR_NONE, "Initialized log collector with: "
                "log storage {%p}, log properties {%p}, log storage status {%p}, is uploaded needed func {%p}"
            , storage, properties, status, need_upl);

    return KAA_ERR_NONE;
}

static void update_storage(kaa_log_collector_t *self)
{
    kaa_log_upload_decision_t decision = (*self->is_upload_needed_fn)(self->log_storage_status);
    switch (decision) {
        case CLEANUP:
            KAA_LOG_WARN(self->logger, KAA_ERR_NONE, "Need to cleanup log storage. Current size: %zu, Maximal volume: %zu"
                    , (*self->log_storage_status->get_total_size)()
                    , self->log_properties->max_log_storage_volume
                    );
            (*self->log_storage->shrink_to_size)(self->log_properties->max_log_storage_volume);
            break;
        case UPLOAD: {
            KAA_LOG_INFO(self->logger, KAA_ERR_NONE, "Initiating log upload...");
            kaa_sync_handler_fn sync = kaa_channel_manager_get_sync_handler(self->channel_manager, logging_sync_services[0]);
            if (sync)
                (*sync)(logging_sync_services, 1);
            break;
        }
        default:
            KAA_LOG_TRACE(self->logger, KAA_ERR_NONE, "Upload shall not be triggered now.");
            break;
     }
}

kaa_error_t kaa_logging_add_record(kaa_log_collector_t *self, kaa_user_log_record_t *entry)
{
    KAA_RETURN_IF_NIL2(self, entry, KAA_ERR_BADPARAM);

    KAA_LOG_DEBUG(self->logger, KAA_ERR_NONE, "Adding new log record {%p}", entry);

    if (self->log_storage && self->is_upload_needed_fn && self->log_storage_status) {
        kaa_log_entry_t record = { NULL, 0 };

        record.record_size = entry->get_size(entry);

        KAA_LOG_TRACE(self->logger, KAA_ERR_NONE, "Record size is %d", record.record_size);

        if (record.record_size > 0) {
            record.record_data = (uint8_t *) KAA_MALLOC(record.record_size * sizeof(uint8_t));
            if (!record.record_data)
                return KAA_ERR_NOMEM;

            avro_writer_t writer = avro_writer_memory((char *)record.record_data, record.record_size);
            if (!writer) {
                KAA_FREE(record.record_data);
                return KAA_ERR_NOMEM;
            }

            entry->serialize(writer, entry);
            avro_writer_free(writer);

            KAA_LOG_TRACE(self->logger, KAA_ERR_NONE, "Adding serialized record to log storage {%p}", self->log_storage);
            (*self->log_storage->add_log_record)(record);
            update_storage(self);
            return KAA_ERR_NONE;
        }
        return KAA_ERR_BADPARAM;

    }
    return KAA_ERR_BAD_STATE;
}

kaa_error_t kaa_profile_request_get_size(kaa_log_collector_t *self, size_t *expected_size)
{
    KAA_RETURN_IF_NIL2(self, expected_size, KAA_ERR_BADPARAM);
    KAA_RETURN_IF_NIL2(self->log_storage_status, self->log_properties, KAA_ERR_NOT_INITIALIZED);
    KAA_RETURN_IF_NIL2(self->log_storage_status->get_records_count, self->log_storage_status->get_total_size, KAA_ERR_BAD_STATE);

    *expected_size = KAA_EXTENSION_HEADER_SIZE;
    *expected_size += sizeof(uint32_t); // request id + log records count

    uint16_t records_count = self->log_storage_status->get_records_count();
    size_t total_size = self->log_storage_status->get_total_size();

    size_t actual_size = records_count * sizeof(uint32_t) + records_count * KAA_MAX_PADDING_LENGTH + total_size;
    *expected_size += ((actual_size < self->log_properties->max_log_block_size) ? actual_size : self->log_properties->max_log_block_size);

    return KAA_ERR_NONE;
}

kaa_error_t kaa_logging_request_serialize(kaa_log_collector_t *self, kaa_platform_message_writer_t *writer)
{
    KAA_RETURN_IF_NIL2(self, writer, KAA_ERR_BADPARAM);
    KAA_RETURN_IF_NIL3(self->log_storage, self->log_storage->get_record, self->log_properties, KAA_ERR_BAD_STATE);

    KAA_LOG_TRACE(self->logger, KAA_ERR_NONE, "Going to compile log request");

    uint32_t total_size = sizeof(uint32_t);
    char *extension_header_size_p = writer->current + sizeof(uint32_t); // pointer for the extension size. Will be filled later.
    if (kaa_platform_message_extension_header_write(writer, KAA_LOGGING_EXTENSION_TYPE, KAA_LOGGING_RECEIVE_UPDATES_FLAG, 0))
        return KAA_ERR_WRITE_FAILED;

    if (!self->log_bucket_id && kaa_status_get_log_bucket_id(self->status, &self->log_bucket_id))
        return KAA_ERR_BAD_STATE;
    ++self->log_bucket_id;

    *((uint16_t *) writer->current) = KAA_HTONS(self->log_bucket_id);
    writer->current += sizeof(uint16_t);
    char *records_count_p = writer->current; // pointer for the records count. Will be filled later.
    writer->current += sizeof(uint16_t);

    ssize_t remaining_size = self->log_properties->max_log_block_size;
    KAA_LOG_TRACE(self->logger, KAA_ERR_NONE, "Extracting log records... (Block size is %zu)", remaining_size);

    uint16_t records_count = 0;
    kaa_log_entry_t entry = self->log_storage->get_record(self->log_bucket_id, remaining_size);
    for (; entry.record_data; entry = self->log_storage->get_record(self->log_bucket_id, remaining_size)) {
        KAA_LOG_TRACE(self->logger, KAA_ERR_NONE, "Got record {%p}, size: %zu", entry.record_data, entry.record_size);
        ++records_count;
        remaining_size -= (kaa_aligned_size_get(entry.record_size) + sizeof(uint32_t));
        if (kaa_platform_message_write(writer, &entry.record_size, sizeof(uint32_t))) {
            if (self->log_storage->upload_failed)
                self->log_storage->upload_failed(self->log_bucket_id);
            return KAA_ERR_WRITE_FAILED;
        }
        if (kaa_platform_message_write_aligned(writer, entry.record_data, entry.record_size)) {
            if (self->log_storage->upload_failed)
                self->log_storage->upload_failed(self->log_bucket_id);
            return KAA_ERR_WRITE_FAILED;
        }
    }
    total_size += (self->log_properties->max_log_block_size - remaining_size);
    KAA_LOG_TRACE(self->logger, KAA_ERR_NONE, "Extracted log records. Total records count = %u. Total extension size = %lu", records_count, total_size);

    *((uint32_t *) extension_header_size_p) = KAA_HTONL(total_size);
    *((uint16_t *) records_count_p) = KAA_HTONS(records_count);

    return KAA_ERR_NONE;
}

kaa_error_t kaa_logging_handle_server_sync(kaa_log_collector_t *self, kaa_platform_message_reader_t *reader, uint32_t extension_options, size_t extension_length)
{
    KAA_RETURN_IF_NIL2(self, reader, KAA_ERR_BADPARAM);
    KAA_RETURN_IF_NIL(self->log_storage, KAA_ERR_NOT_INITIALIZED);

    KAA_LOG_INFO(self->logger, KAA_ERR_NONE, "Received log sync response. Log storage is {%p}", self->log_storage);

    uint16_t id = KAA_NTOHS(*((uint16_t *) reader->current));
    reader->current += sizeof(uint16_t);
    logging_sync_result_t result = *((const uint8_t *) reader->current);
    reader->current += sizeof(uint16_t);

    KAA_LOG_DEBUG(self->logger, KAA_ERR_NONE, "Log block with id %u : %s"
            , id
            , (result == LOGGING_RESULT_SUCCESS ? "uploaded succesfully." : "upload failed.")
        );

    if (result == LOGGING_RESULT_SUCCESS) {
        if (self->log_storage->upload_succeeded)
            (*self->log_storage->upload_succeeded)(id);
    } else if (self->log_storage->upload_failed) {
        (*self->log_storage->upload_failed)(id);
    }
    update_storage(self);
    return KAA_ERR_NONE;

}

#endif

