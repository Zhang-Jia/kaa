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

package org.kaaproject.kaa.server.operations.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.avro.generic.GenericRecord;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kaaproject.kaa.common.avro.GenericAvroConverter;
import org.kaaproject.kaa.common.dto.ApplicationDto;
import org.kaaproject.kaa.common.dto.EndpointGroupDto;
import org.kaaproject.kaa.common.dto.EndpointUserDto;
import org.kaaproject.kaa.common.dto.NotificationDto;
import org.kaaproject.kaa.common.dto.NotificationSchemaDto;
import org.kaaproject.kaa.common.dto.NotificationTypeDto;
import org.kaaproject.kaa.common.dto.ProfileFilterDto;
import org.kaaproject.kaa.common.dto.ProfileSchemaDto;
import org.kaaproject.kaa.common.dto.TopicDto;
import org.kaaproject.kaa.common.dto.TopicTypeDto;
import org.kaaproject.kaa.common.endpoint.gen.BasicEndpointProfile;
import org.kaaproject.kaa.common.endpoint.gen.ConfigurationSyncRequest;
import org.kaaproject.kaa.common.endpoint.gen.EndpointAttachRequest;
import org.kaaproject.kaa.common.endpoint.gen.EndpointDetachRequest;
import org.kaaproject.kaa.common.endpoint.gen.EndpointVersionInfo;
import org.kaaproject.kaa.common.endpoint.gen.EventListenersRequest;
import org.kaaproject.kaa.common.endpoint.gen.EventSyncRequest;
import org.kaaproject.kaa.common.endpoint.gen.NotificationSyncRequest;
import org.kaaproject.kaa.common.endpoint.gen.ProfileSyncRequest;
import org.kaaproject.kaa.common.endpoint.gen.SubscriptionCommand;
import org.kaaproject.kaa.common.endpoint.gen.SubscriptionCommandType;
import org.kaaproject.kaa.common.endpoint.gen.SyncRequest;
import org.kaaproject.kaa.common.endpoint.gen.SyncRequestMetaData;
import org.kaaproject.kaa.common.endpoint.gen.SyncResponse;
import org.kaaproject.kaa.common.endpoint.gen.SyncResponseResultType;
import org.kaaproject.kaa.common.endpoint.gen.SyncResponseStatus;
import org.kaaproject.kaa.common.endpoint.gen.UserAttachRequest;
import org.kaaproject.kaa.common.endpoint.gen.UserSyncRequest;
import org.kaaproject.kaa.common.endpoint.security.KeyUtil;
import org.kaaproject.kaa.common.hash.EndpointObjectHash;
import org.kaaproject.kaa.server.common.dao.ApplicationService;
import org.kaaproject.kaa.server.common.dao.ConfigurationService;
import org.kaaproject.kaa.server.common.dao.EndpointService;
import org.kaaproject.kaa.server.common.dao.NotificationService;
import org.kaaproject.kaa.server.common.dao.ProfileService;
import org.kaaproject.kaa.server.common.dao.TopicService;
import org.kaaproject.kaa.server.common.dao.exception.IncorrectParameterException;
import org.kaaproject.kaa.server.common.dao.impl.ApplicationDao;
import org.kaaproject.kaa.server.common.dao.impl.ConfigurationDao;
import org.kaaproject.kaa.server.common.dao.impl.ConfigurationSchemaDao;
import org.kaaproject.kaa.server.common.dao.impl.EndpointConfigurationDao;
import org.kaaproject.kaa.server.common.dao.impl.EndpointGroupDao;
import org.kaaproject.kaa.server.common.dao.impl.EndpointProfileDao;
import org.kaaproject.kaa.server.common.dao.impl.EndpointUserDao;
import org.kaaproject.kaa.server.common.dao.impl.ProfileFilterDao;
import org.kaaproject.kaa.server.common.dao.impl.ProfileSchemaDao;
import org.kaaproject.kaa.server.common.dao.impl.TenantDao;
import org.kaaproject.kaa.server.common.dao.impl.mongo.AbstractTest;
import org.kaaproject.kaa.server.common.dao.impl.mongo.MongoDBTestRunner;
import org.kaaproject.kaa.server.common.dao.model.mongo.MongoEndpointConfiguration;
import org.kaaproject.kaa.server.common.dao.model.mongo.MongoEndpointProfile;
import org.kaaproject.kaa.server.common.dao.model.mongo.MongoEndpointUser;
import org.kaaproject.kaa.server.common.dao.model.sql.Application;
import org.kaaproject.kaa.server.common.dao.model.sql.Configuration;
import org.kaaproject.kaa.server.common.dao.model.sql.ConfigurationSchema;
import org.kaaproject.kaa.server.common.dao.model.sql.EndpointGroup;
import org.kaaproject.kaa.server.common.dao.model.sql.ProfileFilter;
import org.kaaproject.kaa.server.common.dao.model.sql.ProfileSchema;
import org.kaaproject.kaa.server.common.dao.model.sql.Tenant;
import org.kaaproject.kaa.server.operations.pojo.Base64Util;
import org.kaaproject.kaa.server.operations.pojo.SyncResponseHolder;
import org.kaaproject.kaa.server.operations.pojo.exceptions.GetDeltaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/common-test-context.xml")
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Transactional
public class OperationsServiceIT extends AbstractTest {
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private static final Logger LOG = LoggerFactory.getLogger(OperationsServiceIT.class);

