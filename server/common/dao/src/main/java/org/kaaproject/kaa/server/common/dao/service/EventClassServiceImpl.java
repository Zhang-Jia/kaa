/*
 * Copyright 2014-2016 CyberVision, Inc.
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

package org.kaaproject.kaa.server.common.dao.service;

import static org.kaaproject.kaa.server.common.dao.impl.DaoUtil.convertDtoList;
import static org.kaaproject.kaa.server.common.dao.impl.DaoUtil.getDto;
import static org.kaaproject.kaa.server.common.dao.service.Validator.isValidSqlId;
import static org.kaaproject.kaa.server.common.dao.service.Validator.isValidSqlObject;
import static org.kaaproject.kaa.server.common.dao.service.Validator.validateSqlId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.kaaproject.avro.ui.shared.NamesValidator;
import org.kaaproject.kaa.common.dto.event.AefMapInfoDto;
import org.kaaproject.kaa.common.dto.event.EventClassDto;
import org.kaaproject.kaa.common.dto.event.EventClassFamilyDto;
import org.kaaproject.kaa.common.dto.event.EventClassFamilyVersionDto;
import org.kaaproject.kaa.common.dto.event.EventClassType;
import org.kaaproject.kaa.server.common.dao.EventClassService;
import org.kaaproject.kaa.server.common.dao.exception.IncorrectParameterException;
import org.kaaproject.kaa.server.common.dao.impl.CTLSchemaDao;
import org.kaaproject.kaa.server.common.dao.impl.EventClassDao;
import org.kaaproject.kaa.server.common.dao.impl.EventClassFamilyDao;
import org.kaaproject.kaa.server.common.dao.model.sql.CTLSchema;
import org.kaaproject.kaa.server.common.dao.model.sql.EventClass;
import org.kaaproject.kaa.server.common.dao.model.sql.EventClassFamily;
import org.kaaproject.kaa.server.common.dao.model.sql.EventClassFamilyVersion;
import org.kaaproject.kaa.server.common.dao.schema.EventSchemaProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EventClassServiceImpl implements EventClassService {

    private static final Logger LOG = LoggerFactory.getLogger(EventClassServiceImpl.class);

    @Autowired
    private EventClassFamilyDao<EventClassFamily> eventClassFamilyDao;

    @Autowired
    private EventClassDao<EventClass> eventClassDao;

    @Autowired
    private EventSchemaProcessor eventSchemaProcessor;

    @Autowired
    private CTLSchemaDao<CTLSchema> ctlSchemaDao;

    @Override
    public List<EventClassFamilyDto> findEventClassFamiliesByTenantId(
            String tenantId) {
        List<EventClassFamilyDto> eventClassFamilies;
        if (isValidSqlId(tenantId)) {
            LOG.debug("Find event class families by tenant id [{}]", tenantId);
            eventClassFamilies = convertDtoList(eventClassFamilyDao.findByTenantId(tenantId));
        } else {
            throw new IncorrectParameterException("Incorrect tenant id: " + tenantId);
        }
        return eventClassFamilies;
    }

    @Override
    public EventClassFamilyDto findEventClassFamilyByTenantIdAndName(String tenantId, String name) {
        if (isValidSqlId(tenantId)) {
            LOG.debug("Find event class family by tenant id [{}] and name {}", tenantId, name);
            return eventClassFamilyDao.findByTenantIdAndName(tenantId, name).toDto();
        } else {
            throw new IncorrectParameterException("Incorrect tenant id: " + tenantId);
        }
    }

    @Override
    public EventClassFamilyDto findEventClassFamilyById(String id) {
        validateSqlId(id, "Event class family id is incorrect. Can't find event class family by id " + id);
        return getDto(eventClassFamilyDao.findById(id));
    }

    @Override
    public EventClassFamilyDto findEventClassFamilyByECFVersionId(String id) {
        validateSqlId(id, "Event class family version id is incorrect. Can't find event class family by ECF version id " + id);

        EventClassFamily eventClassFamily = eventClassFamilyDao.find().stream().
                filter(ecf -> ecf.getSchemas().stream()
                        .filter(ecfv -> ecfv.getStringId().equals(id)).count() > 0)
                .findFirst().get();
        return getDto(eventClassFamily);
    }

    @Override
    public List<EventClassFamilyVersionDto> findEventClassFamilyVersionsById(String id) {
        validateSqlId(id, "Event class family id is incorrect. Can't find event class family by id " + id);
        EventClassFamily ecf = eventClassFamilyDao.findById(id);
        return convertDtoList(ecf.getSchemas());
    }

    @Override
    public EventClassFamilyDto saveEventClassFamily(
            EventClassFamilyDto eventClassFamilyDto) {
        EventClassFamilyDto savedEventClassFamilyDto = null;
        if (isValidSqlObject(eventClassFamilyDto)) {
            if (eventClassFamilyDao.validateName(eventClassFamilyDto.getTenantId(), eventClassFamilyDto.getId(), eventClassFamilyDto.getName())) {
                if (StringUtils.isBlank(eventClassFamilyDto.getId())) {
                    if (NamesValidator.validateNamespace(eventClassFamilyDto.getNamespace())) {
                        if (NamesValidator.validateClassName(eventClassFamilyDto.getClassName())) {
                            if (eventClassFamilyDao.validateClassName(eventClassFamilyDto.getTenantId(), eventClassFamilyDto.getId(), eventClassFamilyDto.getClassName())) {
                                eventClassFamilyDto.setCreatedTime(System.currentTimeMillis());
                            } else {
                                LOG.debug("Can't save event class family. Class name should be unique within the tenant.");
                                throw new IncorrectParameterException("Incorrect event class family. Class name should be unique within the tenant.");
                            }
                        } else {
                            LOG.debug("Can't save event class family. Class name [{}] is not valid.", eventClassFamilyDto.getClassName());
                            throw new IncorrectParameterException("Incorrect event class family. Class name is not valid. '" + eventClassFamilyDto.getClassName() + "' is not a valid identifier.");
                        }
                    } else {
                        LOG.debug("Can't save event class family. Namespace [{}] is not valid.", eventClassFamilyDto.getNamespace());
                        throw new IncorrectParameterException("Incorrect event class family. Namespace is not valid. '" + eventClassFamilyDto.getNamespace() + "' is not a valid identifier.");
                    }
                }
                savedEventClassFamilyDto = getDto(eventClassFamilyDao.save(new EventClassFamily(eventClassFamilyDto)));
            } else {
                LOG.debug("Can't save event class family. Name should be unique within the tenant.");
                throw new IncorrectParameterException("Incorrect event class family. Name should be unique within the tenant.");
            }
        }
        return savedEventClassFamilyDto;
    }

    @Override
    public void addEventClassFamilyVersion(String eventClassFamilyId,
                                           EventClassFamilyVersionDto eventClassFamilyVersion, String createdUsername) {
        EventClassFamilyDto eventClassFamily = findEventClassFamilyById(eventClassFamilyId);
        if (eventClassFamily != null) {
            List<EventClassDto> records = eventClassFamilyVersion.getRecords();
            List<String> fqns = new ArrayList<>(records.size());
            for (EventClassDto eventClass : records) {
                fqns.add(eventClass.getFqn());
            }
            if (validateEventClassFamilyFqns(eventClassFamily, fqns)) {
                List<EventClassFamilyVersionDto> schemasDto = findEventClassFamilyVersionsById(eventClassFamilyId);
                int version = 1;
                if (schemasDto != null && !schemasDto.isEmpty()) {
                    Collections.sort(schemasDto, new Comparator<EventClassFamilyVersionDto>() {
                        @Override
                        public int compare(EventClassFamilyVersionDto o1,
                                           EventClassFamilyVersionDto o2) {
                            return o1.getVersion() - o2.getVersion();
                        }
                    });
                    version = schemasDto.get(schemasDto.size()-1).getVersion()+1;
                } else {
                    schemasDto = new ArrayList<>();
                }
                eventClassFamilyVersion.setVersion(version);
                eventClassFamilyVersion.setCreatedTime(System.currentTimeMillis());
                eventClassFamilyVersion.setCreatedUsername(createdUsername);
                schemasDto.add(eventClassFamilyVersion);
                EventClassFamily ecf = new EventClassFamily(eventClassFamily);
                List<EventClassFamilyVersion> schemas = new ArrayList<>();
                for (EventClassDto eventClass : records) {
                    setEventClassProperties(eventClassFamily, eventClass);
                }
                schemasDto.forEach(s -> schemas.add(new EventClassFamilyVersion(s)));
                setBackreference(schemas);
                ecf.setSchemas(schemas);
                eventClassFamilyDao.save(ecf);
            } else {
                LOG.debug("Can't process event class family schema.");
                throw new IncorrectParameterException("Incorrect event class family schema. FQNs should be unique within the tenant.");
            }
        } else {
            LOG.debug("Can't find related event class family.");
            throw new IncorrectParameterException("Event class family not found, id:" + eventClassFamilyId);
        }
    }

    private void setEventClassProperties(EventClassFamilyDto eventClassFamilyDto, EventClassDto eventClass) {
        eventClass.setTenantId(eventClassFamilyDto.getTenantId());
        eventClass.setVersion(ctlSchemaDao.findById(eventClass.getCtlSchemaId()).getVersion());
    }

    private void setBackreference(List<EventClassFamilyVersion> ecfvList) {
        ecfvList.forEach(ecfv -> ecfv.getRecords().forEach(ec -> ec.setEcfv(ecfv)));
    }

    private boolean validateEventClassFamilyFqns(EventClassFamilyDto eventClassFamily, List<String> fqns) {
        String ecfId = eventClassFamily.getId();
        EventClassFamily ecf = eventClassFamilyDao.findById(ecfId);
        List<String> storedFQNs = new ArrayList<>();
        ecf.getSchemas().forEach(ecfv -> ecfv.getRecords().forEach(ec -> storedFQNs.add(ec.getFqn())));

        for (String storedFQN : storedFQNs) {
            long duplicatedFqnCount = fqns.stream().filter(fqn -> fqn.equals(storedFQN)).count();
            if (duplicatedFqnCount > 0) return false;
        }

        return true;
    }

    @Override
    public List<EventClassDto> findEventClassesByFamilyIdVersionAndType(String ecfId, int version, EventClassType type) {
        List<EventClassDto> eventClasses;
        if (isValidSqlId(ecfId)) {
            LOG.debug("Find event classes by family id [{}] version [{}] and type [{}]", ecfId, version, type);
            EventClassFamily ecf = eventClassFamilyDao.findById(ecfId);
            EventClassFamilyVersion ecfv = ecf.getSchemas().stream().filter(s -> s.getVersion() == version).collect(Collectors.toList()).get(0);
            eventClasses = convertDtoList(ecfv.getRecords().stream().filter(ec -> type != null ? ec.getType() == type : true).collect(Collectors.toList()));
        } else {
            throw new IncorrectParameterException("Incorrect event class family id: " + ecfId);
        }
        return eventClasses;
    }

    @Override
    public List<EventClassDto> findEventClassByTenantIdAndFQN(String tenantId, String fqn) {
        if (isValidSqlId(tenantId)) {
            LOG.debug("Find event class family by tenant id [{}] and fqn {}", tenantId, fqn);
            return convertDtoList(eventClassDao.findByTenantIdAndFqn(tenantId, fqn));
        } else {
            throw new IncorrectParameterException("Incorrect tenant id: " + tenantId);
        }
    }

    @Override
    public EventClassDto findEventClassByTenantIdAndFQNAndVersion(String tenantId, String fqn, int version) {
        if (isValidSqlId(tenantId)) {
            LOG.debug("Find event class family by tenant id [{}] and fqn {}", tenantId, fqn);
            return getDto(eventClassDao.findByTenantIdAndFqnAndVersion(tenantId, fqn, version));
        } else {
            throw new IncorrectParameterException("Incorrect tenant id: " + tenantId);
        }

    }

    @Override
    public EventClassDto findEventClassById(String eventClassId) {
        if (isValidSqlId(eventClassId)) {
            LOG.debug("Find event class by id [{}] ", eventClassId);
            return getDto(eventClassDao.findById(eventClassId));
        } else {
            throw new IncorrectParameterException("Incorrect event class id: " + eventClassId);
        }
    }

    @Override
    public boolean isValidECFListInSdkProfile(List<AefMapInfoDto> ecfList) {
        List<EventClass> ecList = new ArrayList<>();
        for (AefMapInfoDto ecfMap : ecfList) {
            EventClassFamily ecf = eventClassFamilyDao.findById(ecfMap.getEcfId());
            ecf.getSchemas().forEach(ecfv -> ecList.addAll(ecfv.getRecords()));
        }

        for (EventClass ec : ecList) {
            long fqnMatches = ecList.stream().filter(ec2 -> ec2.getFqn().equals(ec.getFqn())).count();
            if (fqnMatches > 1) {
                return false;
            }
        }

        return true;
    }
}
