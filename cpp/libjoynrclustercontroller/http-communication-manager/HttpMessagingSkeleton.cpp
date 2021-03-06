/*
 * #%L
 * %%
 * Copyright (C) 2011 - 2017 BMW Car IT GmbH
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
#include "libjoynrclustercontroller/http-communication-manager/HttpMessagingSkeleton.h"

#include <smrf/exceptions.h>

#include "joynr/IMessageRouter.h"
#include "joynr/ImmutableMessage.h"
#include "joynr/Message.h"
#include "joynr/exceptions/JoynrException.h"

namespace joynr
{

INIT_LOGGER(HttpMessagingSkeleton);

HttpMessagingSkeleton::HttpMessagingSkeleton(IMessageRouter& messageRouter)
        : messageRouter(messageRouter)
{
}

void HttpMessagingSkeleton::transmit(
        std::shared_ptr<ImmutableMessage> message,
        const std::function<void(const exceptions::JoynrRuntimeException&)>& onFailure)
{
    message->setReceivedFromGlobal(true);

    try {
        messageRouter.route(std::move(message));
    } catch (exceptions::JoynrRuntimeException& e) {
        onFailure(e);
    }
}

void HttpMessagingSkeleton::onMessageReceived(smrf::ByteVector&& message)
{
    // `message` potentially contains multipe SMRF messages
    // hence we need to split this stream into single messages in order to
    // route them separately
    const std::size_t inputSize = message.size();
    std::size_t remainingSize = inputSize;
    const smrf::Byte* end = message.data() + inputSize;
    while (remainingSize > 0) {
        const std::size_t offset = inputSize - remainingSize;
        const smrf::Byte* start = message.data() + offset;
        smrf::ByteVector splittedMessage(start, end);
        try {
            auto immutableMessage = std::make_shared<ImmutableMessage>(std::move(splittedMessage));
            remainingSize -= immutableMessage->getMessageSize();

            JOYNR_LOG_DEBUG(logger, "<<< INCOMING <<< {}", immutableMessage->toLogMessage());

            auto onFailure = [messageId = immutableMessage->getId()](
                    const exceptions::JoynrRuntimeException& e)
            {
                JOYNR_LOG_ERROR(logger,
                                "Incoming Message with ID {} could not be sent! reason: {}",
                                messageId,
                                e.getMessage());
            };
            transmit(std::move(immutableMessage), onFailure);
        } catch (const smrf::EncodingException& e) {
            JOYNR_LOG_ERROR(logger, "Unable to deserialize message - error: {}", e.what());
            return;
        } catch (const std::invalid_argument& e) {
            JOYNR_LOG_ERROR(logger, "deserialized message is not valid - error: {}", e.what());
            return;
        }
    }
}

} // namespace joynr