    private static final String ENDPOINT_ACCESS_TOKEN = "endpointAccessToken";
    private static final String INVALID_ENDPOINT_ACCESS_TOKEN = "InvalidEndpointAccessToken";
    private static final String REQUEST_ID1 = "request1";
    private static final String USER_EXTERNAL_ID = "user@test.com";
    private static final String USER_ACCESS_TOKEN = "userAccessToken";
    private static final String INVALID_USER_ACCESS_TOKEN = "invalidUserAccessToken";

    private static final int CONF_SCHEMA_VERSION = 2;
    private static final int PROFILE_SCHEMA_VERSION = 2;
    private static final int APPLICATION_SEQ_NUMBER = 9;

    private static final String CUSTOMER_ID = "CustomerId";
    private static final String APPLICATION_NAME = "ApplicationName";
    public static final String DATA_SCHEMA_LOCATION = "service/default_schema.json";
    public static final String BASE_SCHEMA_LOCATION = "service/default_schema_converted_to_base.json";
    public static final String BASE_DATA_LOCATION = "service/base_data.json";
    public static final String BASE_DATA_UPDATED_LOCATION = "service/base_data_updated.json";
    private static final BasicEndpointProfile ENDPOINT_PROFILE = new BasicEndpointProfile("dummy profile 1");
    private static final BasicEndpointProfile NEW_ENDPOINT_PROFILE = new BasicEndpointProfile("dummy profile 2");
    private static final BasicEndpointProfile FAKE_ENDPOINT_PROFILE = new BasicEndpointProfile("dummy profile 3");
    private static final byte[] ENDPOINT_KEY = "Endpoint Super Secret Public Key".getBytes(UTF_8);
    private static final byte[] ENDPOINT_KEY2 = "Endpoint Super Secret Public Key 2".getBytes(UTF_8);

    public static final String NEW_COMPLEX_CONFIG = "service/delta/complexFieldsDeltaNew.json";

    private String deltaSchema;
    private String currentConfiguration;
    private byte[] currentConfigurationHash;
    private ConfigurationSchema confSchema;
    private Application application;
    private Tenant customer;
    private ProfileSchema profileSchema;
    private ProfileFilterDto profileFilter;
    private TopicDto mandatoryTopicDto;
    private TopicDto voluntaryTopicDto;

    private final GenericAvroConverter<GenericRecord> avroConverter = new GenericAvroConverter<GenericRecord>(BasicEndpointProfile.SCHEMA$);

    @Autowired
    protected OperationsService operationsService;

    @Autowired
    protected ConfigurationService configurationService;

    @Autowired
    protected NotificationService notificationService;

    @Autowired
    protected TopicService topicService;

    @Autowired
    protected ApplicationService applicationService;

    @Autowired
    protected ProfileService profileService;

    @Autowired
    protected EndpointService endpointService;

    @Autowired
    protected EndpointUserDao<MongoEndpointUser> endpointUserDao;

    @Autowired
    protected TenantDao<Tenant> customerDao;

    @Autowired
    protected ApplicationDao<Application> applicationDao;

    @Autowired
    protected ConfigurationSchemaDao<ConfigurationSchema> configurationSchemaDao;

    @Autowired
    protected ConfigurationDao<Configuration> configurationDao;

    @Autowired
    protected EndpointConfigurationDao<MongoEndpointConfiguration> endpointConfigurationDao;

    @Autowired
    protected EndpointProfileDao<MongoEndpointProfile> endpointProfileDao;

    @Autowired
    protected ProfileSchemaDao<ProfileSchema> profileSchemaDao;

    @Autowired
    protected EndpointGroupDao<EndpointGroup> endpointGroupDao;

    @Autowired
    protected ProfileFilterDao<ProfileFilter> profileFilterDao;

    private KeyPair keyPair;

    @BeforeClass
    public static void init() throws Exception {
        MongoDBTestRunner.setUp();
    }

    @AfterClass
    public static void after() throws Exception {
        MongoDBTestRunner.tearDown();
    }

