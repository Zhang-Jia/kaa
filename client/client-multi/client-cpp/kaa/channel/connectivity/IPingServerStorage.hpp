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

#ifndef IPINGSERVERSTORAGE_HPP_
#define IPINGSERVERSTORAGE_HPP_

#include "kaa/channel/server/IServerInfo.hpp"

namespace kaa {

class IPingServerStorage {
public:
    virtual IServerInfoPtr getPingServer() = 0;
    virtual ~IPingServerStorage() {}
};

} /* namespace kaa */

#endif /* IPINGSERVERSTORAGE_HPP_ */