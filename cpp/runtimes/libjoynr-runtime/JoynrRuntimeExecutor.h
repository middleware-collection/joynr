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

#ifndef JOYNRRUNTIMEEXECUTOR_H
#define JOYNRRUNTIMEEXECUTOR_H

#include <QtCore/QObject>
#include <QtCore/QThread>
#include <QtCore/QSettings>
#include <QtCore/QSemaphore>
#include <QtCore/QCoreApplication>

#include "joynr/PrivateCopyAssign.h"

namespace joynr {

class LibJoynrRuntime;

class JoynrRuntimeExecutor : public QObject {
    Q_OBJECT

    LibJoynrRuntime *runtime;
    QSemaphore runtimeSemaphore;
    int argc;
    char **argv;
    QCoreApplication coreApplication;

public:
    JoynrRuntimeExecutor();
    virtual ~JoynrRuntimeExecutor();

    LibJoynrRuntime *create(QSettings *settings);
    void quit();

private:
    DISALLOW_COPY_AND_ASSIGN(JoynrRuntimeExecutor);
    void createRuntimeAndExecuteEventLoop(QSettings* settings);
};


} // namespace joynr
#endif //JOYNRRUNTIMEEXECUTOR_H