    @Before
    public void beforeTest() throws IOException, NoSuchAlgorithmException, SQLException {
        clearDBData();

        keyPair = KeyUtil.generateKeyPair();
        operationsService.setPublicKey(keyPair.getPublic());

        String dataSchema = getResourceAsString(DATA_SCHEMA_LOCATION);
        deltaSchema = getResourceAsString(BASE_SCHEMA_LOCATION);
        currentConfiguration = getResourceAsString(NEW_COMPLEX_CONFIG);

        customer = new Tenant();
        customer.setName(CUSTOMER_ID);
        customer = customerDao.save(customer);
        Assert.assertNotNull(customer);
        Assert.assertNotNull(customer.getId());

        Application appObj = new Application();
        appObj.setTenant(customer);
        appObj.setName(APPLICATION_NAME);
        appObj.setUserVerifierName("kaaEndpointUserVerifier");

        ApplicationDto applicationDto = applicationService.saveApp(appObj.toDto());
        Assert.assertNotNull(applicationDto);
        Assert.assertNotNull(applicationDto.getId());

        application = applicationDao.findById(applicationDto.getId());


        EndpointGroup groupAll = endpointGroupDao.findByAppIdAndWeight(application.getStringId(), 0);

        ProfileSchema profileSchemaObj = new ProfileSchema();
        profileSchemaObj.setMajorVersion(PROFILE_SCHEMA_VERSION);
        profileSchemaObj.setMinorVersion(0);
        profileSchemaObj.setSchema(BasicEndpointProfile.SCHEMA$.toString());
        profileSchemaObj.setApplication(application);
        ProfileSchemaDto profileSchemaDto = profileService.saveProfileSchema(profileSchemaObj.toDto());

        profileSchema = profileSchemaDao.findById(profileSchemaDto.getId());

        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setApplication(application);
        endpointGroup.setName("Test group");
        endpointGroup.setWeight(177);
        endpointGroup.setDescription("Test Description");
        endpointGroup = endpointGroupDao.save(endpointGroup);

        ProfileFilter profileFilterObj = new ProfileFilter();
        profileFilterObj.setApplication(application);
        profileFilterObj.setEndpointGroup(endpointGroup);
        profileFilterObj.setBody("profileBody.contains(\"dummy\")");
        profileFilterObj.setProfileSchema(profileSchema);
        profileFilter = profileService.saveProfileFilter(profileFilterObj.toDto());
        profileService.activateProfileFilter(profileFilter.getId(), null);

        confSchema = new ConfigurationSchema();
        confSchema.setApplication(application);
        confSchema.setMajorVersion(CONF_SCHEMA_VERSION);
        confSchema.setMinorVersion(CONF_SCHEMA_VERSION);
        confSchema.setSchema(dataSchema);
        try {
            confSchema = new ConfigurationSchema(configurationService.saveConfSchema(confSchema.toDto()));
        } catch (IncorrectParameterException e) {
            Assert.fail("Can't generate schemas");
        }
        Assert.assertNotNull(confSchema);
        Assert.assertNotNull(confSchema.getId());

        EndpointUserDto userDto = new EndpointUserDto();
        userDto.setTenantId(customer.getId().toString());
        userDto.setExternalId(USER_EXTERNAL_ID);
        userDto.setAccessToken(USER_ACCESS_TOKEN);
        userDto = endpointService.saveEndpointUser(userDto);
        Assert.assertNotNull(userDto);
        Assert.assertNotNull(userDto.getId());

        mandatoryTopicDto = new TopicDto();
        mandatoryTopicDto.setApplicationId(applicationDto.getId());
        mandatoryTopicDto.setName("Mandatory");
        mandatoryTopicDto.setType(TopicTypeDto.MANDATORY);
        mandatoryTopicDto = topicService.saveTopic(mandatoryTopicDto);

        voluntaryTopicDto = new TopicDto();
        voluntaryTopicDto.setApplicationId(applicationDto.getId());
        voluntaryTopicDto.setName("Voluntary");
        voluntaryTopicDto.setType(TopicTypeDto.VOLUNTARY);
        voluntaryTopicDto = topicService.saveTopic(voluntaryTopicDto);


        List<EndpointGroupDto> groups =  endpointService.findEndpointGroupsByAppId(applicationDto.getId());
        for(EndpointGroupDto group : groups){
            endpointService.addTopicToEndpointGroup(group.getId(), mandatoryTopicDto.getId());
            endpointService.addTopicToEndpointGroup(group.getId(), voluntaryTopicDto.getId());
        }

        NotificationSchemaDto userSchemaDto = notificationService.findNotificationSchemaByAppIdAndTypeAndVersion(applicationDto.getId(), NotificationTypeDto.USER, 1);

        NotificationDto mNotificationDto = new NotificationDto();
        mNotificationDto.setTopicId(mandatoryTopicDto.getId());
        mNotificationDto.setSchemaId(userSchemaDto.getId());
        mNotificationDto.setType(NotificationTypeDto.USER);
        mNotificationDto.setBody("{\"message\": \"mandatory\"}".getBytes(Charset.forName("UTF-8")));
        notificationService.saveNotification(mNotificationDto);

        NotificationDto vNotificationDto = new NotificationDto();
        vNotificationDto.setTopicId(voluntaryTopicDto.getId());
        vNotificationDto.setSchemaId(userSchemaDto.getId());
        vNotificationDto.setType(NotificationTypeDto.USER);
        vNotificationDto.setBody("{\"message\": \"voluntary\"}".getBytes(Charset.forName("UTF-8")));
        notificationService.saveNotification(vNotificationDto);

    }

    @Test
    public void basicRegistrationTest() throws GetDeltaException, IOException {
        byte[] profile = avroConverter.encode(ENDPOINT_PROFILE);
        SyncRequest request = new SyncRequest();

        SyncRequestMetaData md = new SyncRequestMetaData();
        md.setApplicationToken(application.getApplicationToken());
        md.setEndpointPublicKeyHash(ByteBuffer.wrap(EndpointObjectHash.fromSHA1(ENDPOINT_KEY).getData()));
        request.setSyncRequestMetaData(md);

        ProfileSyncRequest profileSync = new ProfileSyncRequest(ByteBuffer.wrap(ENDPOINT_KEY),
                ByteBuffer.wrap(profile),
                new EndpointVersionInfo(CONF_SCHEMA_VERSION, PROFILE_SCHEMA_VERSION, 1, 1, null, 1),
                null);
        request.setProfileSyncRequest(profileSync);

        request.setConfigurationSyncRequest(new ConfigurationSyncRequest());

        SyncResponseHolder holder = operationsService.sync(request);
        currentConfigurationHash = holder.getEndpointProfile().getConfigurationHash();
        SyncResponse response = holder.getResponse();
        Assert.assertNotNull(response);
        Assert.assertEquals(SyncResponseResultType.SUCCESS, response.getStatus());
        Assert.assertNotNull(response.getConfigurationSyncResponse());
        Assert.assertEquals(SyncResponseStatus.RESYNC, response.getConfigurationSyncResponse().getResponseStatus());
        Assert.assertEquals(APPLICATION_SEQ_NUMBER, (int) response.getConfigurationSyncResponse().getAppStateSeqNumber());
        Assert.assertNotNull(response.getConfigurationSyncResponse().getConfDeltaBody());
        // Kaa #7786
        Assert.assertNull(response.getConfigurationSyncResponse().getConfSchemaBody());
    }

