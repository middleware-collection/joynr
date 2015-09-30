package io.joynr.generator.cpp.defaultProvider
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
import io.joynr.generator.cpp.util.CppStdTypeUtil
import io.joynr.generator.cpp.util.JoynrCppGeneratorExtensions
import io.joynr.generator.cpp.util.TemplateBase
import io.joynr.generator.util.InterfaceTemplate
import org.franca.core.franca.FBasicTypeId
import org.franca.core.franca.FInterface

class DefaultInterfaceProviderCppTemplate implements InterfaceTemplate{

	@Inject
	private extension TemplateBase

	@Inject
	private extension CppStdTypeUtil

	@Inject
	private extension JoynrCppGeneratorExtensions

	override generate(FInterface serviceInterface)
'''
«val interfaceName = serviceInterface.joynrName»
«warning()»
#include "«getPackagePathWithJoynrPrefix(serviceInterface, "/")»/Default«interfaceName»Provider.h"

#include <chrono>
#include <cstdint>

#include "joynr/RequestStatus.h"
#include "joynr/joynrlogging.h"

«getNamespaceStarter(serviceInterface)»

using namespace joynr::joynr_logging;

Logger* Default«interfaceName»Provider::logger = Logging::getInstance()->getLogger("PROV", "Default«interfaceName»Provider");

Default«interfaceName»Provider::Default«interfaceName»Provider() :
		«interfaceName»AbstractProvider()«IF !serviceInterface.attributes.empty»,«ENDIF»
		«FOR attribute : serviceInterface.attributes SEPARATOR ","»
			«attribute.joynrName»()
		«ENDFOR»
{
	// default uses a priority that is the current time,
	// causing arbitration to the last started instance if highest priority arbitrator is used
	std::chrono::milliseconds millisSinceEpoch = std::chrono::duration_cast<std::chrono::milliseconds>(
			std::chrono::system_clock::now().time_since_epoch()
	);
	providerQos.setPriority(millisSinceEpoch.count());
	providerQos.setScope(joynr::types::ProviderScope::GLOBAL);
	providerQos.setSupportsOnChangeSubscriptions(true);
}

Default«interfaceName»Provider::~Default«interfaceName»Provider()
{
}

«IF !serviceInterface.attributes.empty»
	// attributes
«ENDIF»
«FOR attribute : serviceInterface.attributes»
	«var attributeName = attribute.joynrName»
	«IF attribute.readable»
		void Default«interfaceName»Provider::get«attributeName.toFirstUpper»(
				std::function<void(
						const «attribute.typeName»&
				)> onSuccess
		) {
			onSuccess(«attributeName»);
		}

	«ENDIF»
	«IF attribute.writable»
		void Default«interfaceName»Provider::set«attributeName.toFirstUpper»(
				const «attribute.typeName»& «attributeName»,
				std::function<void()> onSuccess
		) {
			this->«attributeName» = «attributeName»;
			«attributeName»Changed(«attributeName»);
			onSuccess();
		}

	«ENDIF»
«ENDFOR»
«IF !serviceInterface.methods.empty»
	// methods
«ENDIF»
«FOR method : serviceInterface.methods»
	«val outputTypedParamList = method.commaSeperatedTypedConstOutputParameterList»
	«val outputUntypedParamList = getCommaSeperatedUntypedOutputParameterList(method)»
	«val inputTypedParamList = getCommaSeperatedTypedConstInputParameterList(method)»
	«val methodName = method.joynrName»
	void Default«interfaceName»Provider::«method.joynrName»(
			«IF !method.inputParameters.empty»
				«inputTypedParamList.substring(1)»,
			«ENDIF»
			«IF method.outputParameters.empty»
				std::function<void()> onSuccess
			«ELSE»
				std::function<void(
						«outputTypedParamList.substring(1)»
				)> onSuccess
			«ENDIF»
	) {
		«FOR inputParameter: getInputParameters(method)»
			Q_UNUSED(«inputParameter.joynrName»);
		«ENDFOR»
		«FOR argument : method.outputParameters»
			«val outputParamType = argument.typeName»
			«IF !argument.isArray && argument.type.predefined != null»
				«val type = argument.type.predefined»
				«IF type==FBasicTypeId.STRING»
					«outputParamType» «argument.joynrName» = "Hello World";
				«ELSEIF type==FBasicTypeId.BOOLEAN»
					«outputParamType» «argument.joynrName» = false;
				«ELSEIF type==FBasicTypeId.INT8   ||
						type==FBasicTypeId.UINT8  ||
						type==FBasicTypeId.INT16  ||
						type==FBasicTypeId.UINT16 ||
						type==FBasicTypeId.INT32  ||
						type==FBasicTypeId.UINT32 ||
						type==FBasicTypeId.INT64  ||
						type==FBasicTypeId.UINT64»
					«outputParamType» «argument.joynrName» = 42;
				«ELSEIF type==FBasicTypeId.DOUBLE   ||
						type==FBasicTypeId.FLOAT»
					«outputParamType» «argument.joynrName» = 3.1415;
				«ELSE»
					«outputParamType» «argument.joynrName»;
				«ENDIF»
			«ELSE»
				«outputParamType» «argument.joynrName»;
			«ENDIF»
		«ENDFOR»
		LOG_WARN(logger, "**********************************************");
		LOG_WARN(logger, "* Default«interfaceName»Provider::«methodName» called");
		LOG_WARN(logger, "**********************************************");
		onSuccess(
				«outputUntypedParamList»
		);
	}

«ENDFOR»
«getNamespaceEnder(serviceInterface)»
'''

	/**
	 * add to line 73
	 *
	 *	«ELSEIF isArray(getOutputParameter(method))»
	 *	result = QList<«getMappedDatatype(getOutputParameter(method))»>();
	 *
	 */
}