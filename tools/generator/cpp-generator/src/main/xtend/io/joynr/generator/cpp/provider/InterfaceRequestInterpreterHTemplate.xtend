package io.joynr.generator.cpp.provider
/*
 * !!!
 *
 * Copyright (C) 2011 - 2015 BMW Car IT GmbH
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

import com.google.inject.Inject
import org.franca.core.franca.FInterface
import io.joynr.generator.cpp.util.TemplateBase
import io.joynr.generator.cpp.util.JoynrCppGeneratorExtensions
import io.joynr.generator.util.InterfaceTemplate

class InterfaceRequestInterpreterHTemplate implements InterfaceTemplate{

	@Inject
	private extension TemplateBase

	@Inject
	private extension JoynrCppGeneratorExtensions

	override generate(FInterface serviceInterface)
'''
«val interfaceName = serviceInterface.joynrName»
«val headerGuard = ("GENERATED_INTERFACE_"+getPackagePathWithJoynrPrefix(serviceInterface, "_")+
	"_"+interfaceName+"RequestInterpreter_h").toUpperCase»
«warning()»

#ifndef «headerGuard»
#define «headerGuard»

#include "joynr/PrivateCopyAssign.h"
«getDllExportIncludeStatement()»
#include "joynr/IRequestInterpreter.h"

#include "joynr/joynrlogging.h"

#include <QVariant>
#include <QSharedPointer>

«getNamespaceStarter(serviceInterface)»

/** @brief RequestInterpreter class for interface «interfaceName» */
class «getDllExportMacro()» «interfaceName»RequestInterpreter: public joynr::IRequestInterpreter {
public:
	/** @brief Default constructor */
	«interfaceName»RequestInterpreter();

	/** @brief Destructor */
	virtual ~«interfaceName»RequestInterpreter(){}

	/**
	 * @brief Implements IRequestInterpreter.execute().
	 * Executes method methodName with given parameters on the requestCaller object.
	 * @param requestCaller Object on which the method is to be executed
	 * @param methodName The name of the method to be executed
	 * @param paramValues The list of parameter values
	 * @param paramTypes The list of parameter types
	 * @param callbackFct A callback function to be called once the asynchronous computation has
	 * finished. It must expect the method out parameters.
	 */
	void execute(QSharedPointer<joynr::RequestCaller> requestCaller,
					 const QString& methodName,
					 const QList<QVariant>& paramValues,
					 const QList<QVariant>& paramTypes,
					 std::function<void (const QList<QVariant>& outParams)> callbackFct);

private:
	DISALLOW_COPY_AND_ASSIGN(«interfaceName»RequestInterpreter);
	static joynr::joynr_logging::Logger* logger;
};

«getNamespaceEnder(serviceInterface)»
#endif // «headerGuard»
'''
}