    @Test
    public void basicDoubleRegistrationTest() throws GetDeltaException, IOException {
        byte[] profile = avroConverter.encode(ENDPOINT_PROFILE);
        SyncRequest request = new SyncRequest();

        SyncRequestMetaData md = new SyncRequestMetaData();
        md.setApplicationToken(application.getApplicationToken());
        md.setEndpointPublicKeyHash(ByteBuffer.wrap(EndpointObjectHash.fromSHA1(ENDPOINT_KEY).getData()));
        request.setSyncRequestMetaData(md);

        ProfileSyncRequest profileSync = new ProfileSyncRequest(ByteBuffer.wrap(ENDPOINT_KEY),
                ByteBuffer.wrap(profile),
                new EndpointVersionInfo(CONF_SCHEMA_VERSION, PROFILE_SCHEMA_VERSION, 1, 1, null, 1),
                null);
        request.setProfileSyncRequest(profileSync);

        request.setConfigurationSyncRequest(new ConfigurationSyncRequest());

        SyncResponse response = operationsService.sync(request).getResponse();

        Assert.assertNotNull(response);
        Assert.assertEquals(SyncResponseResultType.SUCCESS, response.getStatus());
        Assert.assertNotNull(response.getConfigurationSyncResponse());
        Assert.assertEquals(SyncResponseStatus.RESYNC, response.getConfigurationSyncResponse().getResponseStatus());
        Assert.assertEquals(APPLICATION_SEQ_NUMBER, (int) response.getConfigurationSyncResponse().getAppStateSeqNumber());
        Assert.assertNotNull(response.getConfigurationSyncResponse().getConfDeltaBody());
        // Kaa #7786
        Assert.assertNull(response.getConfigurationSyncResponse().getConfSchemaBody());

        response = operationsService.sync(request).getResponse();
        Assert.assertNotNull(response);
        Assert.assertEquals(SyncResponseResultType.SUCCESS, response.getStatus());
        Assert.assertNotNull(response.getConfigurationSyncResponse());
        Assert.assertEquals(SyncResponseStatus.RESYNC, response.getConfigurationSyncResponse().getResponseStatus());
        Assert.assertEquals(APPLICATION_SEQ_NUMBER, (int) response.getConfigurationSyncResponse().getAppStateSeqNumber());
        Assert.assertNotNull(response.getConfigurationSyncResponse().getConfDeltaBody());
        // Kaa #7786
        Assert.assertNull(response.getConfigurationSyncResponse().getConfSchemaBody());
    }

    @Test
    public void basicUpdateTest() throws GetDeltaException, IOException {
        basicRegistrationTest();
        byte[] oldProfile = avroConverter.encode(ENDPOINT_PROFILE);
        byte[] profile = avroConverter.encode(NEW_ENDPOINT_PROFILE);
        SyncRequest request = new SyncRequest();
        SyncRequestMetaData md = new SyncRequestMetaData();
        md.setApplicationToken(application.getApplicationToken());
        md.setEndpointPublicKeyHash(ByteBuffer.wrap(EndpointObjectHash.fromSHA1(ENDPOINT_KEY).getData()));
        md.setProfileHash(ByteBuffer.wrap(EndpointObjectHash.fromSHA1(oldProfile).getData()));
        request.setSyncRequestMetaData(md);

        ProfileSyncRequest profileSync = new ProfileSyncRequest(null,
                ByteBuffer.wrap(profile),
                new EndpointVersionInfo(CONF_SCHEMA_VERSION, PROFILE_SCHEMA_VERSION, 1, 1, null, 1),
                null);
        request.setProfileSyncRequest(profileSync);

        ConfigurationSyncRequest confSyncRequest = new ConfigurationSyncRequest();
        confSyncRequest.setAppStateSeqNumber(APPLICATION_SEQ_NUMBER);
        confSyncRequest.setConfigurationHash(ByteBuffer.wrap(currentConfigurationHash));

        request.setConfigurationSyncRequest(confSyncRequest);

        SyncResponse response = operationsService.sync(request).getResponse();
        Assert.assertNotNull(response);
        Assert.assertEquals(SyncResponseResultType.SUCCESS, response.getStatus());
        Assert.assertNotNull(response.getConfigurationSyncResponse());
        Assert.assertEquals(SyncResponseStatus.NO_DELTA, response.getConfigurationSyncResponse().getResponseStatus());
        Assert.assertEquals(APPLICATION_SEQ_NUMBER, (int) response.getConfigurationSyncResponse().getAppStateSeqNumber());
        Assert.assertNull(response.getConfigurationSyncResponse().getConfDeltaBody());
        // Kaa #7786
        Assert.assertNull(response.getConfigurationSyncResponse().getConfSchemaBody());
    }

