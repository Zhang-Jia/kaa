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

package org.kaaproject.kaa.server.admin.client.mvp.activity;

import org.kaaproject.kaa.common.dto.TopicDto;
import org.kaaproject.kaa.server.admin.client.KaaAdmin;
import org.kaaproject.kaa.server.admin.client.mvp.ClientFactory;
import org.kaaproject.kaa.server.admin.client.mvp.activity.grid.AbstractDataProvider;
import org.kaaproject.kaa.server.admin.client.mvp.data.TopicsDataProvider;
import org.kaaproject.kaa.server.admin.client.mvp.event.grid.RowAction;
import org.kaaproject.kaa.server.admin.client.mvp.event.grid.RowActionEvent;
import org.kaaproject.kaa.server.admin.client.mvp.place.SendNotificationPlace;
import org.kaaproject.kaa.server.admin.client.mvp.place.TopicPlace;
import org.kaaproject.kaa.server.admin.client.mvp.place.TopicsPlace;
import org.kaaproject.kaa.server.admin.client.mvp.view.BaseListView;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.view.client.MultiSelectionModel;

public class TopicsActivity extends AbstractListActivity<TopicDto, TopicsPlace> {

    private String applicationId;

    public TopicsActivity(TopicsPlace place, ClientFactory clientFactory) {
        super(place, TopicDto.class, clientFactory);
        this.applicationId = place.getApplicationId();
    }

    @Override
    protected BaseListView<TopicDto> getView() {
        return clientFactory.getTopicsView();
    }

    @Override
    protected AbstractDataProvider<TopicDto> getDataProvider(
            MultiSelectionModel<TopicDto> selectionModel) {
        return new TopicsDataProvider(selectionModel, listView, applicationId, null);
    }

    @Override
    protected Place newEntityPlace() {
        return new TopicPlace(applicationId, "");
    }

    @Override
    protected Place existingEntityPlace(String id) {
        return new TopicPlace(applicationId, id);
    }

    @Override
    protected void deleteEntity(String id, AsyncCallback<Void> callback) {
        KaaAdmin.getDataSource().deleteTopic(id, callback);
    }

    @Override
    protected void onCustomRowAction(RowActionEvent<String> event) {
        if (event.getAction()==RowAction.SEND_NOTIFICATION) {
            String id = event.getClickedId();
            sendNotification(id);
        }
    }

    private void sendNotification(String topicId) {
        SendNotificationPlace sendNotificationPlace = new SendNotificationPlace(applicationId, topicId);
        sendNotificationPlace.setPreviousPlace(place);
        goTo(sendNotificationPlace);
    }

}