/*
 * #%L
 * %%
 * Copyright (C) 2011 - 2014 BMW Car IT GmbH
 * %%
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
 * #L%
 */
#include "runtimes/libjoynr-runtime/dbus/LibJoynrDbusRuntime.h"

#include "common/dbus/DbusMessagingStubAdapter.h"
#include "joynr/system/RoutingTypes_QtCommonApiDbusAddress.h"
#include "joynr/DBusMessageRouterAdapter.h"
#include "common/dbus/DbusSettings.h"
#include "libjoynr/dbus/DbusMessagingStubFactory.h"
#include "joynr/Util.h"
#include "joynr/TypeUtil.h"

namespace joynr
{

LibJoynrDbusRuntime::LibJoynrDbusRuntime(Settings* settings)
        : LibJoynrRuntime(settings),
          dbusMessageRouterAdapter(Q_NULLPTR),
          dbusSettings(new DbusSettings(*settings)),
          libjoynrMessagingServiceUrl()
{
    dbusSettings->printSettings();

    QString messagingUuid = Util::createUuid().replace("-", "");
    QString libjoynrMessagingDomain("local");
    QString libjoynrMessagingServiceName("io.joynr.libjoynr.Messaging");
    QString libjoynrMessagingId("libjoynr.messaging.participantid_" + messagingUuid);
    libjoynrMessagingServiceUrl = QString(
            QString("%0:%1:%2").arg(libjoynrMessagingDomain).arg(libjoynrMessagingServiceName).arg(
                    libjoynrMessagingId));
    std::shared_ptr<joynr::system::RoutingTypes::QtAddress> libjoynrMessagingAddress(
            new system::RoutingTypes::QtCommonApiDbusAddress(
                    libjoynrMessagingDomain, libjoynrMessagingServiceName, libjoynrMessagingId));

    // create connection to parent routing service
    std::shared_ptr<joynr::system::RoutingTypes::QtAddress> ccMessagingAddress(
            new system::RoutingTypes::QtCommonApiDbusAddress(
                    TypeUtil::toQt(dbusSettings->getClusterControllerMessagingDomain()),
                    TypeUtil::toQt(dbusSettings->getClusterControllerMessagingServiceName()),
                    TypeUtil::toQt(dbusSettings->getClusterControllerMessagingParticipantId())));

    LibJoynrRuntime::init(
            new DbusMessagingStubFactory(), libjoynrMessagingAddress, ccMessagingAddress);
}

LibJoynrDbusRuntime::~LibJoynrDbusRuntime()
{
    delete dbusMessageRouterAdapter;
    dbusMessageRouterAdapter = Q_NULLPTR;
    delete dbusSettings;
    dbusSettings = Q_NULLPTR;
}

void LibJoynrDbusRuntime::startLibJoynrMessagingSkeleton(MessageRouter& messageRouter)
{
    // create messaging skeleton using uuid
    dbusMessageRouterAdapter = new DBusMessageRouterAdapter(
            messageRouter, TypeUtil::toStd(libjoynrMessagingServiceUrl));
}

} // namespace joynr