    @Test
    public void basicProfileResyncTest() throws GetDeltaException, IOException {
        basicRegistrationTest();
        byte[] profile = avroConverter.encode(FAKE_ENDPOINT_PROFILE);

        SyncRequest request = new SyncRequest();

        SyncRequestMetaData md = new SyncRequestMetaData();
        md.setApplicationToken(application.getApplicationToken());
        md.setEndpointPublicKeyHash(ByteBuffer.wrap(EndpointObjectHash.fromSHA1(ENDPOINT_KEY).getData()));
        md.setProfileHash(ByteBuffer.wrap(EndpointObjectHash.fromSHA1(profile).getData()));
        request.setSyncRequestMetaData(md);

        ConfigurationSyncRequest confSyncRequest = new ConfigurationSyncRequest();
        confSyncRequest.setAppStateSeqNumber(APPLICATION_SEQ_NUMBER - 1);
        confSyncRequest.setConfigurationHash(ByteBuffer.wrap(currentConfigurationHash));

        request.setConfigurationSyncRequest(confSyncRequest);

        SyncResponse response = operationsService.sync(request).getResponse();
        Assert.assertNotNull(response);
        Assert.assertEquals(SyncResponseResultType.PROFILE_RESYNC, response.getStatus());
        Assert.assertNull(response.getConfigurationSyncResponse());
        Assert.assertNull(response.getNotificationSyncResponse());
    }

    @Test
    public void basicDeltaTest() throws GetDeltaException, IOException {
        basicRegistrationTest();
        byte[] profile = avroConverter.encode(ENDPOINT_PROFILE);

        SyncRequest request = new SyncRequest();

        SyncRequestMetaData md = new SyncRequestMetaData();
        md.setApplicationToken(application.getApplicationToken());
        md.setEndpointPublicKeyHash(ByteBuffer.wrap(EndpointObjectHash.fromSHA1(ENDPOINT_KEY).getData()));
        md.setProfileHash(ByteBuffer.wrap(EndpointObjectHash.fromSHA1(profile).getData()));
        request.setSyncRequestMetaData(md);

        ConfigurationSyncRequest confSyncRequest = new ConfigurationSyncRequest();
        confSyncRequest.setAppStateSeqNumber(APPLICATION_SEQ_NUMBER);
        confSyncRequest.setConfigurationHash(ByteBuffer.wrap(currentConfigurationHash));

        request.setConfigurationSyncRequest(confSyncRequest);

        SyncResponse response = operationsService.sync(request).getResponse();
        Assert.assertNotNull(response);
        Assert.assertEquals(SyncResponseResultType.SUCCESS, response.getStatus());
        Assert.assertNotNull(response.getConfigurationSyncResponse());
        Assert.assertEquals(SyncResponseStatus.NO_DELTA, response.getConfigurationSyncResponse().getResponseStatus());
        Assert.assertEquals(Integer.valueOf(APPLICATION_SEQ_NUMBER), response.getConfigurationSyncResponse().getAppStateSeqNumber());
        Assert.assertNull(response.getConfigurationSyncResponse().getConfDeltaBody());
        // Kaa #7786
        Assert.assertNull(response.getConfigurationSyncResponse().getConfSchemaBody());
        Assert.assertNull(response.getNotificationSyncResponse());
    }

    @Test
    public void basicMandatoryNotificationsTest() throws GetDeltaException, IOException {
        basicRegistrationTest();
        byte[] profile = avroConverter.encode(ENDPOINT_PROFILE);

        SyncRequest request = new SyncRequest();

        SyncRequestMetaData md = new SyncRequestMetaData();
        md.setApplicationToken(application.getApplicationToken());
        md.setEndpointPublicKeyHash(ByteBuffer.wrap(EndpointObjectHash.fromSHA1(ENDPOINT_KEY).getData()));
        md.setProfileHash(ByteBuffer.wrap(EndpointObjectHash.fromSHA1(profile).getData()));
        request.setSyncRequestMetaData(md);

        NotificationSyncRequest nfSyncRequest = new NotificationSyncRequest();
        nfSyncRequest.setAppStateSeqNumber(APPLICATION_SEQ_NUMBER);

        request.setNotificationSyncRequest(nfSyncRequest);

        SyncResponse response = operationsService.sync(request).getResponse();
        Assert.assertNotNull(response);
        Assert.assertEquals(SyncResponseResultType.SUCCESS, response.getStatus());
        Assert.assertNotNull(response.getNotificationSyncResponse());
        Assert.assertEquals(SyncResponseStatus.DELTA, response.getNotificationSyncResponse().getResponseStatus());
        Assert.assertEquals(Integer.valueOf(APPLICATION_SEQ_NUMBER), response.getNotificationSyncResponse().getAppStateSeqNumber());
        Assert.assertNotNull(response.getNotificationSyncResponse().getNotifications());
        //Only mandatory notification
        Assert.assertEquals(1, response.getNotificationSyncResponse().getNotifications().size());
    }

    @Test
    public void basicVoluntaryNotificationsTest() throws GetDeltaException, IOException {
        basicRegistrationTest();
        byte[] profile = avroConverter.encode(ENDPOINT_PROFILE);

        SyncRequest request = new SyncRequest();

        SyncRequestMetaData md = new SyncRequestMetaData();
        md.setApplicationToken(application.getApplicationToken());
        md.setEndpointPublicKeyHash(ByteBuffer.wrap(EndpointObjectHash.fromSHA1(ENDPOINT_KEY).getData()));
        md.setProfileHash(ByteBuffer.wrap(EndpointObjectHash.fromSHA1(profile).getData()));
        request.setSyncRequestMetaData(md);

        NotificationSyncRequest nfSyncRequest = new NotificationSyncRequest();
        nfSyncRequest.setAppStateSeqNumber(APPLICATION_SEQ_NUMBER);
        SubscriptionCommand command = new SubscriptionCommand(voluntaryTopicDto.getId(), SubscriptionCommandType.ADD);
        nfSyncRequest.setSubscriptionCommands(Collections.singletonList(command));
        request.setNotificationSyncRequest(nfSyncRequest);

        SyncResponse response = operationsService.sync(request).getResponse();
        Assert.assertNotNull(response);
        Assert.assertEquals(SyncResponseResultType.SUCCESS, response.getStatus());
        Assert.assertNotNull(response.getNotificationSyncResponse());
        Assert.assertEquals(SyncResponseStatus.DELTA, response.getNotificationSyncResponse().getResponseStatus());
        Assert.assertEquals(Integer.valueOf(APPLICATION_SEQ_NUMBER), response.getNotificationSyncResponse().getAppStateSeqNumber());
        Assert.assertNotNull(response.getNotificationSyncResponse().getNotifications());
        //Mandatory + Voluntary notification
        Assert.assertEquals(2, response.getNotificationSyncResponse().getNotifications().size());
    }

    @Test
    public void basicUserAttachTest() throws GetDeltaException, IOException {
        basicRegistrationTest();
        byte[] profile = avroConverter.encode(ENDPOINT_PROFILE);

        SyncRequest request = new SyncRequest();

        SyncRequestMetaData md = new SyncRequestMetaData();
        md.setApplicationToken(application.getApplicationToken());
        md.setEndpointPublicKeyHash(ByteBuffer.wrap(EndpointObjectHash.fromSHA1(ENDPOINT_KEY).getData()));
        md.setProfileHash(ByteBuffer.wrap(EndpointObjectHash.fromSHA1(profile).getData()));
        request.setSyncRequestMetaData(md);

        UserSyncRequest userRequest = new UserSyncRequest();
        userRequest.setUserAttachRequest(new UserAttachRequest(USER_EXTERNAL_ID, USER_ACCESS_TOKEN));
        request.setUserSyncRequest(userRequest);


        SyncResponse response = operationsService.sync(request).getResponse();
        Assert.assertNotNull(response);
        Assert.assertEquals(SyncResponseResultType.SUCCESS, response.getStatus());
        Assert.assertNull(response.getConfigurationSyncResponse());
        Assert.assertNull(response.getNotificationSyncResponse());
        Assert.assertNotNull(response.getUserSyncResponse());
        Assert.assertNotNull(response.getUserSyncResponse().getUserAttachResponse());
        Assert.assertEquals(SyncResponseResultType.SUCCESS, response.getUserSyncResponse().getUserAttachResponse().getResult());
    }

    @Test
    public void basicUserAttachFailTest() throws GetDeltaException, IOException {
        basicRegistrationTest();
        byte[] profile = avroConverter.encode(ENDPOINT_PROFILE);

        SyncRequest request = new SyncRequest();

        SyncRequestMetaData md = new SyncRequestMetaData();
        md.setApplicationToken(application.getApplicationToken());
        md.setEndpointPublicKeyHash(ByteBuffer.wrap(EndpointObjectHash.fromSHA1(ENDPOINT_KEY).getData()));
        md.setProfileHash(ByteBuffer.wrap(EndpointObjectHash.fromSHA1(profile).getData()));
        request.setSyncRequestMetaData(md);

        UserSyncRequest userRequest = new UserSyncRequest();
        userRequest.setUserAttachRequest(new UserAttachRequest(USER_EXTERNAL_ID, INVALID_USER_ACCESS_TOKEN));
        request.setUserSyncRequest(userRequest);

        SyncResponse response = operationsService.sync(request).getResponse();
        Assert.assertNotNull(response);
        Assert.assertEquals(SyncResponseResultType.SUCCESS, response.getStatus());
        Assert.assertNull(response.getConfigurationSyncResponse());
        Assert.assertNull(response.getNotificationSyncResponse());
        Assert.assertNotNull(response.getUserSyncResponse());
        Assert.assertNotNull(response.getUserSyncResponse().getUserAttachResponse());
        Assert.assertEquals(SyncResponseResultType.FAILURE, response.getUserSyncResponse().getUserAttachResponse().getResult());
    }

    private void createSecondEndpoint() throws GetDeltaException, IOException {
        byte[] profile = avroConverter.encode(ENDPOINT_PROFILE);

        SyncRequest request = new SyncRequest();

        SyncRequestMetaData md = new SyncRequestMetaData();
        md.setApplicationToken(application.getApplicationToken());
        md.setEndpointPublicKeyHash(ByteBuffer.wrap(EndpointObjectHash.fromSHA1(ENDPOINT_KEY2).getData()));
        request.setSyncRequestMetaData(md);

        ProfileSyncRequest profileSync = new ProfileSyncRequest(ByteBuffer.wrap(ENDPOINT_KEY2),
                ByteBuffer.wrap(profile),
                new EndpointVersionInfo(CONF_SCHEMA_VERSION, PROFILE_SCHEMA_VERSION, 1, 1, null, 1),
                ENDPOINT_ACCESS_TOKEN);
        request.setProfileSyncRequest(profileSync);

        request.setConfigurationSyncRequest(new ConfigurationSyncRequest());

        SyncResponse response = operationsService.sync(request).getResponse();
        Assert.assertNotNull(response);
        Assert.assertEquals(SyncResponseResultType.SUCCESS, response.getStatus());
    }

    @Test
    public void basicEndpointAttachTest() throws GetDeltaException, IOException {
        // register main endpoint
        basicRegistrationTest();
        // register second endpoint
        createSecondEndpoint();

        byte[] profile = avroConverter.encode(ENDPOINT_PROFILE);

        SyncRequest request = new SyncRequest();

        SyncRequestMetaData md = new SyncRequestMetaData();
        md.setApplicationToken(application.getApplicationToken());
        md.setEndpointPublicKeyHash(ByteBuffer.wrap(EndpointObjectHash.fromSHA1(ENDPOINT_KEY).getData()));
        md.setProfileHash(ByteBuffer.wrap(EndpointObjectHash.fromSHA1(profile).getData()));
        request.setSyncRequestMetaData(md);

        UserSyncRequest userRequest = new UserSyncRequest();
        userRequest.setUserAttachRequest(new UserAttachRequest(USER_EXTERNAL_ID, USER_ACCESS_TOKEN));
        userRequest.setEndpointAttachRequests(Collections.singletonList(new EndpointAttachRequest(REQUEST_ID1, ENDPOINT_ACCESS_TOKEN)));
        request.setUserSyncRequest(userRequest);

        SyncResponse response = operationsService.sync(request).getResponse();
        Assert.assertNotNull(response);
        Assert.assertEquals(SyncResponseResultType.SUCCESS, response.getStatus());
        Assert.assertNull(response.getConfigurationSyncResponse());
        Assert.assertNull(response.getNotificationSyncResponse());
        Assert.assertNotNull(response.getUserSyncResponse());
        Assert.assertNotNull(response.getUserSyncResponse().getUserAttachResponse());
        Assert.assertEquals(SyncResponseResultType.SUCCESS, response.getUserSyncResponse().getUserAttachResponse().getResult());
        Assert.assertNotNull(response.getUserSyncResponse().getEndpointAttachResponses());
        Assert.assertEquals(1, response.getUserSyncResponse().getEndpointAttachResponses().size());
        Assert.assertEquals(SyncResponseResultType.SUCCESS, response.getUserSyncResponse().getEndpointAttachResponses().get(0).getResult());
    }

    @Test
    public void basicEndpointAttachFailTest() throws GetDeltaException, IOException {
        // register main endpoint
        basicRegistrationTest();
        // register second endpoint
        createSecondEndpoint();

        byte[] profile = avroConverter.encode(ENDPOINT_PROFILE);

        SyncRequest request = new SyncRequest();

        SyncRequestMetaData md = new SyncRequestMetaData();
        md.setApplicationToken(application.getApplicationToken());
        md.setEndpointPublicKeyHash(ByteBuffer.wrap(EndpointObjectHash.fromSHA1(ENDPOINT_KEY).getData()));
        md.setProfileHash(ByteBuffer.wrap(EndpointObjectHash.fromSHA1(profile).getData()));
        request.setSyncRequestMetaData(md);

        UserSyncRequest userRequest = new UserSyncRequest();
        userRequest.setUserAttachRequest(new UserAttachRequest(USER_EXTERNAL_ID, USER_ACCESS_TOKEN));
        userRequest.setEndpointAttachRequests(Collections.singletonList(new EndpointAttachRequest(REQUEST_ID1, INVALID_ENDPOINT_ACCESS_TOKEN)));
        request.setUserSyncRequest(userRequest);

        SyncResponse response = operationsService.sync(request).getResponse();
        Assert.assertNotNull(response);
        Assert.assertEquals(SyncResponseResultType.SUCCESS, response.getStatus());
        Assert.assertNull(response.getConfigurationSyncResponse());
        Assert.assertNull(response.getNotificationSyncResponse());
        Assert.assertNotNull(response.getUserSyncResponse());
        Assert.assertNotNull(response.getUserSyncResponse().getUserAttachResponse());
        Assert.assertEquals(SyncResponseResultType.SUCCESS, response.getUserSyncResponse().getUserAttachResponse().getResult());
        Assert.assertNotNull(response.getUserSyncResponse().getEndpointAttachResponses());
        Assert.assertEquals(1, response.getUserSyncResponse().getEndpointAttachResponses().size());
        Assert.assertEquals(SyncResponseResultType.FAILURE, response.getUserSyncResponse().getEndpointAttachResponses().get(0).getResult());
    }

    @Test
    public void basicEndpointDetachTest() throws GetDeltaException, IOException {
        // register main endpoint
        basicEndpointAttachTest();
        byte[] profile = avroConverter.encode(ENDPOINT_PROFILE);

        SyncRequest request = new SyncRequest();

        SyncRequestMetaData md = new SyncRequestMetaData();
        md.setApplicationToken(application.getApplicationToken());
        md.setEndpointPublicKeyHash(ByteBuffer.wrap(EndpointObjectHash.fromSHA1(ENDPOINT_KEY).getData()));
        md.setProfileHash(ByteBuffer.wrap(EndpointObjectHash.fromSHA1(profile).getData()));
        request.setSyncRequestMetaData(md);

        UserSyncRequest userRequest = new UserSyncRequest();
        userRequest.setUserAttachRequest(new UserAttachRequest(USER_EXTERNAL_ID, USER_ACCESS_TOKEN));
        userRequest.setEndpointDetachRequests(Collections.singletonList(new EndpointDetachRequest(REQUEST_ID1, Base64Util.encode(EndpointObjectHash.fromSHA1(ENDPOINT_KEY2).getData()))));
        request.setUserSyncRequest(userRequest);

        SyncResponse response = operationsService.sync(request).getResponse();
        Assert.assertNotNull(response);
        Assert.assertEquals(SyncResponseResultType.SUCCESS, response.getStatus());
        Assert.assertNull(response.getConfigurationSyncResponse());
        Assert.assertNull(response.getNotificationSyncResponse());
        Assert.assertNotNull(response.getUserSyncResponse());
        Assert.assertNotNull(response.getUserSyncResponse().getUserAttachResponse());
        Assert.assertEquals(SyncResponseResultType.SUCCESS, response.getUserSyncResponse().getUserAttachResponse().getResult());
        Assert.assertNotNull(response.getUserSyncResponse().getEndpointDetachResponses());
        Assert.assertEquals(1, response.getUserSyncResponse().getEndpointDetachResponses().size());
        Assert.assertEquals(SyncResponseResultType.SUCCESS, response.getUserSyncResponse().getEndpointDetachResponses().get(0).getResult());
    }

    @Test
    public void basicEndpointDetachFailTest() throws GetDeltaException, IOException {
        // register main endpoint
        basicRegistrationTest();
        // register second endpoint
        createSecondEndpoint();

        byte[] profile = avroConverter.encode(ENDPOINT_PROFILE);

        SyncRequest request = new SyncRequest();

        SyncRequestMetaData md = new SyncRequestMetaData();
        md.setApplicationToken(application.getApplicationToken());
        md.setEndpointPublicKeyHash(ByteBuffer.wrap(EndpointObjectHash.fromSHA1(ENDPOINT_KEY).getData()));
        md.setProfileHash(ByteBuffer.wrap(EndpointObjectHash.fromSHA1(profile).getData()));
        request.setSyncRequestMetaData(md);

        UserSyncRequest userRequest = new UserSyncRequest();
        userRequest.setUserAttachRequest(new UserAttachRequest(USER_EXTERNAL_ID, USER_ACCESS_TOKEN));
        userRequest.setEndpointDetachRequests(Collections.singletonList(new EndpointDetachRequest(REQUEST_ID1, Base64Util.encode(EndpointObjectHash.fromSHA1(ENDPOINT_KEY2).getData()))));
        request.setUserSyncRequest(userRequest);

        SyncResponse response = operationsService.sync(request).getResponse();
        Assert.assertNotNull(response);
        Assert.assertEquals(SyncResponseResultType.SUCCESS, response.getStatus());
        Assert.assertNull(response.getConfigurationSyncResponse());
        Assert.assertNull(response.getNotificationSyncResponse());
        Assert.assertNotNull(response.getUserSyncResponse());
        Assert.assertNotNull(response.getUserSyncResponse().getUserAttachResponse());
        Assert.assertEquals(SyncResponseResultType.SUCCESS, response.getUserSyncResponse().getUserAttachResponse().getResult());
        Assert.assertNotNull(response.getUserSyncResponse().getEndpointDetachResponses());
        Assert.assertEquals(1, response.getUserSyncResponse().getEndpointDetachResponses().size());
        Assert.assertEquals(SyncResponseResultType.FAILURE, response.getUserSyncResponse().getEndpointDetachResponses().get(0).getResult());
    }

    @Test
    public void basicEventListenerFailTest() throws GetDeltaException, IOException {
        // register main endpoint
        basicRegistrationTest();

        byte[] profile = avroConverter.encode(ENDPOINT_PROFILE);

        SyncRequest request = new SyncRequest();

        SyncRequestMetaData md = new SyncRequestMetaData();
        md.setApplicationToken(application.getApplicationToken());
        md.setEndpointPublicKeyHash(ByteBuffer.wrap(EndpointObjectHash.fromSHA1(ENDPOINT_KEY).getData()));
        md.setProfileHash(ByteBuffer.wrap(EndpointObjectHash.fromSHA1(profile).getData()));
        request.setSyncRequestMetaData(md);

        EventSyncRequest eventRequest = new EventSyncRequest();
        eventRequest.setEventListenersRequests(Collections.singletonList(new EventListenersRequest(REQUEST_ID1, Arrays.asList("fqn"))));

        request.setEventSyncRequest(eventRequest);

        SyncResponse response = operationsService.sync(request).getResponse();
        Assert.assertNotNull(response);
        Assert.assertEquals(SyncResponseResultType.SUCCESS, response.getStatus());
        Assert.assertNull(response.getConfigurationSyncResponse());
        Assert.assertNull(response.getNotificationSyncResponse());
        Assert.assertNotNull(response.getEventSyncResponse());
        Assert.assertNotNull(response.getEventSyncResponse().getEventListenersResponses());
        Assert.assertEquals(1, response.getEventSyncResponse().getEventListenersResponses().size());
        Assert.assertEquals(SyncResponseResultType.FAILURE, response.getEventSyncResponse().getEventListenersResponses().get(0).getResult());
    }


    @After
    public void afterTest() {
        endpointConfigurationDao.removeAll();
    }

    public static String getResourceAsString(String path) throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(path);
        File file = new File(url.getPath());
        String result;
        BufferedReader br = new BufferedReader(new FileReader(file));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            result = sb.toString();
        } finally {
            br.close();
        }
        return result;
    }
}
